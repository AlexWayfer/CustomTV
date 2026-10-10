package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.unit.Constraints
import name.alexwayfer.customtv.chat.ChatNick
import name.alexwayfer.customtv.chat.PickerEmote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatComposerPartsTest {
    private val nick = ChatNick("streamer", "Streamer")
    private val emote = PickerEmote("Kappa", "https://kappa")

    @Test
    fun matchingCommandsWinOverNicksAndEmotes() = assertEquals(
        ChatCompletion.Commands(listOf(ChatCommand.User)),
        chatCompletion(listOf(ChatCommand.User), listOf(nick), listOf(emote)),
    )

    @Test
    fun matchingNicksWinOverMatchingEmotes() =
        assertEquals(ChatCompletion.Nicks(listOf(nick)), chatCompletion(emptyList(), listOf(nick), listOf(emote)))

    @Test
    fun emotesShowWhenNoNickMatches() =
        assertEquals(ChatCompletion.Emotes(listOf(emote)), chatCompletion(emptyList(), emptyList(), listOf(emote)))

    @Test
    fun nothingToOfferClosesTheList() = assertNull(chatCompletion(emptyList(), emptyList(), emptyList()))

    @Test
    fun aMessageOnItsWayShowsProgressEvenWithTextTyped() =
        assertEquals(ChatFieldEndIcon.Sending, chatFieldEndIcon(sending = true, hasMessage = true))

    @Test
    fun aTypedMessageShowsSend() = assertEquals(ChatFieldEndIcon.Send, chatFieldEndIcon(sending = false, hasMessage = true))

    @Test
    fun anEmptyFieldShowsTheMenu() = assertEquals(ChatFieldEndIcon.Menu, chatFieldEndIcon(sending = false, hasMessage = false))

    @Test
    fun theOverlayGetsWhatTheBarLeavesOfTheColumn() = assertEquals(600, overlayRoomAbovePx(maxHeightPx = 700, barHeightPx = 100))

    @Test
    fun aBarTallerThanTheColumnLeavesNoRoomAndNeverANegativeOne() = assertEquals(0, overlayRoomAbovePx(maxHeightPx = 80, barHeightPx = 100))

    @Test
    fun anUnboundedColumnLeavesUnboundedRoom() =
        assertEquals(Constraints.Infinity, overlayRoomAbovePx(maxHeightPx = Constraints.Infinity, barHeightPx = 100))
}
