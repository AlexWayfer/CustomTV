package name.alexwayfer.customtv.ui.settings

import android.app.Application
import name.alexwayfer.customtv.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ActivityStatisticsSearchTest {
    @Test
    fun statisticsSwitchIsDiscoverableInBothEditionsWithoutAnAccount() {
        val entries = settingsSearchEntries(null, sdkInt = 36)
        assertEquals(1, entries.count { it.title == R.string.activity_statistics })
    }
}
