package name.alexwayfer.customtv.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmoteAspectRatioTest {
    @Test
    fun usesIntrinsicWhenValid() {
        assertEquals(18f / 28f, aspectRatioFromIntrinsic(18, 28)!!, 0.001f)
        assertEquals(84f / 28f, aspectRatioFromIntrinsic(84, 28)!!, 0.001f)
        assertEquals(1f, aspectRatioFromIntrinsic(32, 32)!!, 0.001f)
    }

    @Test
    fun rejectsInvalidIntrinsic() {
        assertNull(aspectRatioFromIntrinsic(0, 28))
        assertNull(aspectRatioFromIntrinsic(18, 0))
        assertNull(aspectRatioFromIntrinsic(-1, 10))
    }
}
