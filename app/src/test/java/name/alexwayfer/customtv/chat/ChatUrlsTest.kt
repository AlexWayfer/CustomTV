package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatUrlsTest {
    @Test
    fun findsHttpsAndWwwUrls() {
        val spans = findChatUrls("see https://example.com/a and www.twitch.tv/foo")
        assertEquals(2, spans.size)
        assertEquals("https://example.com/a", spans[0].href)
        assertEquals("https://www.twitch.tv/foo", spans[1].href)
        assertEquals("www.twitch.tv/foo", spans[1].raw)
    }

    @Test
    fun findsBareDomainsWithPathsAndPreservesVisibleText() {
        val spans = findChatUrls("see ibb.co and twitch.tv/foo?x=1, now")

        assertEquals(2, spans.size)
        assertEquals("ibb.co", spans[0].raw)
        assertEquals("https://ibb.co", spans[0].href)
        assertEquals("twitch.tv/foo?x=1", spans[1].raw)
        assertEquals("https://twitch.tv/foo?x=1", spans[1].href)
    }

    @Test
    fun ignoresEmailAndIncompleteDomains() {
        assertTrue(findChatUrls("me@ibb.co localhost 1.2 foo. ibb.c").isEmpty())
    }

    @Test
    fun ignoresFileNamesButKeepsFileExtensionsInUrls() {
        assertTrue(findChatUrls("New Text Document.txt and photo.PNG").isEmpty())

        val spans = findChatUrls("https://example.com/notes.txt readme.md")
        assertEquals(listOf("https://example.com/notes.txt", "https://readme.md"), spans.map { it.href })
    }

    @Test
    fun stripsTrailingPunctuation() {
        val spans = findChatUrls("watch https://example.com/clip.")
        assertEquals("https://example.com/clip", spans.single().href)
        assertEquals("https://example.com/clip", spans.single().raw)
    }

    @Test
    fun keepsBalancedParentheses() {
        val spans = findChatUrls("https://en.wikipedia.org/wiki/Foo_(bar)")
        assertEquals("https://en.wikipedia.org/wiki/Foo_(bar)", spans.single().href)
    }

    @Test
    fun ignoresJavascriptAndBareWords() {
        assertTrue(findChatUrls("javascript:alert(1) hello world").isEmpty())
    }
}
