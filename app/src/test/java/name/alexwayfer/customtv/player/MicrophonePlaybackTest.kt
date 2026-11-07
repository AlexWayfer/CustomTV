package name.alexwayfer.customtv.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MicrophonePlaybackTest {
    private val playback = MicrophonePlayback()

    @Test
    fun anInterruptionWithoutRecognitionKeepsBackgroundRecovery() {
        playback.interrupted()

        assertFalse(playback.holdsAutomaticPlayback)
    }

    @Test
    fun recognitionAloneHoldsAutomaticPlaybackUntilItEnds() {
        playback.recordingChanged(active = true, recognizesSpeech = true, interrupted = false)
        assertTrue(playback.holdsAutomaticPlayback)

        playback.recordingChanged(active = false, recognizesSpeech = false, interrupted = false)
        assertFalse(playback.holdsAutomaticPlayback)
    }

    @Test
    fun anInterruptedRecognitionWaitsAfterTheRecordingEnds() {
        playback.recordingChanged(active = true, recognizesSpeech = true, interrupted = false)
        playback.interrupted()
        playback.recordingChanged(active = false, recognizesSpeech = false, interrupted = true)

        assertTrue(playback.holdsAutomaticPlayback)

        playback.clearInterruption()
        assertFalse(playback.holdsAutomaticPlayback)
    }

    @Test
    fun recognitionReportedAfterTheInterruptionAlsoWaitsForPlayback() {
        playback.recordingChanged(active = true, recognizesSpeech = true, interrupted = true)
        playback.recordingChanged(active = false, recognizesSpeech = false, interrupted = true)

        assertTrue(playback.holdsAutomaticPlayback)
    }

    @Test
    fun aPlayingReportDuringRecognitionStillHoldsAutomaticRepeats() {
        playback.recordingChanged(active = true, recognizesSpeech = true, interrupted = true)
        playback.clearInterruption()

        assertTrue(playback.holdsAutomaticPlayback)
    }

    @Test
    fun anOrdinaryRecordingHoldsOnlyAfterAnInterruptionAndReleasesWhenItEnds() {
        playback.recordingChanged(active = true, recognizesSpeech = false, interrupted = false)
        assertFalse(playback.holdsAutomaticPlayback)

        playback.interrupted()
        assertTrue(playback.holdsAutomaticPlayback)

        playback.recordingChanged(active = false, recognizesSpeech = false, interrupted = true)
        assertFalse(playback.holdsAutomaticPlayback)
    }

    @Test
    fun anOrdinaryRecordingReportedAfterAnInterruptionHoldsUntilItEnds() {
        playback.interrupted()
        playback.recordingChanged(active = true, recognizesSpeech = false, interrupted = true)
        assertTrue(playback.holdsAutomaticPlayback)

        playback.recordingChanged(active = false, recognizesSpeech = false, interrupted = true)
        assertFalse(playback.holdsAutomaticPlayback)
    }

    @Test
    fun explicitPlaybackClearsTheHoldOfAnOrdinaryRecording() {
        playback.recordingChanged(active = true, recognizesSpeech = false, interrupted = true)
        playback.clearInterruption()

        assertFalse(playback.holdsAutomaticPlayback)
    }
}
