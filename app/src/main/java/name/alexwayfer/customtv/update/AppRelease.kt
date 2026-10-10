package name.alexwayfer.customtv.update

import name.alexwayfer.customtv.ui.UiText

private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

/** An APK posted in the edition's Telegram topic. */
internal data class AppRelease(
    val fileId: Int,
    val version: AppVersion,
    /** Set when Telegram already holds the whole file on this device. */
    val downloadedPath: String?,
)

/** A posted file counts as a release only when it is an APK and its file name names a version. */
internal fun appRelease(
    fileId: Int,
    fileName: String,
    mimeType: String,
    downloadedPath: String?,
): AppRelease? {
    val apk = mimeType == APK_MIME_TYPE || fileName.endsWith(".apk", ignoreCase = true)
    if (!apk) return null
    val version = AppVersion.ofRelease(fileName) ?: return null
    return AppRelease(fileId, version, downloadedPath?.takeIf { it.isNotEmpty() })
}

/** The newest posted release when it is newer than the installed build. */
internal fun newestRelease(releases: List<AppRelease>, installed: AppVersion): AppRelease? =
    releases.maxByOrNull { it.version }?.takeIf { it.version > installed }

/** Downloaded releases that are not newer than the installed build, so their files can go. */
internal fun installedDownloads(releases: List<AppRelease>, installed: AppVersion): List<AppRelease> =
    releases.filter { it.downloadedPath != null && it.version <= installed }

/** How much of a file is downloaded, from 0 to 1. Telegram may know only the expected size. */
internal fun downloadFraction(downloaded: Long, size: Long, expectedSize: Long): Float {
    val total = if (size > 0) size else expectedSize
    if (total <= 0) return 0f
    return (downloaded.toDouble() / total).toFloat().coerceIn(0f, 1f)
}

internal sealed class AppUpdateDownload {
    data object Idle : AppUpdateDownload()
    data class Running(val fileId: Int, val fraction: Float) : AppUpdateDownload()
    data class Ready(val fileId: Int, val path: String) : AppUpdateDownload()
    data class Failed(val fileId: Int, val message: UiText) : AppUpdateDownload()
}

internal sealed class AppUpdateAction {
    /** [error] is why the last download of this release failed. */
    data class Download(val error: UiText?) : AppUpdateAction()
    data class Downloading(val fraction: Float) : AppUpdateAction()
    data class Install(val path: String) : AppUpdateAction()
}

/** What the update block offers for [release]; a download of another file does not count. */
internal fun appUpdateAction(release: AppRelease, download: AppUpdateDownload): AppUpdateAction = when {
    download is AppUpdateDownload.Running && download.fileId == release.fileId ->
        AppUpdateAction.Downloading(download.fraction)
    download is AppUpdateDownload.Ready && download.fileId == release.fileId ->
        AppUpdateAction.Install(download.path)
    release.downloadedPath != null -> AppUpdateAction.Install(release.downloadedPath)
    download is AppUpdateDownload.Failed && download.fileId == release.fileId ->
        AppUpdateAction.Download(download.message)
    else -> AppUpdateAction.Download(null)
}
