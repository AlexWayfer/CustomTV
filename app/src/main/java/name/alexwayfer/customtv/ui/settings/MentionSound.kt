package name.alexwayfer.customtv.ui.settings

internal const val MENTION_SOUND_LIMIT_MS = 4_000L

internal fun storedMentionSound(picked: String?, defaultUri: String): String? {
    if (picked.isNullOrBlank()) return null
    if (picked == defaultUri) return ""
    return picked
}

internal fun mentionSoundUri(stored: String, defaultUri: String): String =
    stored.ifBlank { defaultUri }

internal fun mentionSoundStillPlaying(elapsedMs: Long, playing: Boolean): Boolean =
    playing && elapsedMs < MENTION_SOUND_LIMIT_MS

internal fun mentionSoundPermissionToRelease(previous: String, stored: String): String? {
    if (previous.isBlank() || previous == stored) return null
    return previous
}
