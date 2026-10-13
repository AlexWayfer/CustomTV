package name.alexwayfer.customtv.update

import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.UiText
import org.junit.Assert.assertEquals
import org.junit.Test

class AppUpdateStatusTest {
    private val release = AppRelease(id = 1, version = AppVersion.parse("9.9")!!, downloadedPath = null)

    @Test
    fun noUpdateStatusNamesTheLatestVersionFound() {
        assertEquals(
            UiText.Resource(R.string.app_update_none_latest, listOf("1.2.3")),
            appNoUpdateText(AppVersion.parse("1.2.3")),
        )
        assertEquals(UiText.Resource(R.string.app_update_none), appNoUpdateText(null))
    }

    @Test
    fun aCheckThatMayNotifyShowsTheNoticeForANewerRelease() {
        assertEquals(AppUpdateNotice.Show(release.version), appUpdateNotice(release, notify = true))
    }

    @Test
    fun theCheckUpdatesButtonLeavesTheNoticeAsItIs() {
        assertEquals(AppUpdateNotice.Keep, appUpdateNotice(release, notify = false))
    }

    @Test
    fun aCheckWithoutANewerReleaseTakesTheNoticeAway() {
        assertEquals(AppUpdateNotice.Clear, appUpdateNotice(null, notify = true))
        assertEquals(AppUpdateNotice.Clear, appUpdateNotice(null, notify = false))
    }
}
