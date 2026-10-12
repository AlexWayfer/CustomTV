package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.secondsRoundedUp

internal fun chatModeCountdown(remainingMillis: Long): String {
    val totalSeconds = secondsRoundedUp(remainingMillis).coerceAtLeast(1L)
    val days = totalSeconds / 86_400L
    val hours = (totalSeconds % 86_400L) / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        days > 0 -> "${days}d $hours:${minutes.twoDigits()}:${seconds.twoDigits()}"
        hours > 0 -> "$hours:${minutes.twoDigits()}:${seconds.twoDigits()}"
        else -> "$minutes:${seconds.twoDigits()}"
    }
}

internal fun plaqueTicks(plaque: ChatModePlaque): Boolean = when (plaque) {
    is ChatModePlaque.FollowersWait -> true
    is ChatModePlaque.Slow -> plaque.remainingMillis != null
    else -> false
}

private fun Long.twoDigits(): String = toString().padStart(2, '0')
