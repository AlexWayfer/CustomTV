package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatTextScaleTest {
    @Test
    fun defaultTextSizeKeepsTheNormalValue() {
        assertEquals(22f, chatScaled(textSize = 13, normal = 22, large = 26), 0.001f)
    }

    @Test
    fun textSize16KeepsTheLargeValue() {
        assertEquals(26f, chatScaled(textSize = 16, normal = 22, large = 26), 0.001f)
    }

    @Test
    fun largerTextSizeContinuesTheLine() {
        assertEquals(30f, chatScaled(textSize = 19, normal = 22, large = 26), 0.001f)
    }

    @Test
    fun smallerTextSizeContinuesTheLineDown() {
        assertEquals(18f, chatScaled(textSize = 10, normal = 22, large = 26), 0.001f)
    }

    @Test
    fun messageTextFollowsTheChosenSize() {
        assertEquals(20f, chatScaled(textSize = 20, normal = 13, large = 16), 0.001f)
    }
}
