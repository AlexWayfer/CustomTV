package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatMessageEnrichmentTest {
    @Test
    fun fillsOnlyMissingRewardFields() {
        val incoming = reward(
            title = "",
            cost = 0,
            background = null,
            image = "event-image",
            prompt = null,
        )
        val known = ChatReward(
            id = "reward",
            title = "Known title",
            cost = 500,
            backgroundColorHex = "#123456",
            imageUrl = "known-image",
            prompt = "Known prompt",
        )

        val result = mergeKnownReward(message().copy(reward = incoming), known).reward!!

        assertEquals("Known title", result.title)
        assertEquals(500, result.cost)
        assertEquals("#123456", result.backgroundColorHex)
        assertEquals("event-image", result.imageUrl)
        assertEquals("Known prompt", result.prompt)
    }

    @Test
    fun messageWithoutRewardIsNotCopied() {
        val message = message()

        assertSame(message, mergeKnownReward(message, reward("Known", 1)))
    }

    @Test
    fun emoteActorUsesKnownColor() {
        val change = ChatEmoteChange(
            platform = EmotePlatform.SevenTv,
            action = EmoteChangeAction.Added,
            actorName = "editor",
            actorColor = Color.Unspecified,
            emoteName = "emote",
        )

        val result = resolveEmoteActorColor(
            message().copy(emoteChange = change),
            Color.Green,
        )

        assertTrue(Color.Green == result.color)
        assertTrue(Color.Green == result.emoteChange?.actorColor)
    }

    @Test
    fun unknownEmoteActorFallsBackToWhite() {
        val change = ChatEmoteChange(
            platform = EmotePlatform.Bttv,
            action = EmoteChangeAction.Removed,
            actorName = "editor",
            actorColor = Color.Unspecified,
            emoteName = "emote",
        )

        assertTrue(
            Color.White == resolveEmoteActorColor(
                message().copy(emoteChange = change),
                null,
            ).color,
        )
    }

    @Test
    fun bttvRemovalUsesResolvedCachedNameAndEmote() {
        val cached = SevenTvEmote(url = "cached")
        val change = emoteChange(EmoteChangeAction.Removed, emoteName = "")

        val result = resolveBttvRemovalMessage(
            message().copy(emoteChange = change),
            resolvedName = "OldName",
            cachedEmote = cached,
        )

        assertEquals("OldName", result.rawText)
        assertEquals("OldName", result.emoteChange?.emoteName)
        assertEquals(cached, result.emoteChange?.emote)
    }

    @Test
    fun bttvRemovalPrefersEmoteFromEvent() {
        val fromEvent = SevenTvEmote(url = "event")
        val change = emoteChange(EmoteChangeAction.Removed, "EventName")
            .copy(emote = fromEvent)

        val result = resolveBttvRemovalMessage(
            message().copy(emoteChange = change),
            resolvedName = "",
            cachedEmote = SevenTvEmote(url = "cached"),
        )

        assertEquals("EventName", result.rawText)
        assertEquals(fromEvent, result.emoteChange?.emote)
    }

    @Test
    fun bttvRenameAddsResolvedPreviousName() {
        val message = message().copy(
            emoteChange = emoteChange(EmoteChangeAction.Renamed, "NewName"),
        )

        val result = resolveBttvRenameMessage(message, "OldName")

        assertEquals("OldName", result.emoteChange?.previousName)
    }

    private fun message() = ChatMessage(
        id = "message",
        userLogin = "viewer",
        displayName = "Viewer",
        color = Color.Red,
        rawText = "text",
        parts = listOf(ChatPart.Text("text")),
        timestampMillis = 1L,
    )

    private fun reward(
        title: String,
        cost: Int,
        background: String? = null,
        image: String? = null,
        prompt: String? = null,
    ) = ChatReward(
        id = "reward",
        title = title,
        cost = cost,
        backgroundColorHex = background,
        imageUrl = image,
        prompt = prompt,
    )

    private fun emoteChange(action: EmoteChangeAction, emoteName: String) = ChatEmoteChange(
        platform = EmotePlatform.Bttv,
        action = action,
        actorName = "editor",
        actorColor = Color.White,
        emoteName = emoteName,
    )
}
