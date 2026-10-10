package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OneShotRequestTest {
    @Test
    fun aRequestIsTakenOnceAndNotAgainWhenItsEffectRunsAnew() {
        val request = OneShotRequest("open")
        assertEquals("open", request.take())
        assertNull(request.take())
    }

    @Test
    fun aNewRequestForTheSameActionIsTakenAgain() {
        OneShotRequest(Unit).take()
        assertEquals(Unit, OneShotRequest(Unit).take())
    }
}
