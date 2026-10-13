package name.alexwayfer.customtv.data

import java.time.Instant
import java.time.temporal.ChronoUnit
import org.json.JSONArray
import org.json.JSONObject

/**
 * The extension's `assignments.json` after the [chatter]'s labels were set to [checkedLabelIds] on the card.
 * Only the chatter's assignments of [shownLabelIds] change: an unchecked one is removed, a newly checked one is
 * appended with [now] as its time, and one that stays checked keeps its time. Every other entry stays as it was.
 */
internal fun updateChatterAssignments(
    assignments: JSONArray,
    chatter: LabeledChatter,
    shownLabelIds: Set<String>,
    checkedLabelIds: Set<String>,
    now: Instant,
): JSONArray {
    val result = JSONArray()
    val kept = mutableSetOf<String>()
    for (index in 0 until assignments.length()) {
        val entry = assignments.opt(index)
        val assignment = entry as? JSONObject
        if (assignment != null && assignment.optString("userId") == chatter.userId) {
            val labelId = assignment.optString("labelId")
            // A duplicate of a kept assignment goes too.
            if (labelId in shownLabelIds && (labelId !in checkedLabelIds || !kept.add(labelId))) continue
        }
        result.put(entry)
    }
    val assignedAt = now.truncatedTo(ChronoUnit.MILLIS).toString()
    for (labelId in checkedLabelIds.intersect(shownLabelIds) - kept) {
        result.put(
            JSONObject()
                .put("userId", chatter.userId)
                .put("username", chatter.displayName)
                .put("login", chatter.login)
                .put("labelId", labelId)
                .put("assignedAt", assignedAt),
        )
    }
    return result
}

/** Indents the JSON with tabs, as the extension writes its Gist files. */
internal fun formatChatterLabelsJson(array: JSONArray): String =
    array.toString(1).replace(LEADING_SPACES) { "\t".repeat(it.value.length) }

private val LEADING_SPACES = Regex("(?m)^ +")

/** The chatter an assignment names: by ID, with the login and the name the extension shows. */
internal data class LabeledChatter(val userId: String, val login: String, val displayName: String)
