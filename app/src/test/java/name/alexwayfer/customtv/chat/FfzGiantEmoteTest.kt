package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FfzGiantEmoteTest {
    @Test
    fun detectsTokenInPromptOrTitle() {
        assertTrue(ChatReward(id = "1", title = "Attack", cost = 1, prompt = "say an emote FFZ:GE").giantEmote)
        assertTrue(ChatReward(id = "1", title = "FFZ:GE Attack", cost = 1).giantEmote)
        assertFalse(ChatReward(id = "1", title = "Attack", cost = 1, prompt = "just an emote").giantEmote)
        assertFalse(ChatReward(id = "1", title = "ffz:ge", cost = 1).giantEmote)
    }

    @Test
    fun plucksOnlyLastEmote() {
        val parts = listOf(
            ChatPart.Text("first "),
            ChatPart.Emote("Kappa", "https://example.com/kappa"),
            ChatPart.Text(" then "),
            ChatPart.Emote("Pog", "https://example.com/pog", aspectRatio = 1.5f),
        )
        val result = FfzGiantEmote.pluckLast(parts)
        assertEquals(ChatPart.Text("first "), result[0])
        assertEquals("Kappa", (result[1] as ChatPart.Emote).name)
        assertEquals(ChatPart.Text(" then "), result[2])
        val giant = result[3] as ChatPart.Gif
        assertEquals("Pog", giant.name)
        assertEquals("https://example.com/pog", giant.url)
        assertEquals(1.5f, giant.aspectRatio, 0.001f)
    }

    @Test
    fun plucksSoleEmoteAndLeavesTextOnlyUnchanged() {
        val only = listOf(ChatPart.Emote("Kappa", "https://example.com/kappa"))
        val giant = FfzGiantEmote.pluckLast(only).single() as ChatPart.Gif
        assertEquals("Kappa", giant.name)
        val text = listOf(ChatPart.Text("no emotes"))
        assertEquals(text, FfzGiantEmote.pluckLast(text))
    }

    @Test
    fun withDisplayPartsOverlaysThenPlucksLastThirdPartyEmote() {
        val message = ChatMessage(
            id = "1",
            userLogin = "viewer",
            displayName = "Viewer",
            color = Color.White,
            rawText = "Kappa coin",
            parts = listOf(
                ChatPart.Emote("Kappa", "https://twitch.tv/kappa"),
                ChatPart.Text(" coin"),
            ),
            timestampMillis = 0L,
            eventKind = ChatEventKind.Reward,
            reward = ChatReward(
                id = "ge",
                title = "Giant",
                cost = 10_000,
                prompt = "FFZ:GE",
            ),
        )
        val result = message.withDisplayParts(
            mapOf("coin" to SevenTvEmote("https://cdn.7tv.app/coin/2x.webp", aspectRatio = 1.8f)),
            modifierPlatforms = emptySet(),
        )
        assertEquals("Kappa", (result.parts[0] as ChatPart.Emote).name)
        assertEquals(ChatPart.Text(" "), result.parts[1])
        val giant = result.parts[2] as ChatPart.Gif
        assertEquals("coin", giant.name)
        assertEquals("https://cdn.7tv.app/coin/4x.webp", giant.url)
        assertEquals(1.8f, giant.aspectRatio, 0.001f)
    }

    @Test
    fun upgradesCdnUrlsToGiantResolution() {
        assertEquals(
            "https://cdn.betterttv.net/emote/5ff04b6ff58a572e54213a0e/3x.webp",
            FfzGiantEmote.highResolutionUrl(
                "https://cdn.betterttv.net/emote/5ff04b6ff58a572e54213a0e/2x.webp",
            ),
        )
        assertEquals(
            "https://cdn.7tv.app/emote/abc/4x.webp",
            FfzGiantEmote.highResolutionUrl("https://cdn.7tv.app/emote/abc/2x.webp"),
        )
        assertEquals(
            "https://static-cdn.jtvnw.net/emoticons/v2/25/default/dark/4.0",
            FfzGiantEmote.highResolutionUrl(
                "https://static-cdn.jtvnw.net/emoticons/v2/25/default/dark/2.0",
            ),
        )
        assertEquals(
            "https://cdn.frankerfacez.com/emote/omega/4",
            FfzGiantEmote.highResolutionUrl("https://cdn.frankerfacez.com/emote/omega/2"),
        )
        assertEquals(
            "https://cdn.frankerfacez.com/emote/ffzw/animated/4",
            FfzGiantEmote.highResolutionUrl(
                "https://cdn.frankerfacez.com/emote/ffzw/animated/2",
            ),
        )
        assertEquals(
            "https://example.com/custom.png",
            FfzGiantEmote.highResolutionUrl("https://example.com/custom.png"),
        )
    }

    @Test
    fun withDisplayPartsDoesNotPluckWithoutToken() {
        val message = ChatMessage(
            id = "1",
            userLogin = "viewer",
            displayName = "Viewer",
            color = Color.White,
            rawText = "Kappa",
            parts = listOf(ChatPart.Emote("Kappa", "https://twitch.tv/kappa")),
            timestampMillis = 0L,
            eventKind = ChatEventKind.Reward,
            reward = ChatReward(id = "x", title = "Normal", cost = 100),
        )
        val result = message.withDisplayParts(emptyMap(), modifierPlatforms = emptySet())
        assertTrue(result.parts.single() is ChatPart.Emote)
    }
}
