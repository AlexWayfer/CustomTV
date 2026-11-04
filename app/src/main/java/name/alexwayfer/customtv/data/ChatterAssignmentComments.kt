package name.alexwayfer.customtv.data

import org.json.JSONArray
import org.json.JSONObject

/** A missing or null comment is empty; the extension replaces each line break with one space. */
internal fun chatterAssignmentComment(assignment: JSONObject): String =
    singleLineChatterComment(assignment.text("comment").orEmpty())

internal fun singleLineChatterComment(comment: String): String = comment.replace(COMMENT_LINE_BREAK, " ")

/**
 * Changes only the supplied comments of this user's existing assignments, keeping their times and unknown fields.
 * An assignment deleted since the editor opened stays deleted. The input is never mutated.
 */
internal fun updateChatterAssignmentComments(
    assignments: JSONArray,
    userId: String,
    comments: Map<String, String>,
): JSONArray = JSONArray().apply {
    for (index in 0 until assignments.length()) {
        val entry = assignments.opt(index)
        val assignment = entry as? JSONObject
        val comment = assignment?.let { comments[it.optString("labelId")] }
        put(
            if (assignment != null && assignment.optString("userId") == userId && comment != null) {
                JSONObject(assignment.toString()).put("comment", singleLineChatterComment(comment))
            } else {
                entry
            },
        )
    }
}

private val COMMENT_LINE_BREAK = Regex("\\r\\n|[\\r\\n]")
