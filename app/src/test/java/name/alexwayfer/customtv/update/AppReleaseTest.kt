package name.alexwayfer.customtv.update

import name.alexwayfer.customtv.ui.UiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppReleaseTest {
    private fun version(text: String): AppVersion = AppVersion.parse(text)!!

    private fun release(fileId: Int, version: String, downloadedPath: String? = null) =
        AppRelease(fileId, version(version), downloadedPath)

    @Test
    fun missingPartsCountAsZero() {
        assertEquals(version("1.1"), version("1.1.0"))
        assertEquals(version("1.1").hashCode(), version("1.1.0").hashCode())
        assertTrue(version("1.1") < version("1.1.1"))
        assertTrue(version("1.1.1") < version("1.2"))
        assertTrue(version("1.9") < version("1.10"))
    }

    @Test
    fun buildVersionNameReadsTheFirstNumber() {
        assertEquals(version("1.0"), AppVersion.parse("1.0-debug"))
        assertNull(AppVersion.parse("debug"))
    }

    @Test
    fun fileNameNamesTheVersion() {
        assertEquals(version("1.1"), AppVersion.ofRelease("CustomTV-1.1.apk"))
        assertEquals(version("1.1.1"), AppVersion.ofRelease("CustomTV-premium-1.1.1.apk"))
        assertEquals(version("2.0"), AppVersion.ofRelease("CustomTV_v2.0.apk"))
    }

    @Test
    fun aFileNameWithoutADottedNumberNamesNoVersion() {
        assertNull(AppVersion.ofRelease("CustomTV.apk"))
        assertNull(AppVersion.ofRelease("CustomTV-v2.apk"))
    }

    @Test
    fun onlyAnApkWithAVersionIsARelease() {
        assertNotNull(appRelease(1, "CustomTV-1.1.apk", "", null))
        assertNotNull(appRelease(1, "CustomTV-1.1", "application/vnd.android.package-archive", null))
        assertNull(appRelease(1, "notes-1.1.txt", "text/plain", null))
        assertNull(appRelease(1, "CustomTV.apk", "", null))
    }

    @Test
    fun anEmptyPathIsNotADownload() {
        assertNull(appRelease(1, "CustomTV-1.1.apk", "", "")!!.downloadedPath)
    }

    @Test
    fun newestReleaseMustBeNewerThanTheInstalledBuild() {
        val releases = listOf(release(1, "1.1"), release(2, "1.2"), release(3, "1.1.5"))
        assertEquals(2, newestRelease(releases, version("1.1"))!!.fileId)
        assertNull(newestRelease(releases, version("1.2.0")))
        assertNull(newestRelease(emptyList(), version("1.0")))
    }

    @Test
    fun installedDownloadsAreTheOnesNotNewer() {
        val releases = listOf(
            release(1, "1.0", "/a.apk"),
            release(2, "1.1", "/b.apk"),
            release(3, "1.2", "/c.apk"),
            release(4, "1.0"),
        )
        assertEquals(listOf(1, 2), installedDownloads(releases, version("1.1")).map { it.fileId })
    }

    @Test
    fun downloadFractionFallsBackToTheExpectedSize() {
        assertEquals(0.5f, downloadFraction(50, 100, 0), 0f)
        assertEquals(0.25f, downloadFraction(25, 0, 100), 0f)
        assertEquals(0f, downloadFraction(25, 0, 0), 0f)
        assertEquals(1f, downloadFraction(150, 100, 0), 0f)
    }

    @Test
    fun actionFollowsTheDownloadOfThisRelease() {
        val release = release(7, "1.2")
        assertEquals(AppUpdateAction.Download(null), appUpdateAction(release, AppUpdateDownload.Idle))
        assertEquals(
            AppUpdateAction.Downloading(0.3f),
            appUpdateAction(release, AppUpdateDownload.Running(7, 0.3f)),
        )
        assertEquals(
            AppUpdateAction.Install("/x.apk"),
            appUpdateAction(release, AppUpdateDownload.Ready(7, "/x.apk")),
        )
        assertEquals(
            AppUpdateAction.Download(UiText.Raw("Timeout")),
            appUpdateAction(release, AppUpdateDownload.Failed(7, UiText.Raw("Timeout"))),
        )
    }

    @Test
    fun anotherFilesDownloadDoesNotCount() {
        val release = release(7, "1.2")
        assertEquals(AppUpdateAction.Download(null), appUpdateAction(release, AppUpdateDownload.Running(8, 0.3f)))
        assertEquals(AppUpdateAction.Download(null), appUpdateAction(release, AppUpdateDownload.Failed(8, UiText.Raw("Timeout"))))
    }

    @Test
    fun anAlreadyDownloadedReleaseInstalls() {
        assertEquals(
            AppUpdateAction.Install("/old.apk"),
            appUpdateAction(release(7, "1.2", "/old.apk"), AppUpdateDownload.Idle),
        )
    }
}
