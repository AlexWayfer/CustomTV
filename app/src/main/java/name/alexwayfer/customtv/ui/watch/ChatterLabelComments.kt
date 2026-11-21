package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.data.ChatterLabelIcon
import name.alexwayfer.customtv.data.singleLineChatterComment

/** Blank comments take up no space in the chatter's profile. */
internal fun chatterLabelSummaryComment(comment: String): String? = comment.trim().takeIf(String::isNotEmpty)

/** Only toggled assignments are replaced, so a comment-only save leaves the current selection in the Gist alone. */
internal fun changedChatterAssignmentIds(assigned: Set<String>, checked: Set<String>): Set<String> =
    (assigned - checked) + (checked - assigned)

/** Only explicit edits of checked labels are written, so unrelated comments changed in the Gist survive. */
internal fun changedChatterComments(
    assigned: List<ChatterLabelIcon>,
    checked: Set<String>,
    edited: Map<String, String>,
): Map<String, String> {
    val saved = assigned.associate { it.id to it.comment }
    return edited.filterKeys { it in checked }
        .mapValues { (_, comment) -> singleLineChatterComment(comment) }
        .filter { (id, comment) -> comment != saved[id].orEmpty() }
}
