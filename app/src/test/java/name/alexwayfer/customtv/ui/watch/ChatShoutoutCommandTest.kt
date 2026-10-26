package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.data.ChatSendError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ChatShoutoutCommandTest {
    @Test
    fun aNickAfterTheCommandIsTheChannelToShoutOut() {
        assertEquals(ChatShoutoutCommand.Send("nick"), chatShoutoutCommand("/shoutout Nick", emptyMap()))
        assertEquals(ChatShoutoutCommand.Send("nick"), chatShoutoutCommand(" /SHOUTOUT @nick ", emptyMap()))
    }

    @Test
    fun aDisplayNameOfTheChatTakesItsLogin() {
        assertEquals(
            ChatShoutoutCommand.Send("tanaka"),
            chatShoutoutCommand("/shoutout 田中", mapOf("tanaka" to "田中")),
        )
    }

    @Test
    fun theCommandWithoutANickIsInvalid() {
        assertEquals(ChatShoutoutCommand.Invalid, chatShoutoutCommand("/shoutout", emptyMap()))
        assertEquals(ChatShoutoutCommand.Invalid, chatShoutoutCommand("/shoutout @", emptyMap()))
        assertEquals(ChatShoutoutCommand.Invalid, chatShoutoutCommand("/shoutout no-such!", emptyMap()))
    }

    @Test
    fun otherTextIsNoShoutout() {
        assertNull(chatShoutoutCommand("/shoutouts nick", emptyMap()))
        assertNull(chatShoutoutCommand("/user nick", emptyMap()))
        assertNull(chatShoutoutCommand("shoutout nick", emptyMap()))
    }

    @Test
    fun theFreeBuildPointsToPremium() {
        assertEquals(
            ChatShoutoutCommandRun.Show(ChatSendError.ShoutoutPremium),
            chatShoutoutCommandRun(ChatShoutoutCommand.Send("nick"), locked = true),
        )
    }

    @Test
    fun premiumExplainsAMissingNickOrShoutsOut() {
        assertEquals(
            ChatShoutoutCommandRun.Show(ChatSendError.ShoutoutUsage),
            chatShoutoutCommandRun(ChatShoutoutCommand.Invalid, locked = false),
        )
        assertEquals(
            ChatShoutoutCommandRun.Send("nick"),
            chatShoutoutCommandRun(ChatShoutoutCommand.Send("nick"), locked = false),
        )
    }

    @Test
    fun theShoutoutPremiumHintTakesATap() {
        val showPremium = {}
        assertSame(showPremium, sendErrorTap(ChatSendError.ShoutoutPremium, ChatModeControl.Locked(showPremium)))
    }

    @Test
    fun onlyTheStreamerAndModeratorsGetTheCommand() {
        assertEquals(
            listOf(ChatCommand.Shoutout),
            matchingChatCommands("/sh", availableChatCommands(predictionOpenable = false, chatModes = true)),
        )
        assertEquals(
            emptyList<ChatCommand>(),
            matchingChatCommands("/sh", availableChatCommands(predictionOpenable = true, chatModes = false)),
        )
    }
}
