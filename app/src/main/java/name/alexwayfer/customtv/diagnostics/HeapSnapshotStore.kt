package name.alexwayfer.customtv.diagnostics

import java.io.File
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/** A reservation survives failures and process death. Only handing off its report rearms capture. */
internal class HeapSnapshotStore(folder: File) {
    private val lock = ReentrantLock()
    private val access = HeapSnapshotAccess(folder)
    private val reservation = File(folder, "pending.txt")
    private val raw = File(folder, "heap.raw")
    private val complete = File(folder, "raw-complete")
    private val temporary = File(folder, "structure.tmp")
    private val ready = File(folder, "memory-structure.hprof.gz")

    val capturedAt: Long? get() = lock.withLock {
        readReservation()?.substringBefore(' ')?.toLongOrNull()
            ?: reservation.takeIf { it.exists() }?.lastModified()
    }

    val needsPreparation: Boolean get() = lock.withLock { raw.exists() || complete.exists() || temporary.exists() }

    fun captureOnCrash(error: Throwable, at: Long, dump: (File) -> Unit) {
        if (error is OutOfMemoryError) capture("oom", at, dump)
    }

    /** Never wait behind a running capture from an uncaught-exception handler. */
    fun capture(reason: String, at: Long, dump: (File) -> Unit) {
        if (!lock.tryLock()) return
        try {
            access.locked {
                if (reservation.exists() || ready.exists()) return@locked
                if (!reservation.createNewFile()) return@locked
                reservation.outputStream().use { output ->
                    output.write("$at $reason".toByteArray(Charsets.UTF_8))
                    output.fd.sync()
                }
                captureRaw(dump)
            }
        } finally {
            lock.unlock()
        }
    }

    private fun captureRaw(dump: (File) -> Unit) {
        try {
            dump(raw)
            complete.writeText("")
        } catch (_: Throwable) {
            // Diagnostics must not replace the original OOM. Keep the reservation even on failure.
            raw.delete()
            complete.delete()
        }
    }

    /** Only the worker process calls this in production. Stale requests leave every file untouched. */
    fun prepare(expectedAt: Long? = capturedAt, sanitize: (File, File) -> Unit): Unit = lock.withLock {
        access.locked {
            if (capturedAt == expectedAt) prepareLocked(sanitize)
        }
    }

    private fun prepareLocked(sanitize: (File, File) -> Unit) {
        var discardRaw = true
        try {
            if (!reservation.exists() || ready.exists()) return
            if (complete.exists() && raw.exists()) {
                sanitize(raw, temporary)
                if (!temporary.isFile || temporary.length() == 0L || !temporary.renameTo(ready)) {
                    throw IOException("Snapshot publication failed")
                }
            }
        } catch (_: IOException) {
            // A corrupt or unrecognized dump is never published. The report still explains the failed attempt.
        } catch (error: OutOfMemoryError) {
            discardRaw = false
            throw error
        } finally {
            if (discardRaw) {
                raw.delete()
                complete.delete()
            }
            temporary.delete()
        }
    }

    /** The raw and temporary files are deliberately never exposed by this API. */
    fun attachment(): File? = lock.withLock { ready.takeIf { it.isFile && it.length() > 0 } }

    /** One atomic view prevents an earlier report from consuming a newer, not-yet-attached dump. */
    fun forReport(): HeapReportSnapshot = lock.withLock {
        access.locked {
            val file = attachment()
            val finished = file != null || (!raw.exists() && !complete.exists())
            HeapReportSnapshot(file, capturedAt.takeIf { finished }, report())
        } ?: HeapReportSnapshot(null, null, "\nHeap structure snapshot: cleanup in progress; not attached.\n")
    }

    fun report(): String = lock.withLock {
        if (!reservation.exists()) return ""
        val request = readReservation() ?: "unknown"
        val state = when {
            ready.isFile -> "ready; attachment=memory-structure.hprof.gz"
            complete.exists() -> "awaiting sanitization"
            else -> "unavailable; capture or sanitization did not complete"
        }
        "\nHeap structure snapshot: $request $state\n" +
            "Only class/field names, object references and sizes are shared; runtime values are removed.\n"
    }

    fun markShared(at: Long): Unit = lock.withLock {
        access.locked {
            if (capturedAt != at) return@locked
            ready.delete()
            raw.delete()
            complete.delete()
            temporary.delete()
            reservation.delete()
        }
    }

    private fun readReservation(): String? = try {
        if (reservation.exists()) reservation.readText().takeIf { it.isNotBlank() } else null
    } catch (_: IOException) {
        null
    }
}

internal data class HeapReportSnapshot(val file: File?, val capturedAt: Long?, val detail: String)
