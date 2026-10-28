package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.alexwayfer.customtv.chat.NamePaint
import name.alexwayfer.customtv.chat.PaintFunction
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
        val clickable = formatted(linksClickable = true)
        val styledOnly = formatted(linksClickable = false)

        assertEquals(clickable.text, styledOnly.text)
        assertEquals(1, clickable.getLinkAnnotations(0, clickable.length).size)
        assertTrue(styledOnly.getLinkAnnotations(0, styledOnly.length).isEmpty())
        val linkStart = styledOnly.text.indexOf("https")
        val linkSpan = styledOnly.spanStyles.single { it.start == linkStart }
        assertEquals(LINK_STYLE, linkSpan.item)
        assertEquals(styledOnly.length, linkSpan.end)
    }

    @Test
    fun aPaintedNickIsMarkedForItsBoxAndTakesItsPaintOnceTheBoxIsKnown() {
        val look = look(linksClickable = true).copy(
            nickColors = mapOf("viewer" to Color.Red, "other" to Color.Blue),
            paintedNicks = setOf("viewer"),
        )
        val unpainted = buildAnnotatedString {
            appendChatTextWithLinks(MENTION_TEXT, BODY_STYLE, LINK_STYLE, look, onClick = {})
        }
        val painted = buildAnnotatedString {
            appendChatTextWithLinks(
                MENTION_TEXT,
                BODY_STYLE,
                LINK_STYLE,
                look.copy(nickPaints = mapOf(MENTION_START to PAINT_STYLE)),
                onClick = {},
            )
        }

        val mark = unpainted.getStringAnnotations(CHAT_NICK_PAINT_TAG, 0, unpainted.length).single()
        assertEquals("viewer", mark.item)
        assertEquals(MENTION_START, mark.start)
        assertEquals(MENTION_START + "@Viewer".length, mark.end)
        assertEquals(Color.Red, unpainted.spanStyles.single { it.start == MENTION_START }.item.color)
        assertEquals(unpainted.text, painted.text)
        assertEquals(Color.Green, painted.spanStyles.single { it.start == MENTION_START }.item.color)
        assertEquals(Color.Blue, painted.spanStyles.single { it.start == MENTION_TEXT.indexOf("@other") }.item.color)
    }

    @Test
    fun eachBoxOfAPaintedNickTakesItsPaintOverTheNickColor() {
        val paint = NamePaint(function = PaintFunction.Image, imageUrl = "https://paint")
        val first = Rect(0f, 0f, 40f, 20f)
        val second = Rect(60f, 0f, 100f, 20f)
        val boxes = listOf(
            NickPaintBox(3, "viewer", first),
            NickPaintBox(15, "other", second),
            NickPaintBox(30, "viewer", second),
            NickPaintBox(40, "fresh", first),
        )

        val boxed = boxedNickPaints(
            boxes = boxes,
            paints = mapOf("viewer" to paint, "fresh" to paint),
            colors = mapOf("viewer" to Color.Red, "other" to Color.Blue),
            readableColors = false,
        )

        assertEquals(
            mapOf(
                3 to BoxedNamePaint(paint, Color.Red, first),
                30 to BoxedNamePaint(paint, Color.Red, second),
                40 to BoxedNamePaint(paint, Color.White, first),
            ),
            boxed,
        )
    }

    private fun formatted(linksClickable: Boolean) = buildAnnotatedString {
        appendChatTextWithLinks(LINK_TEXT, BODY_STYLE, LINK_STYLE, look(linksClickable), onClick = {})
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
        const val LINK_TEXT = "Modpack: https://www.curseforge.com/minecraft/modpacks/cobbleverse"
        val BODY_STYLE = SpanStyle(color = Color.White)
        val LINK_STYLE = SpanStyle(color = Color.Blue)
        const val MENTION_TEXT = "hi @Viewer and @other"
        const val MENTION_START = 3
        val PAINT_STYLE = SpanStyle(color = Color.Green)
    }
}
