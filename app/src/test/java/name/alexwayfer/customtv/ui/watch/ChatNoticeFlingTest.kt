package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatNoticeFlingTest {
    @Test
    fun aHardFlingMovesOnlyToTheNextPageInItsDirection() {
        assertEquals(2, noticePageAfterFling(position = 1.1f, velocity = 9_000f, minFlingVelocity = 50f, pageCount = 5))
        assertEquals(1, noticePageAfterFling(position = 1.9f, velocity = -9_000f, minFlingVelocity = 50f, pageCount = 5))
    }

    @Test
    fun aSlowReleaseSettlesOnTheNearestPage() {
        assertEquals(1, noticePageAfterFling(position = 1.3f, velocity = 20f, minFlingVelocity = 50f, pageCount = 3))
        assertEquals(2, noticePageAfterFling(position = 1.6f, velocity = -20f, minFlingVelocity = 50f, pageCount = 3))
    }

    @Test
    fun aFlingPastTheEdgesStaysOnTheFirstOrLastPage() {
        assertEquals(2, noticePageAfterFling(position = 2f, velocity = 9_000f, minFlingVelocity = 50f, pageCount = 3))
        assertEquals(0, noticePageAfterFling(position = 0f, velocity = -9_000f, minFlingVelocity = 50f, pageCount = 3))
    }

    @Test
    fun aFlingOnAPageEdgeInItsDirectionStaysThere() {
        assertEquals(1, noticePageAfterFling(position = 1f, velocity = 9_000f, minFlingVelocity = 50f, pageCount = 3))
    }
}
