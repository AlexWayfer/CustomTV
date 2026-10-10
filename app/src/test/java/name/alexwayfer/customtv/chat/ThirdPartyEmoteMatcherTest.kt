package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class ThirdPartyEmoteMatcherTest {
    @Test
    fun replacesStandaloneTokensAndKeepsTwitchEmotes() {
        val parts = listOf(
            ChatPart.Text("hello "),
            ChatPart.Emote("Kappa", "https://twitch.tv/kappa"),
            ChatPart.Text(" Sadge there"),
        )
        val result = ThirdPartyEmoteMatcher.overlay(
            parts,
            mapOf("Sadge" to SevenTvEmote("https://cdn.7tv.app/emote/sadge/2x.webp")),
        )
        assertEquals(ChatPart.Text("hello "), result[0])
        assertEquals("Kappa", (result[1] as ChatPart.Emote).name)
        assertEquals(ChatPart.Text(" "), result[2])
        assertEquals("Sadge", (result[3] as ChatPart.Emote).name)
        assertEquals("https://cdn.7tv.app/emote/sadge/2x.webp", (result[3] as ChatPart.Emote).url)
        assertEquals(ChatPart.Text(" there"), result[4])
    }

    @Test
    fun keepsWideEmoteAspectRatio() {
        val parts = listOf(ChatPart.Text("widepeepoHappy"))
        val result = ThirdPartyEmoteMatcher.overlay(
            parts,
            mapOf(
                "widepeepoHappy" to SevenTvEmote(
                    url = "https://cdn.7tv.app/emote/wide/2x.webp",
                    aspectRatio = 3.2f,
                ),
            ),
        )
        val emote = result.single() as ChatPart.Emote
        assertEquals(3.2f, emote.aspectRatio, 0.001f)
    }

    @Test
    fun doesNotMatchPartialTokens() {
        val parts = listOf(ChatPart.Text("Sadgeify Sadge"))
        val result = ThirdPartyEmoteMatcher.overlay(
            parts,
            mapOf("Sadge" to SevenTvEmote("https://cdn.7tv.app/emote/sadge/2x.webp")),
        )
        assertEquals(ChatPart.Text("Sadgeify "), result[0])
        assertEquals("Sadge", (result[1] as ChatPart.Emote).name)
    }

    @Test
    fun stacksOverlayEmoteOnPreviousEmote() {
        val parts = listOf(ChatPart.Text("Kappa ALERT"))
        val result = ThirdPartyEmoteMatcher.overlay(
            parts,
            mapOf(
                "Kappa" to SevenTvEmote("https://cdn.7tv.app/emote/kappa/2x.webp"),
                "ALERT" to SevenTvEmote(
                    url = "https://cdn.7tv.app/emote/alert/2x.webp",
                    overlay = true,
                ),
            ),
        )
        val emote = result[0] as ChatPart.Emote
        assertEquals("Kappa", emote.name)
        assertEquals(1, emote.overlays.size)
        assertEquals("ALERT", emote.overlays[0].name)
        assertEquals(ChatPart.Text(" "), result[1])
    }

    @Test
    fun appliesBttvPrefixModifiersToThirdPartyAndTwitchEmotes() {
        val thirdParty = ThirdPartyEmoteMatcher.overlay(
            listOf(ChatPart.Text("w! h! Sadge")),
            mapOf("Sadge" to SevenTvEmote("https://cdn.betterttv.net/emote/sadge/2x.webp")),
            modifierPlatforms = setOf(EmotePlatform.Bttv),
        ).single() as ChatPart.Emote
        assertEquals(2f, thirdParty.effects.widthMultiplier, 0.001f)
        assertEquals(true, thirdParty.effects.flipX)
        assertEquals(listOf("w!", "h!"), thirdParty.modifiers.map { it.name })
        assertEquals(listOf(EmotePlatform.Bttv, EmotePlatform.Bttv), thirdParty.modifiers.map { it.platform })

        val twitch = ThirdPartyEmoteMatcher.overlay(
            listOf(
                ChatPart.Text("v! "),
                ChatPart.Emote("Kappa", "https://static-cdn.jtvnw.net/kappa"),
            ),
            emptyMap(),
            modifierPlatforms = setOf(EmotePlatform.Bttv),
        ).single() as ChatPart.Emote
        assertEquals(true, twitch.effects.flipY)
        assertEquals("v!", twitch.modifiers.single().name)

        val effects = ThirdPartyEmoteMatcher.overlay(
            listOf(ChatPart.Text("c! p! Sadge")),
            mapOf("Sadge" to SevenTvEmote("https://cdn.betterttv.net/emote/sadge/2x.webp")),
            modifierPlatforms = setOf(EmotePlatform.Bttv),
        ).single() as ChatPart.Emote
        assertEquals(true, effects.effects.cursed)
        assertEquals(true, effects.effects.party)
    }

    @Test
    fun bttvModifiersStayTextWhenBttvIsOff() {
        val prefixed = listOf(
            ChatPart.Text("w! "),
            ChatPart.Emote("Kappa", "https://static-cdn.jtvnw.net/kappa"),
        )
        val prefix = ThirdPartyEmoteMatcher.overlay(prefixed, emptyMap(), modifierPlatforms = setOf(EmotePlatform.Ffz))
        assertEquals(ChatPart.Text("w! "), prefix[0])
        assertEquals(EmoteEffects(), (prefix[1] as ChatPart.Emote).effects)

        val inline = ThirdPartyEmoteMatcher.overlay(
            listOf(ChatPart.Text("h! Sadge")),
            mapOf("Sadge" to SevenTvEmote("https://cdn.7tv.app/emote/sadge/2x.webp")),
        )
        assertEquals(ChatPart.Text("h! "), inline[0])
        val emote = inline[1] as ChatPart.Emote
        assertEquals(EmoteEffects(), emote.effects)
        assertEquals(emptyList<EmoteModifier>(), emote.modifiers)
    }

    @Test
    fun appliesFfzEffectModifierToPreviousEmote() {
        val result = ThirdPartyEmoteMatcher.overlay(
            listOf(ChatPart.Text("CatBag ffzCursed")),
            mapOf(
                "CatBag" to SevenTvEmote("https://cdn.frankerfacez.com/emote/catbag/2"),
                "ffzCursed" to SevenTvEmote(
                    "https://cdn.frankerfacez.com/emote/cursed/2",
                    effects = EmoteEffects(cursed = true),
                ),
            ),
        )
        val emote = result.first() as ChatPart.Emote
        assertEquals("CatBag", emote.name)
        assertEquals(true, emote.effects.cursed)
        assertEquals("ffzCursed", emote.modifiers.single().name)
        assertEquals(EmotePlatform.Ffz, emote.modifiers.single().platform)
    }

    @Test
    fun recognizesKnownFfzModifierBeforeCatalogLoadsOnlyWhenEnabled() {
        val parts = listOf(
            ChatPart.Emote("Kappa", "https://static-cdn.jtvnw.net/kappa"),
            ChatPart.Text(" ffzCursed"),
        )
        val enabled = ThirdPartyEmoteMatcher.overlay(parts, emptyMap(), modifierPlatforms = setOf(EmotePlatform.Ffz))
        assertEquals(true, (enabled.single() as ChatPart.Emote).effects.cursed)
        assertEquals("ffzCursed", (enabled.single() as ChatPart.Emote).modifiers.single().name)
        assertEquals(parts, ThirdPartyEmoteMatcher.overlay(parts, emptyMap(), modifierPlatforms = emptySet()))
    }

    @Test
    fun duplicateBttvAndFfzEffectsDoNotCancelOrStack() {
        val result = ThirdPartyEmoteMatcher.overlay(
            listOf(ChatPart.Text("h! v! w! c! Sadge ffzX ffzY ffzW ffzCursed")),
            mapOf("Sadge" to SevenTvEmote("https://cdn.betterttv.net/emote/sadge/2x.webp")),
            modifierPlatforms = setOf(EmotePlatform.Bttv, EmotePlatform.Ffz),
        )
        val emote = result.single() as ChatPart.Emote
        assertEquals(true, emote.effects.flipX)
        assertEquals(true, emote.effects.flipY)
        assertEquals(2f, emote.effects.widthMultiplier, 0.001f)
        assertEquals(true, emote.effects.cursed)
    }

    @Test
    fun overlayWithoutPreviousEmoteStaysStandalone() {
        val parts = listOf(ChatPart.Text("hello ALERT"))
        val result = ThirdPartyEmoteMatcher.overlay(
            parts,
            mapOf(
                "ALERT" to SevenTvEmote(
                    url = "https://cdn.7tv.app/emote/alert/2x.webp",
                    overlay = true,
                ),
            ),
        )
        assertEquals(ChatPart.Text("hello "), result[0])
        assertEquals("ALERT", (result[1] as ChatPart.Emote).name)
        assertEquals(0, (result[1] as ChatPart.Emote).overlays.size)
    }

    @Test
    fun leavesTextWhenMapIsEmpty() {
        val parts = listOf(ChatPart.Text("Sadge"))
        assertEquals(parts, ThirdPartyEmoteMatcher.overlay(parts, emptyMap()))
    }
}
