package name.alexwayfer.customtv.diagnostics

import java.io.File
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.RandomAccessFile

class HeapSnapshotStoreTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test
    fun `only an OOM starts a capture and later OOMs leave the original intact`() {
        val store = HeapSnapshotStore(temporary.newFolder())
        var captures = 0
        val dump: (File) -> Unit = { captures++; it.writeText("first") }
        store.captureOnCrash(IllegalStateException(), 10, dump)
        assertEquals(0, captures)
        store.captureOnCrash(OutOfMemoryError(), 20, dump)
        assertEquals(1, captures)
        store.captureOnCrash(OutOfMemoryError(), 30, dump)
        store.prepare { input, output -> output.writeText(input.readText()) }
        assertEquals(1, captures)
        assertEquals("first", store.attachment()?.readText())
    }

    @Test
    fun `another process holding the file lock prevents cleanup and sharing`() {
        val folder = temporary.newFolder()
        val store = HeapSnapshotStore(folder)
        store.capture("oom", 10) { it.writeText("first") }
        var cleanups = 0
        RandomAccessFile(File(folder, "guard.lock"), "rw").use { guard ->
            guard.channel.lock().use {
                store.prepare { _, output -> cleanups++; output.writeText("clean") }
                assertNull(store.forReport().capturedAt)
                store.markShared(10)
            }
        }
        assertEquals(0, cleanups)
        assertTrue(File(folder, "heap.raw").exists())
    }

    @Test
    fun `an OOM never waits for another process or starts an overlapping capture`() {
        val folder = temporary.newFolder()
        val store = HeapSnapshotStore(folder)
        var captures = 0
        RandomAccessFile(File(folder, "guard.lock"), "rw").use { guard ->
            guard.channel.lock().use {
                store.captureOnCrash(OutOfMemoryError(), 10) { file -> captures++; file.writeText("raw") }
            }
        }
        assertEquals(0, captures)
        assertNull(store.capturedAt)
        store.captureOnCrash(OutOfMemoryError(), 20) { captures++; it.writeText("raw") }
        assertEquals(1, captures)
    }

    @Test
    fun `an OOM during cleanup preserves the complete original for the next process`() {
        val folder = temporary.newFolder()
        val store = HeapSnapshotStore(folder)
        store.capture("oom", 10) { it.writeText("original") }
        assertThrows(OutOfMemoryError::class.java) {
            store.prepare { _, output -> output.writeText("partial"); throw OutOfMemoryError() }
        }
        assertNull(store.attachment())
        assertTrue(File(folder, "heap.raw").exists())
        assertTrue(File(folder, "raw-complete").exists())
        HeapSnapshotStore(folder).prepare { input, output -> output.writeText("clean-${input.readText()}") }
        assertEquals("clean-original", store.attachment()?.readText())
    }

    @Test
    fun `an OOM and a restart cannot replace a ready snapshot`() {
        val folder = temporary.newFolder()
        val store = HeapSnapshotStore(folder)
        var dumps = 0
        store.capture("pressure", 10) { it.writeText("first"); dumps++ }
        store.prepare { input, output -> output.writeText("clean-${input.readText()}") }
        store.capture("oom", 20) { it.writeText("bad"); dumps++ }
        val restarted = HeapSnapshotStore(folder)
        restarted.capture("pressure", 30) { it.writeText("empty"); dumps++ }
        restarted.prepare { _, _ -> error("Already prepared") }

        assertEquals(1, dumps)
        assertEquals("clean-first", restarted.attachment()?.readText())
        assertTrue(restarted.report().contains("pressure"))
        assertTrue(restarted.report().contains("10"))
    }

    @Test
    fun `an interrupted cleanup resumes after restart without another dump`() {
        val folder = temporary.newFolder()
        HeapSnapshotStore(folder).capture("oom", 10) { it.writeText("private") }
        val restarted = HeapSnapshotStore(folder)
        assertNull(restarted.attachment())
        restarted.prepare { input, output ->
            assertEquals("private", input.readText())
            output.writeText("structure")
        }
        assertEquals("structure", restarted.attachment()?.readText())
        assertFalse(File(folder, "heap.raw").exists())
    }

    @Test
    fun `failed cleanup never publishes raw or partial data or rearms on restart`() {
        val folder = temporary.newFolder()
        val store = HeapSnapshotStore(folder)
        store.capture("oom", 10) { it.writeText("private-token") }
        store.prepare { _, output -> output.writeText("partial-private"); throw IOException("private-error") }
        assertNull(store.attachment())
        assertFalse(File(folder, "heap.raw").exists())
        assertFalse(store.report().contains("private"))
        HeapSnapshotStore(folder).capture("pressure", 30) { error("Must stay reserved") }
    }

    @Test
    fun `failed capture never loses the reservation or exposes a partial dump`() {
        val folder = temporary.newFolder()
        val store = HeapSnapshotStore(folder)
        store.capture("oom", 10) { it.writeText("partial"); throw OutOfMemoryError("private") }
        assertNull(store.attachment())
        assertFalse(File(folder, "heap.raw").exists())
        HeapSnapshotStore(folder).capture("pressure", 20) { error("No automatic retry") }
    }

    @Test
    fun `only handing off the matching report allows a later capture`() {
        val store = HeapSnapshotStore(temporary.newFolder())
        store.capture("pressure", 10) { it.writeText("raw") }
        store.prepare { _, output -> output.writeText("clean") }
        store.markShared(20)
        assertEquals("clean", store.attachment()?.readText())
        store.markShared(10)
        assertNull(store.attachment())
        store.capture("pressure", 30) { it.writeText("new") }
        store.prepare { _, output -> output.writeText("new-clean") }
        assertEquals("new-clean", store.attachment()?.readText())
    }

    @Test
    fun `a report references only its own ready snapshot and its reservation`() {
        val store = HeapSnapshotStore(temporary.newFolder())
        store.capture("pressure", 10) { it.writeText("raw") }
        store.prepare { _, output -> output.writeText("clean") }
        val report = store.forReport()
        assertEquals("clean", report.file?.readText())
        assertEquals(10L, report.capturedAt)
        assertTrue(report.detail.contains("attachment=memory-structure.hprof.gz"))
    }

    @Test
    fun `an earlier report cannot consume a dump created while it was being prepared`() {
        val store = HeapSnapshotStore(temporary.newFolder())
        val earlier = store.forReport()
        assertNull(earlier.capturedAt)
        store.capture("pressure", 20) { it.writeText("raw") }
        val pending = store.forReport()
        assertNull(pending.capturedAt)
        assertNull(pending.file)
        assertTrue(pending.detail.contains("awaiting sanitization"))
        store.prepare { _, output -> output.writeText("new-clean") }
        assertEquals(20L, store.forReport().capturedAt)
    }
}
