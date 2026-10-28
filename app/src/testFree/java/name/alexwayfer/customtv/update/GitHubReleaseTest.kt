package name.alexwayfer.customtv.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubReleaseTest {
    private val digest = "7e87dce7c6f776d1970356a1c13c51c87901fb805aca262e6c5f4429522cb8f0"
    private val apk = GitHubApk(
        id = 300123456789,
        version = AppVersion.parse("1.7.0")!!,
        url = "https://github.com/AlexWayfer/CustomTV/releases/download/free-v1.7.0/CustomTV-free-1.7.0.apk",
        size = 48115930,
        sha256 = digest,
    )

    private fun asset(
        name: String = "CustomTV-free-1.7.0.apk",
        state: String = "uploaded",
        digest: String? = "\"sha256:${this.digest.uppercase()}\"",
        url: String = apk.url,
    ) = """
        {
          "id": 300123456789,
          "name": "$name",
          "content_type": "application/vnd.android.package-archive",
          "state": "$state",
          "size": 48115930,
          "digest": $digest,
          "browser_download_url": "$url"
        }
    """

    private fun release(vararg assets: String) =
        """{"tag_name": "free-v1.7.0", "assets": [${assets.joinToString(",")}]}"""

    @Test
    fun theApkAssetGivesItsVersionUrlSizeAndDigest() {
        assertEquals(apk, githubLatestApk(release(asset())))
    }

    @Test
    fun anotherFileBeforeTheApkIsSkipped() {
        val notes = """{"id": 1, "name": "notes-1.7.0.txt", "content_type": "text/plain", "state": "uploaded",
            "size": 10, "browser_download_url": "https://github.com/notes.txt"}"""
        assertEquals(apk, githubLatestApk(release(notes, asset())))
    }

    @Test
    fun anAssetWithoutADigestIsCheckedBySizeOnly() {
        assertNull(githubLatestApk(release(asset(digest = "null")))!!.sha256)
    }

    @Test
    fun anUnfinishedOrUnsafeAssetIsNotOffered() {
        assertNull(githubLatestApk(release(asset(state = "starter"))))
        assertNull(githubLatestApk(release(asset(url = "http://github.com/CustomTV-free-1.7.0.apk"))))
    }

    @Test
    fun aReleaseWithoutAnApkOrABrokenAnswerOffersNothing() {
        assertNull(githubLatestApk(release()))
        assertNull(githubLatestApk(release(asset(name = "CustomTV-free.apk"))))
        assertNull(githubLatestApk("<html>"))
        assertNull(githubLatestApk("""{"message": "Not Found"}"""))
    }

    @Test
    fun onlyASha256DigestCounts() {
        assertEquals("abc", githubSha256("sha256:ABC"))
        assertNull(githubSha256("sha512:abc"))
        assertNull(githubSha256("sha256:"))
        assertNull(githubSha256(""))
    }

    @Test
    fun aDownloadIsWholeWithTheSizeAndTheDigest() {
        assertTrue(githubDownloadIntact(apk, 48115930, digest))
    }

    @Test
    fun aCutOrChangedDownloadIsDamaged() {
        assertFalse(githubDownloadIntact(apk, 1000, digest))
        assertFalse(githubDownloadIntact(apk, 48115930, "0".repeat(64)))
    }

    @Test
    fun withoutADigestTheSizeDecides() {
        val undigested = apk.copy(sha256 = null)
        assertTrue(githubDownloadIntact(undigested, 48115930, "0".repeat(64)))
        assertFalse(githubDownloadIntact(undigested, 1000, "0".repeat(64)))
    }

    @Test
    fun theApkIsNamedForItsVersion() {
        assertEquals("CustomTV-free-1.7.0.apk", githubApkFileName(apk))
    }
}
