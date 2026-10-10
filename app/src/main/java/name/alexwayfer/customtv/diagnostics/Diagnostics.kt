package name.alexwayfer.customtv.diagnostics

import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.BuildConfig
import name.alexwayfer.customtv.auth.twitchAuthMessage
import name.alexwayfer.customtv.data.TwitchSessionStore
import name.alexwayfer.customtv.data.gqlErrorsToReport
import name.alexwayfer.customtv.telegram.TelegramSession
import java.io.File
import java.io.IOException
import java.time.ZoneId

/**
 * Keeps the problems a developer should hear about and offers the user to send them. Nothing
 * leaves the device unless the user shares the report.
 */
internal object Diagnostics {
    private const val TAG = "Diagnostics"
    private const val PREFS = "diagnostics"
    private const val NOTICED_AT = "noticed_at"
    private const val EXITS_READ_AT = "exits_read_at"
    private const val SENT_AT = "sent_at"
    private val lock = Any()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val rateLimitHits = mutableMapOf<String, List<Long>>()
    private lateinit var application: Application

    fun start(application: Application) {
        this.application = application
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                record(crashEvent(thread, error))
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
        if (httpCode !in 200..299) {
            reportHttp(source, endpoint, httpCode, twitchAuthMessage(body), emptySet())
            return
        }
        val errors = gqlErrorsToReport(body)
        if (errors.isEmpty() || !::application.isInitialized) return
        Log.w(TAG, "gql errors $source")
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
        Log.w(TAG, "rejected $source HTTP $httpCode")
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
        Log.w(TAG, "rate limited too often $source")
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
        if (record(event)) notice(event)
    }

    /**
     * The report as plain text, read when the user chooses to send it. [withAppLog] adds the whole log file, for a
     * report sent from the settings about a problem the app did not notice.
     */
    fun report(context: Context, withAppLog: Boolean): String {
        val account = TwitchSessionStore(context).read()?.account
        return diagnosticReport(
            device = device(context),
            accounts = DiagnosticAccounts(
                twitchLogin = account?.login,
                twitchUserId = account?.userId,
                telegramUsername = TelegramSession.ui.value.accountUsername,
            ),
            events = diagnosticEventsToSend(read(context), sentAt(context)),
            zone = ZoneId.systemDefault(),
            appLog = if (withAppLog) AppLog.fileLines() else emptyList(),
        )
    }

    /** The message that goes with the report file. */
    fun shareText(context: Context): String =
        diagnosticShareText(device(context), diagnosticEventsToSend(read(context), sentAt(context)).size)

    /** The headlines of the problems the next report holds, newest first. */
    fun headlinesToSend(context: Context): List<String> =
        diagnosticEventsToSend(read(context), sentAt(context)).map(::diagnosticHeadline)

    /** The report went to the share sheet: the next one starts after it, and a repeat is news again. */
    fun markSent(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putLong(SENT_AT, System.currentTimeMillis()) }
    }

    private fun sentAt(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(SENT_AT, 0L)

    private fun device(context: Context) = DiagnosticDevice(
        appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        premium = BuildConfig.PREMIUM,
        androidRelease = Build.VERSION.RELEASE,
        sdk = Build.VERSION.SDK_INT,
        manufacturer = Build.MANUFACTURER,
        model = Build.MODEL,
        // Without the regional preferences, such as the first day of the week, that ride along as extensions.
        languages = Resources.getSystem().configuration.locales.let { locales ->
            (0 until locales.size()).map { locales[it].stripExtensions().toLanguageTag() }.distinct()
        },
        simCountry = context.getSystemService(TelephonyManager::class.java)?.simCountryIso,
        debug = BuildConfig.DEBUG,
        installedAtMillis = diagnosticInstalledAt(
            packageUpdatedMillis = packageUpdatedMillis(context),
            // Android Studio deploys a changed debug build into this overlay without reinstalling the APK.
            studioOverlayMillis = File(context.codeCacheDir, ".overlay/id").lastModified(),
        ),
    )

    private fun packageUpdatedMillis(context: Context): Long? = try {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        info.lastUpdateTime
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    private fun crashEvent(thread: Thread, error: Throwable): DiagnosticEvent {
        val frame = error.stackTrace.firstOrNull()?.toString()
        return DiagnosticEvent(
            kind = DiagnosticKind.Crash,
            source = thread.name,
            detail = listOfNotNull(error.javaClass.name, frame?.let { "at $it" }).joinToString(" "),
            message = Log.getStackTraceString(error),
            firstAtMillis = System.currentTimeMillis(),
        )
    }

    /** Android keeps why the process ended; a freeze since the last start becomes an event. */
    private fun readFreezes() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val prefs = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
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
                        Log.w(TAG, "freeze trace failed ${error.javaClass.simpleName}")
                        ""
                    }
                    Log.w(TAG, "freeze found")
                    record(
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
        val noticedAt = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(NOTICED_AT, 0L)
        unseenExitEvents(read(application), noticedAt).firstOrNull()?.let(::notice)
    }

    private fun notice(event: DiagnosticEvent) {
        Log.w(TAG, "problem notice ${event.kind}")
        application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putLong(NOTICED_AT, System.currentTimeMillis()) }
        showDiagnosticNotification(application, headlinesToSend(application).ifEmpty { listOf(diagnosticHeadline(event)) })
    }

    /** Adds [event] to the journal; true when the journal did not hold this problem yet. */
    private fun record(event: DiagnosticEvent): Boolean = synchronized(lock) {
        val file = journal(application)
        val previous = read(application)
        // A freeze is found after a restart, when the lines before it went with the old process.
        val withLog = if (event.kind == DiagnosticKind.Anr) event else event.copy(recentLog = AppLog.snapshot())
        try {
            file.parentFile?.mkdirs()
            file.writeText(encodeDiagnosticEvents(recordDiagnosticEvent(previous, withLog)))
        } catch (error: IOException) {
            Log.w(TAG, "journal write failed ${error.javaClass.simpleName}")
        }
        diagnosticNoticeDue(previous, event, sentAt(application))
    }

    private fun read(context: Context): List<DiagnosticEvent> = synchronized(lock) {
        val file = journal(context)
        if (!file.exists()) return emptyList()
        try {
            decodeDiagnosticEvents(file.readText())
        } catch (error: IOException) {
            Log.w(TAG, "journal read failed ${error.javaClass.simpleName}")
            emptyList()
        }
    }

    private fun journal(context: Context): File = File(context.filesDir, "diagnostics/journal.json")
}
