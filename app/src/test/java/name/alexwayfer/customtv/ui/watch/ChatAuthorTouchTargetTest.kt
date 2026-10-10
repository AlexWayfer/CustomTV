package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
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
}
