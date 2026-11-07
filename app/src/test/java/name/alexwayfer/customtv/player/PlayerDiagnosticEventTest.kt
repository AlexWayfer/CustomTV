package name.alexwayfer.customtv.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerDiagnosticEventTest {
    @Test
    fun existingPlaybackEventsRemainAccepted() {
        for (event in listOf("playing", "play-request", "timeline-reset-before", "buffer-split-after")) {
            assertTrue(event, isPlayerDiagnosticEvent(event))
        }
    }

    @Test
    fun playActionsCarryAFixedOriginAndACommandNumber() {
        for (origin in listOf("command", "repeat", "late-repeat", "background")) {
            for (action in listOf(
                "request", "already-playing", "ad-skipped", "button-before", "button-after", "button-failed",
                "native-before", "native-after", "native-resolved", "native-rejected", "native-failed",
            )) {
                val event = "play-$action/$origin/12"
                assertTrue(event, isPlayerDiagnosticEvent(event))
                assertTrue(isPlayerDiagnosticEvent("play-$action-stale/$origin/12"))
            }
        }
    }

    @Test
    fun errorRecoveryCarriesOnlyAPhaseAndNumericCode() {
        for (phase in listOf("shown", "reload-before", "reload-after", "reload-click-auto", "reload-click-manual")) {
            assertTrue(isPlayerDiagnosticEvent("error-$phase/2000"))
            assertTrue(isPlayerDiagnosticEvent("error-$phase/0"))
        }
        assertTrue(isPlayerDiagnosticEvent("error-reload-missing"))
    }

    @Test
    fun qualityActionsCarryTheRequestedModeAndACommandNumber() {
        for (mode in listOf("auto", "best", "user", "audio-only", "video-return")) {
            for (action in listOf(
                "request", "fixed-before", "fixed-after", "auto-before", "auto-after",
                "queued", "confirmed", "superseded", "failed", "deferred", "timeout",
            )) {
                assertTrue(isPlayerDiagnosticEvent("quality-$action/$mode/12"))
                assertTrue(isPlayerDiagnosticEvent("quality-$action-stale/$mode/12"))
            }
        }
    }

    @Test
    fun arbitraryTextAndMalformedCommandMetadataAreRejected() {
        for (event in listOf(
            "play-request/https://example.com/1", "play-request/background/0", "play-request/background/-1",
            "play-request/background/12345678901", "play-request/background/1\n", "play-secret/command/1",
            "play-request-stale-stale/command/1", "error-shown/private text", "error-shown/1234567",
            "error-shown/2000?token=secret", "quality-request/unknown/1", "quality-secret/auto/1",
            "quality-request/auto/0", "unknown",
        )) {
            assertFalse(event, isPlayerDiagnosticEvent(event))
        }
    }
}
