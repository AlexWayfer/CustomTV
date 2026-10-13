package name.alexwayfer.customtv.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.IOException
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChatterLabelProblem
import name.alexwayfer.customtv.data.ChatterLabelsGist
import name.alexwayfer.customtv.data.ChatterLabelsRepository
import name.alexwayfer.customtv.data.EditableChatterLabel
import name.alexwayfer.customtv.data.chatterLabelChatterCounts
import name.alexwayfer.customtv.data.chatterLabelProblems
import name.alexwayfer.customtv.data.chatterLabelsErrorText
import name.alexwayfer.customtv.data.editableChatterLabels
import name.alexwayfer.customtv.data.newChatterLabel
import name.alexwayfer.customtv.ui.UiText
import org.json.JSONException

/** The page's copy of the labels: what the Gist had when the page read it, and the edits since. */
internal class ChatterLabelsEditorState {
    private var gist by mutableStateOf<ChatterLabelsGist?>(null)
    private var loadedLabels by mutableStateOf<List<EditableChatterLabel>>(emptyList())
    var labels by mutableStateOf<List<EditableChatterLabel>>(emptyList())
        private set

    /** How many chatters have each saved label, by label ID. */
    var chatterCounts by mutableStateOf<Map<String, Int>>(emptyMap())
        private set
    var loading by mutableStateOf(false)
        private set
    var loadError by mutableStateOf<UiText?>(null)
        private set
    var saving by mutableStateOf(false)
        private set

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

    suspend fun load() {
        loading = true
        loadError = null
        try {
            show(ChatterLabelsRepository.loadForEditing())
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
        chatterCounts = chatterLabelChatterCounts(loaded.assignments)
        showProblems = false
    }
}
