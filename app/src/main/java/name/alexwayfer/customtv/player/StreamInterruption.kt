package name.alexwayfer.customtv.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import name.alexwayfer.customtv.diagnostics.AppLog

/**
 * Stops of the video that the app did not ask for, and a pause the app made for another app's sound.
 * [StreamPlayback] clears both on any playing report, play, or pause.
 */
internal object StreamInterruption {
    private val mutableInterrupted = MutableStateFlow(false)
    private val microphone = MicrophonePlayback()

    val microphoneHoldsPlayback: Boolean
        get() = microphone.holdsAutomaticPlayback

    fun reportAudioRecording(recording: Boolean, recognizingVoice: Boolean) {
        if (recording != microphone.recording || recognizingVoice != microphone.recognizingVoice) {
            AppLog.i("PlayerAudio", "Audio recording changed: active=$recording, recognition=$recognizingVoice")
        }
        microphone.recordingChanged(recording, recognizingVoice, interrupted = mutableInterrupted.value)
    }

    /**
     * The video stopped without the app asking: it buffers or another app took the sound away while Twitch's
     * player still means to play it, and Twitch plays it on by itself; or Twitch's player paused it itself.
     */
    val interrupted: StateFlow<Boolean> = mutableInterrupted.asStateFlow()

    /** The app paused the stream for another app's sound, and nothing played or paused it since. */
    @Volatile
    var pausedForOtherSound: Boolean = false

    fun report() {
        microphone.interrupted()
        mutableInterrupted.value = true
    }

    fun clear() {
        microphone.clearInterruption()
        mutableInterrupted.value = false
        pausedForOtherSound = false
    }
}
