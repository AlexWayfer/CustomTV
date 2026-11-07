package name.alexwayfer.customtv.player

/**
 * Holds automatic playback when a microphone recording interrupts it. Ordinary recording releases the hold when
 * it ends: a voice conversation can take permanent audio focus, so WebView will receive no focus gain callback.
 * Speech recognition also holds before an interruption, and waits afterwards for WebView to resume or an explicit
 * playback command: an assistant may still own transient focus while it thinks or answers.
 * Interruptions without recording keep the background recovery that screen locking needs.
 */
internal class MicrophonePlayback {
    var recording = false
        private set
    var recognizingVoice = false
        private set
    private var interruptedForVoice = false
    private var playbackInterrupted = false

    val holdsAutomaticPlayback: Boolean
        get() {
            if (recognizingVoice || interruptedForVoice) return true
            return recording && playbackInterrupted
        }

    fun recordingChanged(active: Boolean, recognizesSpeech: Boolean, interrupted: Boolean) {
        recording = active
        recognizingVoice = recognizesSpeech
        playbackInterrupted = interrupted
        if (recognizesSpeech && interrupted) interruptedForVoice = true
    }

    fun interrupted() {
        playbackInterrupted = true
        if (recognizingVoice) interruptedForVoice = true
    }

    fun clearInterruption() {
        playbackInterrupted = false
        interruptedForVoice = false
    }
}
