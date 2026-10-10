package name.alexwayfer.customtv.player

import org.junit.Assert.assertEquals
import org.junit.Test

class StreamMediaArtworkTest {
    @Test
    fun previewUrlLowercasesLogin() {
        assertEquals(
            "https://static-cdn.jtvnw.net/previews-ttv/live_user_xqcow-640x360.jpg",
            StreamMediaArtwork.previewUrl("xQcOW"),
        )
    }

    @Test
    fun previewUrlCanBustCaches() {
        assertEquals(
            "https://static-cdn.jtvnw.net/previews-ttv/live_user_xqcow-640x360.jpg?t=42",
            StreamMediaArtwork.previewUrl("xQcOW", fetchedAt = 42L),
        )
    }

    @Test
    fun titlePrefersDisplayName() {
        assertEquals("xQc", streamMediaTitle("xqcow", "xQc"))
    }

    @Test
    fun titleFallsBackToLogin() {
        assertEquals("xqcow", streamMediaTitle("xqcow", null))
        assertEquals("xqcow", streamMediaTitle("xqcow", "  "))
    }

    @Test
    fun artistPrefersCategory() {
        assertEquals("Just Chatting", streamMediaArtist("CustomTV", "Just Chatting"))
    }

    @Test
    fun artistFallsBackToAppName() {
        assertEquals("CustomTV", streamMediaArtist("CustomTV", null))
        assertEquals("CustomTV", streamMediaArtist("CustomTV", "  "))
    }
}
