package name.alexwayfer.customtv.diagnostics

import java.io.File
import java.util.zip.ZipFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MemoryReportAttachmentTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test
    fun `without a snapshot the report remains a plain text attachment`() {
        val report = temporary.newFile("report.txt")
        assertSame(report, memoryReportAttachment(report, null))
    }

    @Test
    fun `the archive includes the text and only the published structure file`() {
        val report = temporary.newFile("report.txt").apply { writeText("numeric-report") }
        val structure = temporary.newFile("memory-structure.hprof.gz").apply { writeText("sanitized") }
        temporary.newFile("heap.raw").writeText("private-token")
        val attachment = memoryReportAttachment(report, structure)
        assertEquals("report.zip", attachment.name)
        ZipFile(attachment).use { archive ->
            assertEquals(setOf(report.name, structure.name), archive.entries().asSequence().map { it.name }.toSet())
            assertEquals("numeric-report", archive.getInputStream(archive.getEntry(report.name)).reader().readText())
            assertEquals("sanitized", archive.getInputStream(archive.getEntry(structure.name)).reader().readText())
        }
        assertFalse(attachment.readBytes().toString(Charsets.ISO_8859_1).contains("private-token"))
    }

    @Test
    fun `an interrupted archive is not left as an apparently sendable report`() {
        val report = temporary.newFile("report.txt")
        val missing = File(temporary.root, "memory-structure.hprof.gz")
        org.junit.Assert.assertThrows(java.io.IOException::class.java) { memoryReportAttachment(report, missing) }
        assertFalse(File(temporary.root, "report.zip").exists())
    }
}
