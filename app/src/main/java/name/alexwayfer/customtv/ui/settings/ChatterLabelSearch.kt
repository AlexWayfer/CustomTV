package name.alexwayfer.customtv.ui.settings

import name.alexwayfer.customtv.data.ChatterLabelAssignee

/** What the labels' search looks for: the typed text without spaces around it or an @ before a nick; null for none. */
internal fun chatterSearchTerm(query: String): String? =
    query.trim().removePrefix("@").trim().takeIf { it.isNotEmpty() }

/** The chatters whose login or name holds [term] anywhere, in any case; all of them without a term. */
internal fun assigneesMatching(assignees: List<ChatterLabelAssignee>, term: String?): List<ChatterLabelAssignee> =
    if (term == null) {
        assignees
    } else {
        assignees.filter { assignee ->
            assignee.login.contains(term, ignoreCase = true) || assignee.displayName.contains(term, ignoreCase = true)
        }
    }
