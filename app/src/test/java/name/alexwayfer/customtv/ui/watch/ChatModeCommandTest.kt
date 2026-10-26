package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.chat.ChatModeChange
import name.alexwayfer.customtv.data.ChatSendError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ChatModeCommandTest {
    private fun change(text: String) = (chatModeCommand(text) as? ChatModeCommandInput.Change)?.change

    @Test
    fun slowTakesItsSeconds() {
        assertEquals(ChatModeChange.Slow(10), change("/slow 10"))
        assertEquals(ChatModeChange.Slow(120), change(" /SLOW   120 "))
    }

    @Test
    fun slowAloneTakesThirtySecondsAsTwitchDoes() {
        assertEquals(ChatModeChange.Slow(30), change("/slow"))
    }

    @Test
    fun slowWithAnythingButANumberIsInvalid() {
        assertEquals(ChatModeCommandInput.Invalid(ChatCommand.Slow), chatModeCommand("/slow fast"))
        assertEquals(ChatModeCommandInput.Invalid(ChatCommand.Slow), chatModeCommand("/slow 10s"))
        assertEquals(ChatModeCommandInput.Invalid(ChatCommand.Slow), chatModeCommand("/slow 99999999999"))
    }

    @Test
    fun followersAloneLetsEveryFollowerChat() {
        assertEquals(ChatModeChange.Followers(0), change("/followers"))
    }

    @Test
    fun followersTakesTheDurationsTwitchListsInShortAndLongForm() {
        assertEquals(ChatModeChange.Followers(30), change("/followers 30m"))
        assertEquals(ChatModeChange.Followers(30), change("/followers 30 minutes"))
        assertEquals(ChatModeChange.Followers(120), change("/followers 2h"))
        assertEquals(ChatModeChange.Followers(60), change("/followers 1 hours"))
        assertEquals(ChatModeChange.Followers(2 * 1440), change("/followers 2d"))
        assertEquals(ChatModeChange.Followers(2 * 1440), change("/followers 2 days"))
        assertEquals(ChatModeChange.Followers(10080), change("/followers 1w"))
        assertEquals(ChatModeChange.Followers(10080), change("/followers 1 week"))
        assertEquals(ChatModeChange.Followers(3 * 43200), change("/followers 3mo"))
        assertEquals(ChatModeChange.Followers(3 * 43200), change("/followers 3 months"))
    }

    @Test
    fun followersWithANumberAloneTakesMinutes() {
        assertEquals(ChatModeChange.Followers(15), change("/followers 15"))
    }

    @Test
    fun followersUnitsTakeAnyCase() {
        assertEquals(ChatModeChange.Followers(60), change("/Followers 1H"))
    }

    @Test
    fun followersWithAnUnknownDurationIsInvalid() {
        assertEquals(ChatModeCommandInput.Invalid(ChatCommand.Followers), chatModeCommand("/followers soon"))
        assertEquals(ChatModeCommandInput.Invalid(ChatCommand.Followers), chatModeCommand("/followers 2y"))
        assertEquals(ChatModeCommandInput.Invalid(ChatCommand.Followers), chatModeCommand("/followers 1h 30m"))
        assertEquals(
            ChatModeCommandInput.Invalid(ChatCommand.Followers),
            chatModeCommand("/followers 9999999999mo"),
        )
    }

    @Test
    fun theOtherCommandsTurnTheirModeOnOrOff() {
        assertEquals(ChatModeChange.Slow(null), change("/slowoff"))
        assertEquals(ChatModeChange.Followers(null), change("/followersoff"))
        assertEquals(ChatModeChange.Subscribers(true), change("/subscribers"))
        assertEquals(ChatModeChange.Subscribers(false), change("/subscribersoff"))
        assertEquals(ChatModeChange.EmoteOnly(true), change("/emoteonly"))
        assertEquals(ChatModeChange.EmoteOnly(false), change("/emoteonlyoff"))
        assertEquals(ChatModeChange.UniqueChat(true), change("/uniquechat"))
        assertEquals(ChatModeChange.UniqueChat(false), change("/uniquechatoff"))
    }

    @Test
    fun aCommandWithoutAValueIgnoresTextAfterIt() {
        assertEquals(ChatModeChange.EmoteOnly(true), change("/emoteonly please"))
    }

    @Test
    fun aValidCommandSendsItsChange() {
        assertEquals(
            ChatModeCommandRun.Send(ChatModeChange.Slow(10)),
            chatModeCommandRun(ChatModeCommandInput.Change(ChatModeChange.Slow(10)), locked = false),
        )
    }

    @Test
    fun aValueTheCommandCannotTakeShowsHowToWriteIt() {
        assertEquals(
            ChatModeCommandRun.Show(ChatSendError.SlowUsage),
            chatModeCommandRun(ChatModeCommandInput.Invalid(ChatCommand.Slow), locked = false),
        )
        assertEquals(
            ChatModeCommandRun.Show(ChatSendError.FollowersUsage),
            chatModeCommandRun(ChatModeCommandInput.Invalid(ChatCommand.Followers), locked = false),
        )
    }

    @Test
    fun theFreeBuildPointsToPremiumWhateverTheValue() {
        assertEquals(
            ChatModeCommandRun.Show(ChatSendError.ChatModesPremium),
            chatModeCommandRun(ChatModeCommandInput.Change(ChatModeChange.Slow(10)), locked = true),
        )
        assertEquals(
            ChatModeCommandRun.Show(ChatSendError.ChatModesPremium),
            chatModeCommandRun(ChatModeCommandInput.Invalid(ChatCommand.Slow), locked = true),
        )
    }

    @Test
    fun onlyThePremiumHintOfTheFreeBuildTakesATap() {
        val showPremium = {}
        val locked = ChatModeControl.Locked(showPremium)
        assertSame(showPremium, sendErrorTap(ChatSendError.ChatModesPremium, locked))
        assertNull(sendErrorTap(ChatSendError.Failed, locked))
        assertNull(sendErrorTap(null, locked))
        assertNull(sendErrorTap(ChatSendError.ChatModesPremium, null))
        val available = ChatModeControl.Available(change = { null }, shoutout = { null })
        assertNull(sendErrorTap(ChatSendError.ChatModesPremium, available))
    }

    @Test
    fun otherTextIsNoChatModeCommand() {
        assertNull(chatModeCommand("/user nick"))
        assertNull(chatModeCommand("/slowly"))
        assertNull(chatModeCommand("slow 10"))
        assertNull(chatModeCommand("hi /slow 10"))
        assertNull(chatModeCommand(""))
    }
}
