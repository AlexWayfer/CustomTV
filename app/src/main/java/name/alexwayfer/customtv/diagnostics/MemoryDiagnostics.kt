package name.alexwayfer.customtv.diagnostics

import android.app.Application
import android.os.Debug
import android.util.AtomicFile
import coil.Coil
import java.io.File
import java.io.IOException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.ui.components.SharedEmoteStore

/** Size counters only. Neither chat contents nor objects from the heap enter this history. */
internal object MemoryDiagnostics {
    private const val SAMPLE_COUNT = 60
    private val interval = 30.seconds
    private val history = MemorySampleHistory(SAMPLE_COUNT)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile var replayRows: Int = 0
    @Volatile var crashHistory: String = ""
        private set
    @Volatile private var previousHistory: String = ""

    fun start(application: Application) {
        scope.launch {
            val file = AtomicFile(File(application.filesDir, "diagnostics/memory.txt"))
            previousHistory = readPrevious(file)
            while (isActive) {
                sample(application)
                try {
                    writeHistory(file)
                } catch (error: IOException) {
                    AppLog.w("MemoryDiagnostics", "sample write failed ${error.javaClass.simpleName}")
                }
                delay(interval)
            }
        }
    }

    fun report(): String = "\nMemory samples (bytes unless marked KiB):\n" +
        listOf(previousHistory, crashHistory).filter(String::isNotBlank).joinToString("\n") + "\n"

    private fun sample(application: Application) {
        val runtime = Runtime.getRuntime()
        val memory = Debug.MemoryInfo()
        Debug.getMemoryInfo(memory)
        val line = MemorySample(
            System.currentTimeMillis(),
            runtime.totalMemory() - runtime.freeMemory(),
            runtime.maxMemory(),
            Debug.getNativeHeapAllocatedSize(),
            replayRows,
            Coil.imageLoader(application).memoryCache?.size?.toLong() ?: 0L,
        ).line()
        history.add(
            "$line pssKiB=${memory.totalPss} javaPssKiB=${memory.dalvikPss} " +
                "nativePssKiB=${memory.nativePss} emoteHolders=${SharedEmoteStore.retainedCount}",
        )
        crashHistory = history.snapshot().joinToString("\n")
    }

    private fun readPrevious(file: AtomicFile): String = try {
        if (file.baseFile.exists()) {
            file.openRead().bufferedReader().useLines { it.take(SAMPLE_COUNT).joinToString("\n") }
        } else {
            ""
        }
    } catch (error: IOException) {
        AppLog.w("MemoryDiagnostics", "sample read failed ${error.javaClass.simpleName}")
        ""
    }

    private fun writeHistory(file: AtomicFile) {
        val output = file.startWrite()
        try {
            output.write(crashHistory.toByteArray(Charsets.UTF_8))
            file.finishWrite(output)
        } catch (error: IOException) {
            file.failWrite(output)
            throw error
        }
    }
}
