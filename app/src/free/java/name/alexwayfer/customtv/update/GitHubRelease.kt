package name.alexwayfer.customtv.update

import org.json.JSONException
import org.json.JSONObject

private const val GITHUB_REPOSITORY = "AlexWayfer/CustomTV"
internal const val GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/$GITHUB_REPOSITORY/releases/latest"
internal const val GITHUB_RELEASES_PAGE_URL = "https://github.com/$GITHUB_REPOSITORY/releases"

private const val SHA256_PREFIX = "sha256:"
private const val PART_SUFFIX = ".part"
private const val HEX_DIGITS = "0123456789abcdef"
private const val BYTE_MASK = 0xFF
private const val NIBBLE_BITS = 4
private const val NIBBLE_MASK = 0x0F

/** The APK of the latest free release on GitHub. */
internal data class GitHubApk(
    /** GitHub's ID of the release asset. */
    val id: Long,
    val version: AppVersion,
    val url: String,
    val size: Long,
    /** The file's SHA-256 in lowercase hex, as GitHub reports it; null for an asset it has no digest for. */
    val sha256: String?,
)

/** The first finished APK asset of the latest release that names a version; null when there is none. */
internal fun githubLatestApk(json: String): GitHubApk? {
    val assets = try {
        JSONObject(json).optJSONArray("assets")
    } catch (_: JSONException) {
        null
    } ?: return null
    return (0 until assets.length()).asSequence()
        .mapNotNull { assets.optJSONObject(it) }
        .firstNotNullOfOrNull(::githubApk)
}

private fun githubApk(asset: JSONObject): GitHubApk? {
    if (asset.optString("state") != "uploaded") return null
    val url = asset.optString("browser_download_url").takeIf { it.startsWith("https://") } ?: return null
    val release = appRelease(
        id = asset.optLong("id"),
        fileName = asset.optString("name"),
        mimeType = asset.optString("content_type"),
        downloadedPath = null,
    ) ?: return null
    return GitHubApk(
        id = release.id,
        version = release.version,
        url = url,
        size = asset.optLong("size"),
        sha256 = githubSha256(asset.optString("digest")),
    )
}

/** GitHub writes a digest as `sha256:<hex>`; another algorithm or none gives null. */
internal fun githubSha256(digest: String): String? {
    if (!digest.startsWith(SHA256_PREFIX)) return null
    return digest.removePrefix(SHA256_PREFIX).lowercase().takeIf { it.isNotEmpty() }
}

/** A whole download has the size GitHub names, and its digest when GitHub has one. */
internal fun githubDownloadIntact(apk: GitHubApk, size: Long, sha256: String): Boolean =
    size == apk.size && (apk.sha256 == null || apk.sha256 == sha256)

/** The downloaded APK's file name: its version, so a newer release does not reuse an older file. */
internal fun githubApkFileName(apk: GitHubApk): String = "CustomTV-free-${apk.version}.apk"

/** The file the download writes until it is checked; only a checked file takes the APK's own name. */
internal fun githubPartFileName(apk: GitHubApk): String = githubApkFileName(apk) + PART_SUFFIX

/**
 * The files in the updates folder to delete after a check: everything but the offered APK and its download in
 * progress, so an installed or skipped release does not stay on the device.
 */
internal fun staleUpdateFiles(names: List<String>, offered: GitHubApk?): List<String> {
    val kept = offered?.let { setOf(githubApkFileName(it), githubPartFileName(it)) }.orEmpty()
    return names.filterNot { it in kept }
}

internal fun ByteArray.toLowerHex(): String = buildString(size * 2) {
    for (byte in this@toLowerHex) {
        val value = byte.toInt() and BYTE_MASK
        append(HEX_DIGITS[value ushr NIBBLE_BITS])
        append(HEX_DIGITS[value and NIBBLE_MASK])
    }
}
