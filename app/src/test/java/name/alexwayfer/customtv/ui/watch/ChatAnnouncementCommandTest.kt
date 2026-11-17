package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.data.ChatSendError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ChatAnnouncementCommandTest {
    @Test
    fun eachCommandSelectsItsTwitchColor() {
        val colors = mapOf(
            "/announce" to "primary",
            "/announceblue" to "blue",
            "/announcegreen" to "green",
            "/announceorange" to "orange",
            "/announcepurple" to "purple",
        )
        colors.forEach { (command, color) ->
            assertEquals(ChatAnnouncementCommand.Send("Hello!", color), chatAnnouncementCommand("$command Hello!"))
        }
    }

    @Test
    fun caseAndOuterWhitespaceDoNotChangeTheCommandOrItsMessage() {
        assertEquals(
            ChatAnnouncementCommand.Send("Hello,  chat!", "purple"),
            chatAnnouncementCommand("  /AnnouncePURPLE\tHello,  chat!  "),
        )
    }

    @Test
    fun aRecognizedCommandWithoutTextNeedsAMessage() {
        assertEquals(ChatAnnouncementCommand.MissingMessage, chatAnnouncementCommand("/announce"))
        assertEquals(ChatAnnouncementCommand.MissingMessage, chatAnnouncementCommand("/announceblue \t"))
    }

    @Test
    fun anUnknownCommandOrAnAnnouncementWordInsideAMessageDoesNotMatch() {
        assertNull(chatAnnouncementCommand("/announceyellow Hi"))
        assertNull(chatAnnouncementCommand("Type /announce Hi"))
    }

    @Test
    fun tappingThePremiumAnnouncementErrorOpensTheBanner() {
        val showPremium: () -> Unit = {}
        assertSame(showPremium, sendErrorTap(ChatSendError.AnnouncementPremium, ChatModeControl.Locked(showPremium)))
    }
}
