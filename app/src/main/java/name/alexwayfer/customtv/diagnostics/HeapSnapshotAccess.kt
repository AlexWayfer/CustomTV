package name.alexwayfer.customtv.diagnostics

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.OverlappingFileLockException

/** Shared by both app processes. An OOM handler must never wait for the cleanup process. */
internal class HeapSnapshotAccess(private val folder: File) {
    fun <T> locked(action: () -> T): T? = try {
        folder.mkdirs()
        RandomAccessFile(File(folder, "guard.lock"), "rw").use { guard ->
            guard.channel.tryLock()?.use { action() }
        }
    } catch (_: OverlappingFileLockException) {
        null
    } catch (_: IOException) {
        null
    }
}
