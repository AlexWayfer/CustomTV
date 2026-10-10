package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatTextFormattingTest {
    @Test
    fun keepsOnlyCommandPrefixTogether() {
        assertEquals("!\u2060drops", keepCommandPrefixTogether("!drops"))
        assertEquals("hello !\u2060drops and !\u2060команда", keepCommandPrefixTogether("hello !drops and !команда"))
        assertEquals("wow! hello!! ! 123", keepCommandPrefixTogether("wow! hello!! ! 123"))
    }

    @Test
    fun keepsBareLinkHostAndPathTogetherWithoutChangingScheme() {
        assertEquals("ibb.co\u2060/\u2060SwSs2ZhJ", keepUrlPathTogether("ibb.co/SwSs2ZhJ"))
        assertEquals(
            "https://ibb.co\u2060/\u2060SwSs2ZhJ",
            keepUrlPathTogether("https://ibb.co/SwSs2ZhJ"),
        )
        assertEquals("ibb.co", keepUrlPathTogether("ibb.co"))
    }
}
