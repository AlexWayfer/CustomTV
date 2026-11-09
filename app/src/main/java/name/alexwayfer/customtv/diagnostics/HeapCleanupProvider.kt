package name.alexwayfer.customtv.diagnostics

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Process
import java.io.File

internal const val HEAP_CLEANUP_PROCESS = ":heap_cleanup"
internal const val HEAP_CLEANUP_METHOD = "prepare"
internal const val HEAP_CLEANUP_AT = "captured_at"

internal fun isHeapCleanupProcess(processName: String, packageName: String): Boolean =
    processName == packageName + HEAP_CLEANUP_PROCESS

/** Private IPC endpoint; it exposes no files or database operations. */
class HeapCleanupProvider : ContentProvider() {
    private val store by lazy { HeapSnapshotStore(File(requireNotNull(context).noBackupFilesDir, "diagnostic-heap")) }

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        if (method != HEAP_CLEANUP_METHOD || extras == null) return Bundle.EMPTY
        val expectedAt = if (extras.containsKey(HEAP_CLEANUP_AT)) extras.getLong(HEAP_CLEANUP_AT) else null
        val priority = Process.getThreadPriority(Process.myTid())
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            store.prepare(expectedAt, ::sanitizeHeapStructure)
        } catch (_: OutOfMemoryError) {
            // This process has its own heap. Keep the complete original for a later cleanup attempt.
        } finally {
            Process.setThreadPriority(priority)
        }
        return Bundle.EMPTY
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = throw UnsupportedOperationException()
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException()
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException()
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException()
}
