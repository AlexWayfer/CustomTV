package name.alexwayfer.customtv.update

import android.content.pm.PackageInstaller
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.UiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppInstallErrorTest {
    @Test
    fun successAndCancelShowNoError() {
        assertNull(appInstallError(PackageInstaller.STATUS_SUCCESS, null))
        assertNull(appInstallError(PackageInstaller.STATUS_FAILURE_ABORTED, "User rejected"))
    }

    @Test
    fun failureShowsAndroidsMessage() {
        assertEquals(
            UiText.Raw("Package conflicts"),
            appInstallError(PackageInstaller.STATUS_FAILURE_CONFLICT, "Package conflicts"),
        )
    }

    @Test
    fun failureWithoutMessageNamesTheStatus() {
        assertEquals(
            UiText.Resource(R.string.app_update_install_failed, listOf(PackageInstaller.STATUS_FAILURE_CONFLICT)),
            appInstallError(PackageInstaller.STATUS_FAILURE_CONFLICT, " "),
        )
    }
}
