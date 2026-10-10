package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatShareOfferTest {
    @Test
    fun plainMessageNeedsWords() {
        assertNull(chatTextToSubmit("   ", emptyAllowed = false))
        assertEquals("hi @streamer", chatTextToSubmit("  hi @streamer ", emptyAllowed = false))
    }

    @Test
    fun shareThatAllowsItGoesWithoutWords() = assertEquals("", chatTextToSubmit("  ", emptyAllowed = true))

    @Test
    fun shareCarriesTheWrittenMessage() = assertEquals("GG Kappa", chatTextToSubmit("GG Kappa ", emptyAllowed = true))

    @Test
    fun sharedEmoteFillsAnEmptyFieldWithASpaceAfterIt() {
        assertEquals("abcHype ", draftWithShared("", "abcHype"))
        assertEquals("abcHype ", draftWithShared("  ", "abcHype"))
    }

    @Test
    fun sharedEmoteGoesAfterWrittenWordsASpaceApart() {
        assertEquals("look abcHype ", draftWithShared("look", "abcHype"))
        assertEquals("look abcHype ", draftWithShared("look ", "abcHype"))
    }
}
