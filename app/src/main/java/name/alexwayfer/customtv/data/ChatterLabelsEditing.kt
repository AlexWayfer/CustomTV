package name.alexwayfer.customtv.data

import java.util.Base64
import java.util.UUID
import kotlin.math.roundToInt
import org.json.JSONArray
import org.json.JSONObject

/** A label as the settings edit it: [icon] is the extension's data URL, empty until an image is picked. */
internal data class EditableChatterLabel(
    val id: String,
    val name: String,
    val icon: String,
    val enabled: Boolean,
)

/** The extension's two Gist files as the settings page loaded them. */
internal class ChatterLabelsGist(val labels: JSONArray, val assignments: JSONArray)

/** What keeps a label from being saved. */
internal enum class ChatterLabelProblem {
    IconMissing,
    NameMissing,
    NameTaken,
}

/** The Gist files after Save; [assignments] is null when no label was deleted, so that file stays as it is. */
internal class ChatterLabelsUpdate(val labels: JSONArray, val assignments: JSONArray?)

/** The size an icon is stored at. */
internal data class ChatterLabelIconSize(val width: Int, val height: Int)

/** A new label, shown in chat, with an ID made the way the extension makes one. */
internal fun newChatterLabel(): EditableChatterLabel =
    EditableChatterLabel(id = UUID.randomUUID().toString(), name = "", icon = "", enabled = true)

/** Every label in the extension's order, hidden ones too. An entry without an ID is not a label the page can edit. */
internal fun editableChatterLabels(labels: JSONArray): List<EditableChatterLabel> =
    labels.objects().filter { it.optString("id").isNotBlank() }.map { label ->
        EditableChatterLabel(
            id = label.optString("id"),
            name = label.optString("name"),
            icon = label.optString("icon"),
            enabled = label.optBoolean("enabled", true),
        )
    }

/** How many chatters have each label, by label ID. A chatter listed twice for one label counts once. */
internal fun chatterLabelChatterCounts(assignments: JSONArray): Map<String, Int> {
    val users = mutableMapOf<String, MutableSet<String>>()
    for (index in 0 until assignments.length()) {
        val assignment = assignments.optJSONObject(index) ?: continue
        val labelId = assignment.optString("labelId")
        val userId = assignment.optString("userId")
        if (labelId.isNotBlank() && userId.isNotBlank()) users.getOrPut(labelId) { mutableSetOf() }.add(userId)
    }
    return users.mapValues { (_, ids) -> ids.size }
}

/**
 * What keeps each label from being saved, by label ID, as the extension checks it: an icon is required, and a name
 * that is blank or, trimmed, the same as an earlier label's is not allowed.
 */
internal fun chatterLabelProblems(labels: List<EditableChatterLabel>): Map<String, Set<ChatterLabelProblem>> {
    val seenNames = mutableSetOf<String>()
    val result = mutableMapOf<String, Set<ChatterLabelProblem>>()
    for ((id, name, icon) in labels) {
        val trimmedName = name.trim()
        val problems = buildSet {
            if (icon.isBlank()) add(ChatterLabelProblem.IconMissing)
            when {
                trimmedName.isEmpty() -> add(ChatterLabelProblem.NameMissing)
                !seenNames.add(trimmedName) -> add(ChatterLabelProblem.NameTaken)
            }
        }
        if (problems.isNotEmpty()) result[id] = problems
    }
    return result
}

/**
 * The Gist files with the labels set to [edited], in that order. A label keeps the fields the app does not know.
 * The assignments of a deleted label go in the same save, as the extension drops them; every other entry stays.
 */
internal fun chatterLabelsUpdate(gist: ChatterLabelsGist, edited: List<EditableChatterLabel>): ChatterLabelsUpdate {
    val originals = mutableMapOf<String, JSONObject>()
    for (index in 0 until gist.labels.length()) {
        val label = gist.labels.optJSONObject(index) ?: continue
        val id = label.optString("id")
        if (id.isNotBlank()) originals.putIfAbsent(id, label)
    }
    val labels = JSONArray()
    for (label in edited) {
        val entry = originals[label.id]?.let { JSONObject(it.toString()) } ?: JSONObject()
        labels.put(
            entry
                .put("id", label.id)
                .put("icon", label.icon)
                .put("name", label.name.trim())
                .put("enabled", label.enabled),
        )
    }
    val removed = originals.keys - edited.mapTo(mutableSetOf()) { it.id }
    if (removed.isEmpty()) return ChatterLabelsUpdate(labels, assignments = null)
    val assignments = JSONArray()
    for (index in 0 until gist.assignments.length()) {
        val entry = gist.assignments.opt(index)
        val labelId = (entry as? JSONObject)?.optString("labelId")
        if (labelId != null && labelId in removed) continue
        assignments.put(entry)
    }
    return ChatterLabelsUpdate(labels, assignments)
}

/** The size the extension stores a picked image at: [CHATTER_LABEL_ICON_HEIGHT] high at most, never enlarged. */
internal fun chatterLabelIconSize(width: Int, height: Int): ChatterLabelIconSize {
    val scale = if (height > CHATTER_LABEL_ICON_HEIGHT) CHATTER_LABEL_ICON_HEIGHT.toDouble() / height else 1.0
    return ChatterLabelIconSize(
        width = (width * scale).roundToInt().coerceAtLeast(1),
        height = (height * scale).roundToInt().coerceAtLeast(1),
    )
}

/** A PNG as the data URL the extension keeps in `labels.json`. */
internal fun chatterLabelIconDataUrl(png: ByteArray): String =
    "data:image/png;base64," + Base64.getEncoder().encodeToString(png)

/** The height in pixels the extension resizes a label's image to. */
private const val CHATTER_LABEL_ICON_HEIGHT = 36
