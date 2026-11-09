package name.alexwayfer.customtv.diagnostics

import android.app.Application
import android.content.ComponentName
import android.os.Bundle
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class HeapCleanupProviderTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val folder get() = File(app.noBackupFilesDir, "diagnostic-heap")

    @Before
    fun cleanBefore() { folder.deleteRecursively() }

    @After
    fun cleanAfter() { folder.deleteRecursively() }

    @Test
    fun `the provider is private and runs in the dedicated cleanup process`() {
        val info = app.packageManager.getProviderInfo(ComponentName(app, HeapCleanupProvider::class.java), 0)
        assertFalse(info.exported)
        assertEquals(app.packageName + HEAP_CLEANUP_PROCESS, info.processName)
        assertTrue(isHeapCleanupProcess(info.processName, app.packageName))
        assertFalse(isHeapCleanupProcess(app.packageName, app.packageName))
    }

    @Test
    fun `the worker cleans only the recorded pending dump`() {
        val store = HeapSnapshotStore(folder)
        store.capture("oom", 10) { it.writeBytes(HeapStructureFixture(4).bytes) }
        val provider = Robolectric.buildContentProvider(HeapCleanupProvider::class.java).create().get()
        provider.call(HEAP_CLEANUP_METHOD, null, Bundle().apply { putLong(HEAP_CLEANUP_AT, 10) })
        assertNotNull(store.attachment())
        assertFalse(File(folder, "heap.raw").exists())
    }

    @Test
    fun `a stale cleanup request cannot touch a newer dump`() {
        val store = HeapSnapshotStore(folder)
        store.capture("oom", 20) { it.writeBytes(HeapStructureFixture(4).bytes) }
        val provider = Robolectric.buildContentProvider(HeapCleanupProvider::class.java).create().get()
        provider.call(HEAP_CLEANUP_METHOD, null, Bundle().apply { putLong(HEAP_CLEANUP_AT, 10) })
        assertNull(store.attachment())
        assertTrue(File(folder, "heap.raw").exists())
    }
}
