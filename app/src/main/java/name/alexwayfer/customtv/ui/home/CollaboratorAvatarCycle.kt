package name.alexwayfer.customtv.ui.home

import kotlin.time.Duration.Companion.milliseconds

/** How long each collaborator avatar stays next to the channel avatar. */
internal val COLLABORATOR_AVATAR_INTERVAL = 2500.milliseconds

/**
 * The first shared-clock boundary that leaves the first avatar a whole interval after [startedAtMillis].
 * Later switches happen on the shared boundaries, so all rows switch at the same moment.
 */
private fun firstCollaboratorAvatarSwitch(startedAtMillis: Long): Long {
    val interval = COLLABORATOR_AVATAR_INTERVAL.inWholeMilliseconds
    val earliest = startedAtMillis + interval
    return earliest + Math.floorMod(-earliest, interval)
}

/** Which collaborator avatar to show at [uptimeMillis] when the carousel appeared at [startedAtMillis]. */
internal fun collaboratorAvatarIndex(uptimeMillis: Long, startedAtMillis: Long, count: Int): Int {
    if (count <= 1) return 0
    val firstSwitch = firstCollaboratorAvatarSwitch(startedAtMillis)
    if (uptimeMillis < firstSwitch) return 0
    val step = 1 + (uptimeMillis - firstSwitch) / COLLABORATOR_AVATAR_INTERVAL.inWholeMilliseconds
    return Math.floorMod(step, count.toLong()).toInt()
}

/** How long to wait after [uptimeMillis] before the next avatar is due. */
internal fun millisUntilNextCollaboratorAvatar(uptimeMillis: Long, startedAtMillis: Long): Long {
    val firstSwitch = firstCollaboratorAvatarSwitch(startedAtMillis)
    if (uptimeMillis < firstSwitch) return firstSwitch - uptimeMillis
    val interval = COLLABORATOR_AVATAR_INTERVAL.inWholeMilliseconds
    return interval - Math.floorMod(uptimeMillis, interval)
}
