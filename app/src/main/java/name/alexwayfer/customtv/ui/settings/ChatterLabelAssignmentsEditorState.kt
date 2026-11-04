package name.alexwayfer.customtv.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.IOException
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChatterLabelAssignee
import name.alexwayfer.customtv.data.ChatterLabelsRepository
import name.alexwayfer.customtv.data.LabeledChatter
import name.alexwayfer.customtv.data.chatterLabelAssignees
import name.alexwayfer.customtv.data.chatterLabelsErrorText
import name.alexwayfer.customtv.ui.UiText
import org.json.JSONArray
import org.json.JSONException

/** Assignments are saved immediately, independently of the labels being edited on the same page. */
internal class ChatterLabelAssignmentsEditorState {
    var entries: JSONArray = JSONArray()
        private set
    var assignees by mutableStateOf<Map<String, List<ChatterLabelAssignee>>>(emptyMap())
        private set
    var busy by mutableStateOf(false)
        private set

    fun show(assignments: JSONArray) {
        entries = assignments
        assignees = chatterLabelAssignees(assignments)
    }

    suspend fun unassign(assignee: ChatterLabelAssignee, labelId: String): UiText? = edit {
        ChatterLabelsRepository.saveAssignments(
            chatter = LabeledChatter(assignee.userId, assignee.login, assignee.displayName),
            shownLabelIds = setOf(labelId),
            checkedLabelIds = emptySet(),
        )
    }

    /** An empty selection edit updates only the comment and cannot recreate a deleted assignment. */
    suspend fun saveComment(assignee: ChatterLabelAssignee, labelId: String, comment: String): UiText? = edit {
        ChatterLabelsRepository.saveAssignments(
            chatter = LabeledChatter(assignee.userId, assignee.login, assignee.displayName),
            shownLabelIds = emptySet(),
            checkedLabelIds = setOf(labelId),
            comments = mapOf(labelId to comment),
        )
    }

    private suspend fun edit(write: suspend () -> JSONArray): UiText? {
        busy = true
        return try {
            show(write())
            null
        } catch (failure: IOException) {
            failure.chatterLabelsErrorText(UiText.Resource(R.string.chatter_labels_error_save_failed))
        } catch (failure: JSONException) {
            failure.chatterLabelsErrorText(UiText.Resource(R.string.chatter_labels_error_save_failed))
        } finally {
            busy = false
        }
    }
}
