package name.alexwayfer.customtv.diagnostics

import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.auth.twitchAuthMessage
import name.alexwayfer.customtv.data.HTTP_SUCCESS
import name.alexwayfer.customtv.data.gqlErrorsToReport
import name.alexwayfer.customtv.update.AppUpdates
import java.io.IOException
import java.time.ZoneId

/** The log tag of the diagnostics themselves, written straight to Android's log. */
internal const val DIAGNOSTICS_TAG = "Diagnostics"

/**
 * Keeps the problems a developer should hear about and offers the user to send them. Nothing
 * leaves the device unless the user shares the report.
 */
internal object Diagnostics {
    private const val NOTICED_AT = "noticed_at"
    private const val EXITS_READ_AT = "exits_read_at"
    private val lock = Any()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val rateLimitHits = mutableMapOf<String, List<Long>>()
    private lateinit var application: Application

    fun start(application: Application) {
        this.application = application
        val screens = DiagnosticScreenTracker()
        application.registerActivityLifecycleCallbacks(screens)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                // A failed read of the window must not cost the crash its record.
                val screen = runCatching { screens.current() }.getOrNull()
                DiagnosticJournal.record(application, crashEvent(thread, error, screen))
            } catch (_: Throwable) {
                // The crash itself must still reach the system handler.
            }
            previous?.uncaughtException(thread, error)
        }
        scope.launch {
            readFreezes()
            noticeUnseenExits()
        }
    }

    /**
     * Any 4xx answer: [expected] holds the codes that are routine for this request, and the rest
     * reach the report. [endpoint] names the request without IDs, so its repeats count as one.
     */
    fun reportHttp(source: String, endpoint: String, httpCode: Int, message: String, expected: Set<Int>) {
        when (httpDiagnostic(httpCode, expected)) {
            DiagnosticKind.Rejected -> reportRejected(source, endpoint, httpCode, message)
            DiagnosticKind.RateLimited -> reportRateLimited(source, endpoint)
            else -> Unit
        }
    }

    /**
     * Any answer to a GQL request through the public client: no login is involved, so every 4xx
     * is unexpected, and an HTTP 200 may still carry errors that need a fix.
     */
    fun reportGql(source: String, operation: String, httpCode: Int, body: String) {
        val endpoint = "gql $operation"
        if (httpCode !in HTTP_SUCCESS) {
            reportHttp(source, endpoint, httpCode, twitchAuthMessage(body), emptySet())
            return
        }
        val errors = gqlErrorsToReport(body)
        if (errors.isEmpty() || !::application.isInitialized) return
        Log.w(DIAGNOSTICS_TAG, "gql errors $source")
        recordAndNotice(
            DiagnosticEvent(
                kind = DiagnosticKind.GqlError,
                source = source,
                detail = endpoint,
                message = errors.joinToString("\n"),
                firstAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    /** A 4xx that sending again will not fix: the request itself needs a change. */
    fun reportRejected(source: String, endpoint: String, httpCode: Int, message: String) {
        if (!::application.isInitialized) return
        Log.w(DIAGNOSTICS_TAG, "rejected $source HTTP $httpCode")
        recordAndNotice(
            DiagnosticEvent(
                kind = DiagnosticKind.Rejected,
                source = source,
                detail = endpoint,
                httpCode = httpCode,
                message = message,
                firstAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    /** A 429 is routine alone; only a run of them within the window reaches the report. */
    fun reportRateLimited(source: String, endpoint: String) {
        if (!::application.isInitialized) return
        val now = System.currentTimeMillis()
        val tooOften = synchronized(lock) {
            val key = "$source $endpoint"
            val hit = rateLimitHit(rateLimitHits[key].orEmpty(), now)
            rateLimitHits[key] = hit.hits
            hit.tooOften
        }
        if (!tooOften) return
        Log.w(DIAGNOSTICS_TAG, "rate limited too often $source")
        recordAndNotice(
            DiagnosticEvent(
                kind = DiagnosticKind.RateLimited,
                source = source,
                detail = endpoint,
                httpCode = 429,
                firstAtMillis = now,
            ),
        )
    }

    private fun recordAndNotice(event: DiagnosticEvent) {
        if (DiagnosticJournal.record(application, event)) notice(event)
    }

    /**
     * The report as plain text, read when the user chooses to send it. [withAppLog] adds the whole log file, for a
     * report sent from the settings about a problem the app did not notice.
     */
    fun report(context: Context, withAppLog: Boolean): String =
        diagnosticReport(
            device = diagnosticDevice(context),
            accounts = diagnosticAccounts(context),
            events = DiagnosticJournal.eventsToSend(context),
            zone = ZoneId.systemDefault(),
            appLog = if (withAppLog) AppLog.fileLines() else emptyList(),
        )

    /** The message that goes with the report file. */
    fun shareText(context: Context): String =
        diagnosticShareText(diagnosticDevice(context), DiagnosticJournal.eventsToSend(context).size)

    /** Android keeps why the process ended; a freeze since the last start becomes an event. */
    private fun readFreezes() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val prefs = application.getSharedPreferences(DIAGNOSTICS_PREFS, Context.MODE_PRIVATE)
        val readAt = prefs.getLong(EXITS_READ_AT, -1L)
        val now = System.currentTimeMillis()
        // The first start only marks the time, so freezes from before this version are not reported.
        if (readAt >= 0) {
            val exits = application.getSystemService(ActivityManager::class.java)
                ?.getHistoricalProcessExitReasons(application.packageName, 0, 0)
                .orEmpty()
            exits.filter { it.reason == ApplicationExitInfo.REASON_ANR && it.timestamp > readAt }
                .sortedBy { it.timestamp }
                .forEach { exit ->
                    val main = try {
                        exit.traceInputStream?.bufferedReader()?.useLines(::anrMainThread).orEmpty()
                    } catch (error: IOException) {
                        Log.w(DIAGNOSTICS_TAG, "freeze trace failed ${error.javaClass.simpleName}")
                        ""
                    }
                    Log.w(DIAGNOSTICS_TAG, "freeze found")
                    DiagnosticJournal.record(
                        application,
                        DiagnosticEvent(
                            kind = DiagnosticKind.Anr,
                            source = "main",
                            detail = topStackFrame(main) ?: "unknown",
                            message = listOfNotNull(exit.description, main.ifBlank { null }).joinToString("\n"),
                            firstAtMillis = exit.timestamp,
                        ),
                    )
                }
        }
        prefs.edit { putLong(EXITS_READ_AT, now) }
    }

    private fun noticeUnseenExits() {
        val noticedAt = application.getSharedPreferences(DIAGNOSTICS_PREFS, Context.MODE_PRIVATE)
            .getLong(NOTICED_AT, 0L)
        unseenExitEvents(DiagnosticJournal.read(application), noticedAt).firstOrNull()?.let(::notice)
    }

    private fun notice(event: DiagnosticEvent) {
        Log.w(DIAGNOSTICS_TAG, "problem notice ${event.kind}")
        application.getSharedPreferences(DIAGNOSTICS_PREFS, Context.MODE_PRIVATE)
            .edit { putLong(NOTICED_AT, System.currentTimeMillis()) }
        showDiagnosticNotification(
            application,
            DiagnosticJournal.eventsToSend(application).map(::diagnosticHeadline)
                .ifEmpty { listOf(diagnosticHeadline(event)) },
        )
        AppUpdates.afterProblem()
    }
}
