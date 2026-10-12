package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatAuthorTouchTargetTest {
    @Test
    fun expandsAboveAndBelowWithoutWideningNickname() {
        assertEquals(Rect(20f, 6f, 90f, 34f), authorTouchBounds(Rect(20f, 10f, 90f, 30f), 4f))
    }

    @Test
    fun firstLineCanExtendAboveTextWithoutChangingTextLayout() {
        assertEquals(Rect(0f, -4f, 30f, 24f), authorTouchBounds(Rect(0f, 0f, 30f, 20f), 4f))
    }

    @Test
    fun layoutWithAddedLinkStyleMatchesText() {
        val text = buildAnnotatedString {
            append("see ")
            withLink(LinkAnnotation.Url("https://example.com")) { append("example.com") }
        }
        val laidOut = buildAnnotatedString {
            append(text)
            addStyle(SpanStyle(color = Color.Blue), 4, text.length)
        }
        assertTrue(layoutTextMatches(laidOut, text))
    }

    @Test
    fun layoutOfOtherTextDoesNotMatch() {
        assertFalse(layoutTextMatches(AnnotatedString("old message"), AnnotatedString("new message")))
    }
}
