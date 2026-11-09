package name.alexwayfer.customtv.diagnostics

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Only the published sanitized file is supplied; sibling raw and temporary files are never traversed. */
internal fun memoryReportAttachment(report: File, structure: File?): File {
    if (structure == null) return report
    val archive = File(report.parentFile, "${report.nameWithoutExtension}.zip")
    var finished = false
    try {
        ZipOutputStream(archive.outputStream().buffered()).use { zip ->
            for (file in listOf(report, structure)) {
                zip.putNextEntry(ZipEntry(file.name))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        finished = true
    } finally {
        if (!finished) archive.delete()
    }
    return archive
}
