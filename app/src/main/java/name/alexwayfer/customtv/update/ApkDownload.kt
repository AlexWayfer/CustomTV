package name.alexwayfer.customtv.update

import java.io.File
import java.io.IOException
import java.security.MessageDigest
import okhttp3.ResponseBody

private const val PART_SUFFIX = ".part"
private const val BUFFER_BYTES = 64 * 1024
private const val HEX_DIGITS = "0123456789abcdef"
private const val BYTE_MASK = 0xFF
private const val NIBBLE_BITS = 4
private const val NIBBLE_MASK = 0x0F

/** The file the download writes until it is checked; only a checked file takes the APK's own name. */
internal fun partFileName(apkFileName: String): String = apkFileName + PART_SUFFIX

/**
 * The files in the updates folder to delete after a check: everything but the offered APK and its download in
 * progress, so an installed or skipped release does not stay on the device.
 */
internal fun staleUpdateFiles(names: List<String>, keptApk: String?): List<String> {
    val kept = keptApk?.let { setOf(it, partFileName(it)) }.orEmpty()
    return names.filterNot { it in kept }
}

/**
 * Writes [body] into a part file next to [target] and gives it the target's name only once [intact] accepts its size
 * and SHA-256, passing the progress of [total] bytes to [onProgress]. Returns the APK, or null when it was damaged;
 * the part file goes in both that case and a failed write.
 */
internal fun saveCheckedApk(
    body: ResponseBody,
    target: File,
    total: Long,
    intact: (size: Long, sha256: String) -> Boolean,
    onProgress: (Float) -> Unit,
): File? {
    val part = File(target.parentFile, partFileName(target.name))
    val sha256 = try {
        copyWithProgress(body, part, total, onProgress)
    } catch (error: IOException) {
        part.delete()
        throw error
    }
    if (intact(part.length(), sha256) && part.renameTo(target)) return target
    part.delete()
    return null
}

/**
 * Writes [body] to [file], passing its progress of [total] bytes to [onProgress], and returns the file's SHA-256 in
 * lowercase hex.
 */
private fun copyWithProgress(body: ResponseBody, file: File, total: Long, onProgress: (Float) -> Unit): String {
    val digest = MessageDigest.getInstance("SHA-256")
    var copied = 0L
    var shownPercent = 0
    body.byteStream().use { input ->
        file.outputStream().use { output ->
            val buffer = ByteArray(BUFFER_BYTES)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                output.write(buffer, 0, read)
                digest.update(buffer, 0, read)
                copied += read
                val fraction = downloadFraction(copied, total)
                // One update per percent: the progress bar shows no finer step.
                val percent = (fraction * 100).toInt()
                if (percent != shownPercent) {
                    shownPercent = percent
                    onProgress(fraction)
                }
            }
        }
    }
    return digest.digest().toLowerHex()
}

internal fun ByteArray.toLowerHex(): String = buildString(size * 2) {
    for (byte in this@toLowerHex) {
        val value = byte.toInt() and BYTE_MASK
        append(HEX_DIGITS[value ushr NIBBLE_BITS])
        append(HEX_DIGITS[value and NIBBLE_MASK])
    }
}
