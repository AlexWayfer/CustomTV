package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchStreakTimestampTest {
    @Test
    fun putsTheTimestampOnTheUserLineOnlyWhenTheNoticeHasText() {
        assertTrue(watchStreakTimestampOnUserMessage(hasUserText = true))
        assertFalse(watchStreakTimestampOnUserMessage(hasUserText = false))
    }
}
