package name.alexwayfer.customtv.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stops of the video that the app did not ask for, and a pause the app made for another app's sound.
 * [StreamPlayback] clears both on any playing report, play, or pause.
 */
internal object StreamInterruption {
    private val mutableInterrupted = MutableStateFlow(false)

    /**
     * The video stopped without the app asking: it buffers or another app took the sound away while Twitch's
     * player still means to play it, and Twitch plays it on by itself; or Twitch's player paused it itself.
     */
    val interrupted: StateFlow<Boolean> = mutableInterrupted.asStateFlow()

    /** The app paused the stream for another app's sound, and nothing played or paused it since. */
    @Volatile
    var pausedForOtherSound: Boolean = false

    fun report() {
        mutableInterrupted.value = true
    }

    fun clear() {
        mutableInterrupted.value = false
        pausedForOtherSound = false
    }
}
