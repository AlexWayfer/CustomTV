package name.alexwayfer.customtv.player

/** How long the page fades the sound in or out as the player plays, pauses, starts, or closes. */
internal const val PLAYER_SOUND_FADE_MS = 300L

// The fade's timer steps can run a little past its length.
private const val CLOSE_FADE_MARGIN_MS = 50L

/** The fade the page is told to make: none without Smooth sound. */
internal fun playerSoundFadeMs(smoothSound: Boolean): Long = if (smoothSound) PLAYER_SOUND_FADE_MS else 0L

/** How long a closed player's page keeps playing while its sound fades out: not at all without Smooth sound. */
internal fun playerCloseDelayMs(smoothSound: Boolean): Long =
    if (smoothSound) PLAYER_SOUND_FADE_MS + CLOSE_FADE_MARGIN_MS else 0L
