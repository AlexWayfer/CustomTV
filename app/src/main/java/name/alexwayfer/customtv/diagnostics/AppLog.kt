package name.alexwayfer.customtv.diagnostics

import android.content.Context
import android.util.Log
import name.alexwayfer.customtv.BuildConfig
import java.io.File
import java.time.ZoneId
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

/**
 * Writes to logcat like [Log] and keeps the latest lines in memory, so a problem report shows
 * what the app did just before the problem. The app only logs transitions and failures, never
 * secrets, so the lines can go into a report as they are.
 */
internal object AppLog {
    private val recent = RecentLog()
    @Volatile private var file: AppLogFile? = null
    private val fileWriter = Executors.newSingleThreadExecutor { task ->
        Thread(task, "AppLogFile").apply { isDaemon = true }
    }

    /**
     * Also writes every line to a file in the cache, starting with a mark for this process. The mark names the
     * build, so the file shows which version ran before an update and which after.
     */
    fun start(context: Context) {
        val logFile = AppLogFile(File(context.cacheDir, "logs"))
        file = logFile
        val edition = if (BuildConfig.PREMIUM) "premium" else "free"
        val build = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) $edition"
        val mark = recentLogLine(System.currentTimeMillis(), ZoneId.systemDefault(), 'I', "AppLog", "Process started $build")
        fileWriter.execute { runCatching { logFile.append(mark) } }
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        remember('I', tag, message)
    }

    fun w(tag: String, message: String) {
        Log.w(tag, message)
        remember('W', tag, message)
    }

    fun w(tag: String, message: String, error: Throwable) {
        Log.w(tag, message, error)
        remember('W', tag, "$message: ${error.javaClass.simpleName}")
    }

    fun e(tag: String, message: String, error: Throwable) {
        Log.e(tag, message, error)
        remember('E', tag, "$message: ${error.javaClass.simpleName}")
    }

    /** The latest lines, oldest first. */
    fun snapshot(): List<String> = recent.lines()

    /** Every line the log file keeps, oldest first, read after the lines still waiting to be written. */
    fun fileLines(): List<String> {
        val logFile = file ?: return emptyList()
        return try {
            fileWriter.submit<List<String>> { logFile.read() }.get()
        } catch (error: ExecutionException) {
            Log.w("AppLog", "log file read failed ${error.cause?.javaClass?.simpleName}")
            emptyList()
        }
    }

    private fun remember(level: Char, tag: String, message: String) {
        val line = recentLogLine(System.currentTimeMillis(), ZoneId.systemDefault(), level, tag, message)
        recent.add(line)
        val logFile = file ?: return
        fileWriter.execute { runCatching { logFile.append(line) } }
    }
}
