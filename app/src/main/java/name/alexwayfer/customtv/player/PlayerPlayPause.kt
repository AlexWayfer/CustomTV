package name.alexwayfer.customtv.player

/** Whether the player plays, stays paused, or is on its way to playing: loading, or starting again after a pause. */
internal enum class PlayerPlayback { Playing, Paused, Starting }

internal fun playerPlayback(paused: Boolean, awaitingPlayback: Boolean, playbackConfirmed: Boolean): PlayerPlayback =
    when {
        paused -> PlayerPlayback.Paused
        awaitingPlayback || !playbackConfirmed -> PlayerPlayback.Starting
        else -> PlayerPlayback.Playing
    }

internal enum class PlayerCenterButton { Play, Pause }

/**
 * The screen leaves the player's middle to Play and Pause while the player rests at its full size, not on the way to
 * the corner or in it, and while Twitch's settings menu, which can reach over the middle of a phone's player, is shut.
 */
internal fun playerCenterFree(compactSettled: Boolean, progress: Float, settingsMenuOpen: Boolean): Boolean =
    !compactSettled && progress == 0f && !settingsMenuOpen

/**
 * The button in the middle of the player. A paused player shows Play whether or not its controls are up; a playing one
 * shows Pause only with them. A starting player shows neither, since Twitch's spinner takes the middle then, and
 * neither does a player whose middle is taken by something else, such as the seeking taps' count. [centerFree]: the
 * player rests at its full size, with nothing over its middle.
 */
internal fun playerCenterButton(
    playback: PlayerPlayback,
    controlsUp: Boolean,
    centerFree: Boolean,
): PlayerCenterButton? =
    when {
        !centerFree -> null
        playback == PlayerPlayback.Paused -> PlayerCenterButton.Play
        playback == PlayerPlayback.Playing && controlsUp -> PlayerCenterButton.Pause
        else -> null
    }
