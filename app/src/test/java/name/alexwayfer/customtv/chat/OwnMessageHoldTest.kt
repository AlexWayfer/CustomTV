package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnMessageHoldTest {
    private fun notification(type: String, event: String): String =
        """{"metadata":{"message_type":"notification"},"payload":{"subscription":{"type":"$type"},"event":$event}}"""

    @Test
    fun heldOwnMessageShowsTheMessageAndTheCheckingNotice() {
        val rows = ownMessageHoldRows(
            notification(
                "channel.chat.user_message_hold",
                """{"user_id":"1","user_login":"me","user_name":"Me","message_id":"m-1",
                   "message":{"text":"hi Kappa","fragments":[{"type":"text","text":"hi "},{"type":"emote","text":"Kappa","emote":{"id":"25"}}]}}""",
            ),
            emptyMap(),
            7L,
        )
        assertEquals(listOf("own-held-m-1", "automod-notice-m-1-Checking"), rows.map { it.id })
        assertEquals("me", rows[0].userLogin)
        assertEquals(2, rows[0].parts.size)
        assertFalse(chatMessageCanBeReplied(rows[0]))
        assertEquals(AutoModNotice.Checking, rows[1].autoModNotice)
        assertEquals(ChatEventKind.System, rows[1].eventKind)
    }

    @Test
    fun updateAddsTheNoticeOfTheDecision() {
        mapOf(
            "approved" to AutoModNotice.Allowed,
            "denied" to AutoModNotice.Removed,
        ).forEach { (status, notice) ->
            val rows = ownMessageHoldRows(
                notification("channel.chat.user_message_update", """{"message_id":"m-1","status":"$status"}"""),
                emptyMap(),
                0L,
            )
            assertEquals(listOf(notice), rows.map { it.autoModNotice })
        }
    }

    @Test
    fun messageNobodyDecidedOnAddsNoNotice() {
        assertTrue(
            ownMessageHoldRows(
                notification("channel.chat.user_message_update", """{"message_id":"m-1","status":"invalid"}"""),
                emptyMap(),
                0L,
            ).isEmpty(),
        )
    }

    @Test
    fun unknownStatusAndOtherSubscriptionsAddNothing() {
        assertTrue(
            ownMessageHoldRows(
                notification("channel.chat.user_message_update", """{"message_id":"m-1","status":"pending"}"""),
                emptyMap(),
                0L,
            ).isEmpty(),
        )
        assertTrue(ownMessageHoldRows(notification("automod.message.hold", """{"message_id":"m-1"}"""), emptyMap(), 0L).isEmpty())
        assertTrue(ownMessageHoldRows("not json", emptyMap(), 0L).isEmpty())
    }

    @Test
    fun repeatedNotificationKeepsTheRowAlreadyShown() {
        val rows = ownMessageHoldRows(
            notification("channel.chat.user_message_update", """{"message_id":"m-1","status":"denied"}"""),
            emptyMap(),
            0L,
        )
        val once = rows.fold(emptyList(), ::withChatRowOnce)
        assertEquals(once, rows.fold(once, ::withChatRowOnce))
    }
}
