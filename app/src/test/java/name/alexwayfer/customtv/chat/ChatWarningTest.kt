package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.ChatSendResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatWarningTest {
    @Test
    fun onlyAMessageDroppedAsWarnedMeansAWarning() {
        assertTrue(chatSendWarned(ChatSendResult.Dropped("user_warned", "")))
        assertFalse(chatSendWarned(ChatSendResult.Dropped("msg_followersonly", "Followers only")))
        assertFalse(chatSendWarned(ChatSendResult.Rejected(403, "")))
        assertFalse(chatSendWarned(ChatSendResult.Sent))
    }

    @Test
    fun acknowledgeWaitsSevenSecondsRoundedUpAndNeverBelowZero() {
        assertEquals(7, warningAcknowledgeWaitSeconds(shownAtMillis = 1_000L, nowMillis = 1_000L))
        assertEquals(7, warningAcknowledgeWaitSeconds(shownAtMillis = 1_000L, nowMillis = 1_001L))
        assertEquals(1, warningAcknowledgeWaitSeconds(shownAtMillis = 1_000L, nowMillis = 7_999L))
        assertEquals(0, warningAcknowledgeWaitSeconds(shownAtMillis = 1_000L, nowMillis = 8_000L))
        assertEquals(0, warningAcknowledgeWaitSeconds(shownAtMillis = 1_000L, nowMillis = 60_000L))
    }}
