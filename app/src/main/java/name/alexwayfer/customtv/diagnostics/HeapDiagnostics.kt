package name.alexwayfer.customtv.diagnostics

import android.app.Application
import android.os.Debug
import android.os.Bundle
import android.os.RemoteException
import androidx.core.net.toUri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Raw dumps stay under noBackupFilesDir, outside the FileProvider's reports directory. */
internal object HeapDiagnostics {
    private lateinit var store: HeapSnapshotStore
    private lateinit var application: Application
    private var startupSnapshot: Long? = null
    private val cleanup = Mutex()

    fun start(application: Application) {
        this.application = application
        store = HeapSnapshotStore(File(application.noBackupFilesDir, "diagnostic-heap"))
        startupSnapshot = store.capturedAt
    }

    /** Save first and sanitize after restart: the OOM handler should not index a heap with no memory left. */
    fun onCrash(error: Throwable) {
        store.captureOnCrash(error, System.currentTimeMillis()) { Debug.dumpHprofData(it.absolutePath) }
    }

    /** Waits for IPC, never runs the sanitizer in the playback process. Only recovers a dump present at startup. */
    suspend fun prepare(): Unit = cleanup.withLock {
        if (!heapCleanupDue(startupSnapshot, store.capturedAt, store.needsPreparation)) return@withLock
        withContext(Dispatchers.IO) {
            val extras = Bundle().apply { startupSnapshot?.let { putLong(HEAP_CLEANUP_AT, it) } }
            cleanupInWorker(extras)
        }
    }

    private fun cleanupInWorker(extras: Bundle) {
        val uri = "content://${application.packageName}.heap-cleanup".toUri()
        try {
            // An unstable client lets the worker die without Android killing its playback client as well.
            application.contentResolver.acquireUnstableContentProviderClient(uri)?.use { client ->
                client.call(HEAP_CLEANUP_METHOD, null, extras)
            }
        } catch (error: RemoteException) {
            logCleanupFailure(error)
        } catch (error: SecurityException) {
            logCleanupFailure(error)
        } catch (error: IllegalArgumentException) {
            logCleanupFailure(error)
        }
    }

    private fun logCleanupFailure(error: Exception) {
        AppLog.w("HeapDiagnostics", "cleanup IPC failed ${error.javaClass.simpleName}")
    }

    fun forReport(): HeapReportSnapshot = store.forReport()

    fun markShared(at: Long?) {
        if (at != null) store.markShared(at)
    }
}
