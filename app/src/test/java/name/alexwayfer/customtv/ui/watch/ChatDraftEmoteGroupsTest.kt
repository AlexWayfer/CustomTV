package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.text.TextRange
import name.alexwayfer.customtv.chat.EmoteEffects
import name.alexwayfer.customtv.chat.PickerEmote
import name.alexwayfer.customtv.chat.SevenTvEmote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatDraftEmoteGroupsTest {
    private val kappa = PickerEmote("Kappa", "file:///missing/kappa.png")
    private val wide = PickerEmote("w!", "file:///missing/wide.png")
    private val flip = PickerEmote("h!", "file:///missing/flip.png")
    private val ffzWide = PickerEmote("ffzW", "file:///missing/ffz-wide.png")
    private val pictures = listOf(kappa, wide, flip, ffzWide).associateBy { it.name }
    private val ffzEmotes = mapOf(
        "ffzW" to SevenTvEmote(url = ffzWide.url, effects = EmoteEffects(widthMultiplier = 2f)),
    )
    private val wideEffects = EmoteEffects(widthMultiplier = 2f)

    private fun modifier(token: String) =
        chatDraftModifier(token, bttvModifiers = true, ffzModifiers = true, ffzEmotes = ffzEmotes)

    private fun shown(text: String) =
        chatDraftShownEmotes(text, { it in pictures }, { pictures[it] }, ::modifier)

    @Test
    fun aBttvModifierBeforeAnEmoteHidesIntoItsPicture() {
        assertEquals(
            listOf(ChatDraftShownEmote(TextRange(0, 8), TextRange(0, 1), kappa, wideEffects, TextRange(3, 8))),
            shown("w! Kappa "),
        )
    }

    @Test
    fun severalBttvModifiersCombineOnTheEmote() {
        assertEquals(
            listOf(
                ChatDraftShownEmote(
                    TextRange(0, 11),
                    TextRange(0, 1),
                    kappa,
                    EmoteEffects(widthMultiplier = 2f, flipX = true),
                    TextRange(6, 11),
                ),
            ),
            shown("w! h! Kappa "),
        )
    }

    @Test
    fun anFfzModifierAfterAnEmoteHidesIntoItsPicture() {
        assertEquals(
            listOf(ChatDraftShownEmote(TextRange(0, 10), TextRange(0, 1), kappa, wideEffects, TextRange(6, 10))),
            shown("Kappa ffzW "),
        )
    }

    @Test
    fun aModifierWithoutAnEmoteShowsItsOwnPicture() {
        assertEquals(listOf(ChatDraftShownEmote(TextRange(0, 2), TextRange(0, 1), wide)), shown("w! "))
    }

    @Test
    fun aBttvModifierBeforeAWordShowsItsOwnPicture() {
        assertEquals(
            listOf(
                ChatDraftShownEmote(TextRange(0, 2), TextRange(0, 1), wide),
                ChatDraftShownEmote(TextRange(6, 11), TextRange(5, 6), kappa),
            ),
            shown("w! ok Kappa "),
        )
    }

    @Test
    fun anEmoteBeingTypedAfterAModifierLeavesTheModifierAlone() {
        assertEquals(
            listOf(
                ChatDraftShownEmote(TextRange(0, 2), TextRange(0, 1), wide),
                ChatDraftShownEmote(TextRange(3, 8), TextRange(2, 7), null),
            ),
            shown("w! Kappa"),
        )
    }

    @Test
    fun anFfzModifierBeingTypedStaysTextNextToThePicture() {
        assertEquals(
            listOf(
                ChatDraftShownEmote(TextRange(0, 5), TextRange(0, 1), kappa),
                ChatDraftShownEmote(TextRange(6, 10), TextRange(2, 6), null),
            ),
            shown("Kappa ffzW"),
        )
    }

    @Test
    fun aModifierBetweenEmotesGoesToItsOwnSide() {
        assertEquals(
            listOf(
                ChatDraftShownEmote(TextRange(0, 5), TextRange(0, 1), kappa),
                ChatDraftShownEmote(TextRange(6, 14), TextRange(2, 3), kappa, wideEffects, TextRange(9, 14)),
            ),
            shown("Kappa w! Kappa "),
        )
    }

    @Test
    fun aBackspaceAfterABttvGroupTakesTheEmoteAndLeavesTheModifiers() {
        val original = "w! h! Kappa "
        assertEquals(
            TextRange(6, 11),
            chatDraftWholeEmoteDeletion(original, TextRange(12), "w! h! Kappa", shown(original))!!,
        )
    }

    @Test
    fun aBackspaceAfterAnFfzGroupTakesTheModifierFirst() {
        val original = "Kappa ffzW "
        assertEquals(
            TextRange(6, 10),
            chatDraftWholeEmoteDeletion(original, TextRange(11), "Kappa ffzW", shown(original))!!,
        )
    }

    @Test
    fun anFfzModifierIsNoneWithFfzOff() {
        assertNull(chatDraftModifier("ffzW", bttvModifiers = true, ffzModifiers = false, ffzEmotes = ffzEmotes))
    }

    @Test
    fun aBttvModifierIsNoneWithBttvOff() {
        assertNull(chatDraftModifier("w!", bttvModifiers = false, ffzModifiers = true, ffzEmotes = ffzEmotes))
    }
}
