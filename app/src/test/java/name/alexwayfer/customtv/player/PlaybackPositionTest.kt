package name.alexwayfer.customtv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackPositionTest {
    @Test
    fun playingRecordingMovesOnSinceTheReport() {
        assertEquals(10_500L, playbackPositionMs(10_000L, reportedAtMs = 1_000L, nowMs = 1_500L, advancing = true))
    }

    @Test
    fun pausedRecordingStaysAtTheReport() {
        assertEquals(10_000L, playbackPositionMs(10_000L, reportedAtMs = 1_000L, nowMs = 1_500L, advancing = false))
    }

    @Test
    fun clockBeforeTheReportDoesNotMoveBack() {
        assertEquals(10_000L, playbackPositionMs(10_000L, reportedAtMs = 1_000L, nowMs = 900L, advancing = true))
    }

    @Test
    fun durationComesFromTheList() {
        assertEquals(60_000L, recordingDurationMs(durationSeconds = 60L, positionMs = 5_000L))
    }

    @Test
    fun growingRecordingEndsAtThePosition() {
        assertEquals(75_000L, recordingDurationMs(durationSeconds = 60L, positionMs = 75_000L))
    }

    @Test
    fun unknownDurationAtTheStartIsNull() {
        assertNull(recordingDurationMs(durationSeconds = 0L, positionMs = 0L))
    }
}
