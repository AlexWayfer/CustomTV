package name.alexwayfer.customtv.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.IOException
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.CHATTER_LABELS_TAG
import name.alexwayfer.customtv.data.ChannelProfileFetcher
import name.alexwayfer.customtv.data.ChatterLabelAssignee
import name.alexwayfer.customtv.data.ChatterLabelProblem
import name.alexwayfer.customtv.data.ChatterLabelsCredentials
import name.alexwayfer.customtv.data.ChatterLabelsGist
import name.alexwayfer.customtv.data.ChatterLabelsRepository
import name.alexwayfer.customtv.data.EditableChatterLabel
import name.alexwayfer.customtv.data.chatterAssignmentUserIds
import name.alexwayfer.customtv.data.chatterLabelAssignees
import name.alexwayfer.customtv.data.chatterLabelProblems
import name.alexwayfer.customtv.data.chatterLabelsErrorText
import name.alexwayfer.customtv.data.editableChatterLabels
import name.alexwayfer.customtv.data.newChatterLabel
import name.alexwayfer.customtv.data.syncChatterAssignmentUsers
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.ui.UiText
import org.json.JSONArray
import org.json.JSONException

/** The page's copy of the labels: what the Gist had when the page read it, and the edits since. */
internal class ChatterLabelsEditorState {
    private var gist by mutableStateOf<ChatterLabelsGist?>(null)
    private var loadedLabels by mutableStateOf<List<EditableChatterLabel>>(emptyList())
    var labels by mutableStateOf<List<EditableChatterLabel>>(emptyList())
        private set

    /** The chatters who have each saved label, by label ID. */
    var assignees by mutableStateOf<Map<String, List<ChatterLabelAssignee>>>(emptyMap())
        private set

    /** While the chatters' current names and logins load from Twitch. */
    var namesLoading by mutableStateOf(false)
        private set

    /** The user IDs of the chatters Twitch has banned. */
    var banned by mutableStateOf<Set<String>>(emptySet())
        private set

    /** The user IDs of the chatters Twitch does not know, or knows only as deleted. */
    var notFound by mutableStateOf<Set<String>>(emptySet())
        private set
    var loading by mutableStateOf(false)
        private set
    var loadError by mutableStateOf<UiText?>(null)
        private set
    var saving by mutableStateOf(false)
        private set

    /** While a chatter's label is being deleted in the Gist. */
    var unassigning by mutableStateOf(false)
        private set

    /** While the page writes to the Gist, so the labels and their chatters wait. */
    val busy: Boolean get() = saving || unassigning

    /** A failed save or image read, shown above Save until the next edit. */
    var error by mutableStateOf<UiText?>(null)

    /** The problems show only after a Save found them, so a new empty label is not red at once. */
    var showProblems by mutableStateOf(false)
        private set

    val ready: Boolean get() = gist != null
    val changed: Boolean get() = labels != loadedLabels
    val problems: Map<String, Set<ChatterLabelProblem>> get() = chatterLabelProblems(labels)

    /** Whether the label is in the Gist already, so deleting it there needs a confirmation. */
    fun isSaved(id: String): Boolean = loadedLabels.any { it.id == id }

    private var loadedFrom: ChatterLabelsCredentials? = null

    /**
     * Reads the labels from the Gist of [credentials], unless this editor holds them already, as when the page comes
     * back from a profile: its edits stay.
     */
    suspend fun load(credentials: ChatterLabelsCredentials) {
        if (ready && loadedFrom == credentials) return
        loading = true
        loadError = null
        try {
            show(ChatterLabelsRepository.loadForEditing())
            loadedFrom = credentials
            syncChatterNames()
        } catch (failure: IOException) {
            loadError = failure.chatterLabelsErrorText(UiText.Resource(R.string.chatter_labels_error_load_failed))
        } catch (failure: JSONException) {
            loadError = failure.chatterLabelsErrorText(UiText.Resource(R.string.chatter_labels_error_load_failed))
        } finally {
            loading = false
        }
    }

