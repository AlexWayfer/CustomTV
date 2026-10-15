package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun aLinkThatDoesNotOpenKeepsTheTextAndTheLookOfOneThatDoes() {
        val text = "Modpack: https://www.curseforge.com/minecraft/modpacks/cobbleverse"
        val clickable = formatted(text, linksClickable = true)
        val styledOnly = formatted(text, linksClickable = false)

        assertEquals(clickable.text, styledOnly.text)
        assertEquals(1, clickable.getLinkAnnotations(0, clickable.length).size)
        assertTrue(styledOnly.getLinkAnnotations(0, styledOnly.length).isEmpty())
        val linkStart = styledOnly.text.indexOf("https")
        val linkSpan = styledOnly.spanStyles.single { it.start == linkStart }
        assertEquals(LINK_STYLE, linkSpan.item)
        assertEquals(styledOnly.length, linkSpan.end)
    }

    private fun formatted(text: String, linksClickable: Boolean) = buildAnnotatedString {
        appendChatTextWithLinks(text, BODY_STYLE, LINK_STYLE, look(linksClickable), onClick = {})
    }

    private fun look(linksClickable: Boolean) = ChatMessageLook(
        nickColor = Color.White,
        actionItalic = false,
        actionColor = false,
        sizes = ChatTextSizes(14.sp, 12.sp, 22.dp, 22.sp, 14.dp, 18.sp, 14.sp),
        badgeUrls = emptyMap(),
        chatterLabels = emptyMap(),
        nickColors = emptyMap(),
        readableColors = false,
        linksClickable = linksClickable,
        includeAuthor = false,
        timestampText = null,
    )

    private companion object {
        val BODY_STYLE = SpanStyle(color = Color.White)
        val LINK_STYLE = SpanStyle(color = Color.Blue)
    }
}
