package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatNicksTest {
    @Test
    fun findsAtMentionsAndSkipsUrlSchemes() {
        val spans = findChatNicks(
            "hi @AlexWayfer and @https://example.com @http://x @www.twitch.tv",
            knownKeys = emptySet(),
        )
        assertEquals(listOf("@AlexWayfer"), spans.map { it.raw })
        assertTrue(spans.single().mentioned)
        assertEquals("alexwayfer", spans.single().key)
    }

    @Test
    fun colorsBareKnownNicksWithoutAt() {
        val spans = findChatNicks(
            "AlexWayfer said hello to nobody",
            knownKeys = setOf("alexwayfer"),
        )
        assertEquals("AlexWayfer", spans.single().raw)
        assertEquals(false, spans.single().mentioned)
    }

    @Test
    fun ignoresBareNamesThatAreNotChatParticipants() {
        assertTrue(findChatNicks("AlexWayfer is here", knownKeys = emptySet()).isEmpty())
    }

    @Test
    fun doesNotTreatShortOrInvalidTokensAsNicks() {
        assertTrue(findChatNicks("hi @ab @ok!", knownKeys = setOf("ab", "ok")).isEmpty())
        assertTrue(findChatNicks("@foo-bar", knownKeys = emptySet()).isEmpty())
    }

    @Test
    fun doesNotHighlightNicksInsideUrls() {
        val spans = findChatNicks(
            "see https://twitch.tv/AlexWayfer @CoolUser",
            knownKeys = setOf("alexwayfer"),
        )
        assertEquals(listOf("@CoolUser"), spans.map { it.raw })
    }

    @Test
    fun atMentionWinsOverBareName() {
        val spans = findChatNicks("@AlexWayfer", knownKeys = setOf("alexwayfer"))
        assertEquals(1, spans.size)
        assertTrue(spans.single().mentioned)
        assertEquals("@AlexWayfer", spans.single().raw)
    }
}