    suspend fun save() {
        val loaded = gist ?: return
        if (problems.isNotEmpty()) {
            showProblems = true
            return
        }
        saving = true
        error = null
        try {
            show(ChatterLabelsRepository.saveLabels(loaded, labels))
        } catch (failure: IOException) {
            error = failure.chatterLabelsErrorText(UiText.Resource(R.string.chatter_labels_error_save_failed))
        } catch (failure: JSONException) {
            error = failure.chatterLabelsErrorText(UiText.Resource(R.string.chatter_labels_error_save_failed))
        } finally {
            saving = false
        }
    }

    /**
     * Takes the label [labelId] from [assignee] in the Gist at once, as the extension deletes an assignment; the labels
     * being edited stay as they are, and a failure shows as [error].
     */
    suspend fun unassign(assignee: ChatterLabelAssignee, labelId: String) {
        unassigning = true
        error = null
        try {
            showAssignments(
                ChatterLabelsRepository.saveAssignments(
                    userId = assignee.userId,
                    login = assignee.login,
                    displayName = assignee.displayName,
                    shownLabelIds = setOf(labelId),
                    checkedLabelIds = emptySet(),
                ),
            )
        } catch (failure: IOException) {
            error = failure.chatterLabelsErrorText(UiText.Resource(R.string.chatter_labels_error_save_failed))
        } catch (failure: JSONException) {
            error = failure.chatterLabelsErrorText(UiText.Resource(R.string.chatter_labels_error_save_failed))
        } finally {
            unassigning = false
        }
    }

    fun update(id: String, change: (EditableChatterLabel) -> EditableChatterLabel) {
        labels = labels.map { if (it.id == id) change(it) else it }
        error = null
    }

    fun add() {
        labels = labels + newChatterLabel()
        error = null
    }

    fun delete(id: String) {
        labels = labels.filterNot { it.id == id }
        error = null
    }

    fun move(from: Int, to: Int) {
        if (from == to || from !in labels.indices || to !in labels.indices) return
        labels = labels.toMutableList().apply { add(to, removeAt(from)) }
        error = null
    }

    private fun show(loaded: ChatterLabelsGist) {
        gist = loaded
        loadedLabels = editableChatterLabels(loaded.labels)
        labels = loadedLabels
        assignees = chatterLabelAssignees(loaded.assignments)
        showProblems = false
    }

    /**
     * Once the labels show, reads every assigned chatter's current name and login from Twitch and writes the changed
     * ones into the Gist, as the extension does when a chatter's card opens: an assignment from before the extension
     * kept logins gets one, so that chatter's profile opens, and a renamed chatter shows the new name. The page shows
     * them before the write, and keeps them if the write fails; the labels being edited stay as they are.
     */
    private suspend fun syncChatterNames() {
        val loaded = gist ?: return
        val ids = chatterAssignmentUserIds(loaded.assignments)
        if (ids.isEmpty()) return
        namesLoading = true
        val names = try {
            ChannelProfileFetcher.names(ids)
        } finally {
            namesLoading = false
        }
        banned = names.banned
        notFound = names.notFound
        val users = names.found
        try {
            showAssignments(syncChatterAssignmentUsers(loaded.assignments, users) ?: return)
            ChatterLabelsRepository.syncAssignmentUsers(users)?.let { written ->
                showAssignments(written)
                AppLog.i(CHATTER_LABELS_TAG, "chatter names synced to the gist")
            }
        } catch (failure: IOException) {
            AppLog.w(CHATTER_LABELS_TAG, "chatter names sync failed: ${failure.javaClass.simpleName}")
        } catch (failure: JSONException) {
            AppLog.w(CHATTER_LABELS_TAG, "chatter names sync failed: ${failure.javaClass.simpleName}")
        }
    }

    private fun showAssignments(assignments: JSONArray) {
        gist = gist?.let { ChatterLabelsGist(it.labels, assignments) }
        assignees = chatterLabelAssignees(assignments)
    }
}
