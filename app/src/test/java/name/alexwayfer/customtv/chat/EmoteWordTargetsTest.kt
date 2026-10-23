package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.chat.EmoteWordKind.BttvModifier
import name.alexwayfer.customtv.chat.EmoteWordKind.Emote
import name.alexwayfer.customtv.chat.EmoteWordKind.FfzModifier
import name.alexwayfer.customtv.chat.EmoteWordKind.FfzModifierEmote
import name.alexwayfer.customtv.chat.EmoteWordKind.Other
import name.alexwayfer.customtv.chat.EmoteWordKind.Overlay
import org.junit.Assert.assertEquals
import org.junit.Test

class EmoteWordTargetsTest {
    private fun targets(vararg kinds: EmoteWordKind) = emoteWordTargets(kinds.toList()).toList()

    @Test
    fun bttvModifiersApplyToTheEmoteAfterThem() {
        assertEquals(listOf(2, 2, NO_TARGET), targets(BttvModifier, BttvModifier, Emote))
    }

    @Test
    fun bttvModifiersBeforeAWordApplyToNothing() {
        assertEquals(listOf(NO_TARGET, NO_TARGET, NO_TARGET), targets(BttvModifier, Other, Emote))
    }

    @Test
    fun ffzModifiersApplyToTheEmoteBeforeThem() {
        assertEquals(listOf(NO_TARGET, 0, 0), targets(Emote, FfzModifier, FfzModifierEmote))
    }

    @Test
    fun anFfzModifierWithoutAnEmoteBeforeItAppliesToNothing() {
        assertEquals(listOf(NO_TARGET), targets(FfzModifier))
        assertEquals(listOf(NO_TARGET, NO_TARGET, NO_TARGET), targets(Emote, Other, FfzModifier))
    }

    @Test
    fun anFfzModifierEmoteWithoutAnEmoteBeforeItStandsAndTakesModifiers() {
        assertEquals(listOf(1, NO_TARGET, 1), targets(BttvModifier, FfzModifierEmote, FfzModifier))
    }

    @Test
    fun anFfzModifierDropsTheBttvModifiersWaitingBeforeIt() {
        assertEquals(listOf(NO_TARGET, NO_TARGET, 0), targets(Emote, BttvModifier, FfzModifier))
    }

    @Test
    fun anOverlayStacksOnTheEmoteBeforeItAndTakesTheBttvModifiersBeforeIt() {
        assertEquals(listOf(NO_TARGET, 2, 0), targets(Emote, BttvModifier, Overlay))
    }

    @Test
    fun anOverlayWithoutAnEmoteBeforeItStandsAlone() {
        assertEquals(listOf(NO_TARGET, NO_TARGET, 1), targets(Other, Overlay, FfzModifier))
    }

    @Test
    fun ffzModifiersAfterAnOverlayStillApplyToTheEmote() {
        assertEquals(listOf(NO_TARGET, 0, 0), targets(Emote, Overlay, FfzModifier))
    }
}
