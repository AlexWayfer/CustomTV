package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModerationNoticesTest {
    @Test
    fun anIrcTimeoutAddsALineNamedByTheChattersDisplayName() {
        val result = withModerationNotice(listOf(said), irc("viewer", 600, at = 1_000))

        assertEquals(2, result.size)
        assertEquals(ChatModerationNotice("viewer", "Viewer", 600, fromIrc = true), result[1].moderationNotice)
    }

    @Test
    fun anIrcBanOfAChatterWithoutMessagesKeepsTheLogin() {
        val result = withModerationNotice(emptyList(), irc("viewer", null, at = 1_000))

        assertEquals(ChatModerationNotice("viewer", "viewer", null, fromIrc = true), result.single().moderationNotice)
    }

    @Test
    fun eventSubAfterIrcFillsTheModeratorIntoTheSameLine() {
        val shown = withModerationNotice(emptyList(), irc("viewer", 600, at = 1_000))

        val result = withModerationNotice(shown, eventSub("viewer", 599, "TheMod", at = 1_500))

        assertEquals(
            ChatModerationNotice("viewer", "Viewer", 600, moderatorName = "TheMod", fromIrc = true),
            result.single().moderationNotice,
        )
    }

    @Test
    fun ircAfterEventSubKeepsOneLineWithTheExactDuration() {
        val shown = withModerationNotice(emptyList(), eventSub("viewer", 599, "TheMod", at = 1_000))

        val result = withModerationNotice(shown, irc("viewer", 600, at = 1_200))

        assertEquals("moderate-viewer-1000", result.single().id)
        assertEquals(
            ChatModerationNotice("viewer", "Viewer", 600, moderatorName = "TheMod", fromIrc = true),
            result.single().moderationNotice,
        )
    }

    @Test
    fun aBanDoesNotPairWithATimeoutOfTheSameChatter() {
        val shown = withModerationNotice(emptyList(), irc("viewer", 600, at = 1_000))

        val result = withModerationNotice(shown, eventSub("viewer", null, "TheMod", at = 1_500))

        assertEquals(2, result.size)
    }

    @Test
    fun anotherChattersTimeoutPairsOnlyWithItsOwnLine() {
        var rows = withModerationNotice(emptyList(), irc("viewer", 600, at = 1_000))
        rows = withModerationNotice(rows, eventSub("other", 600, "TheMod", at = 1_500))
        rows = withModerationNotice(rows, irc("other", 600, at = 1_600))

        assertEquals(
            listOf(
                ChatModerationNotice("viewer", "viewer", 600, fromIrc = true),
                ChatModerationNotice("other", "Other", 600, moderatorName = "TheMod", fromIrc = true),
            ),
            rows.map { it.moderationNotice },
        )
    }

    @Test
    fun anOldLineOutsideTheWindowIsNotFilled() {
        val shown = withModerationNotice(emptyList(), irc("viewer", 600, at = 0))

        val result = withModerationNotice(shown, eventSub("viewer", 600, "TheMod", at = 31_000))

        assertEquals(2, result.size)
        assertNull(result[0].moderationNotice?.moderatorName)
    }

    @Test
    fun twoTimeoutsInARowPairInOrder() {
        var rows = withModerationNotice(emptyList(), irc("viewer", 60, at = 1_000))
        rows = withModerationNotice(rows, eventSub("viewer", 60, "FirstMod", at = 1_100))
        rows = withModerationNotice(rows, irc("viewer", 600, at = 2_000))
        rows = withModerationNotice(rows, eventSub("viewer", 600, "SecondMod", at = 2_100))

        assertEquals(
            listOf("FirstMod" to 60L, "SecondMod" to 600L),
            rows.map { it.moderationNotice?.moderatorName to it.moderationNotice?.timeoutSeconds },
        )
    }

    @Test
    fun theIrcLineIsNotStruckThroughWithTheChattersMessages() {
        val event = ChatMessage(
            id = "clearchat-viewer-1000",
            userLogin = "viewer",
            displayName = "viewer",
            color = Color.Unspecified,
            rawText = "",
            parts = emptyList(),
            timestampMillis = 1_000,
            eventKind = ChatEventKind.UserMessagesDeleted,
            timeoutSeconds = 600,
        )
        val marked = markUserMessagesDeleted(listOf(said), "viewer", null, timeoutSeconds = 600)

        val result = withModerationNotice(marked, clearChatNoticeRow(event))

        assertEquals(listOf(true, false), result.map { it.deleted })
        assertEquals("clearchat-viewer-1000-notice", result[1].id)
        assertEquals("Viewer", result[1].moderationNotice?.targetName)
    }

    @Test
    fun aNoticeWhoseIdTheChatAlreadyShowsAddsNoSecondRow() {
        val shown = said.copy(id = "clearchat-viewer-1000-notice")

        val result = withModerationNotice(listOf(shown), irc("viewer", 600, at = 1_000))

        assertEquals(listOf(shown), result)
    }

    private fun irc(login: String, timeoutSeconds: Long?, at: Long) = moderationNoticeRow(
        id = "clearchat-$login-$at-notice",
        notice = ChatModerationNotice(login, login, timeoutSeconds, fromIrc = true),
        timestampMillis = at,
    )

    /** EventSub names the chatter by display name, here the login capitalized. */
    private fun eventSub(login: String, timeoutSeconds: Long?, moderator: String, at: Long) =
        moderationNoticeRow(
            id = "moderate-$login-$at",
            notice = ChatModerationNotice(
                login,
                login.replaceFirstChar(Char::uppercaseChar),
                timeoutSeconds,
                moderatorName = moderator,
            ),
            timestampMillis = at,
        )

    private val said = ChatMessage(
        id = "m-1",
        userLogin = "viewer",
        displayName = "Viewer",
        color = Color.White,
        rawText = "hi",
        parts = listOf(ChatPart.Text("hi")),
        timestampMillis = 500,
    )
}
