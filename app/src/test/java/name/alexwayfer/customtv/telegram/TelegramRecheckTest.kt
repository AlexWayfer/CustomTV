package name.alexwayfer.customtv.telegram

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramRecheckTest {
    @Test
    fun noOpenCheckChecksAgain() {
        assertTrue(telegramRecheckDue(null))
    }

    @Test
    fun recentOpenCheckSkipsTheCheck() {
        assertFalse(telegramRecheckDue(30.seconds))
        assertFalse(telegramRecheckDue(29.minutes))
    }

    @Test
    fun openCheckAtTheIntervalChecksAgain() {
        assertTrue(telegramRecheckDue(30.minutes))
        assertTrue(telegramRecheckDue(45.minutes))
    }
}
