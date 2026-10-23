package name.alexwayfer.customtv.update

import android.content.Context
import android.os.SystemClock
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.BuildConfig
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.HTTP_FORBIDDEN
import name.alexwayfer.customtv.data.sharedHttpClient
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import name.alexwayfer.customtv.ui.UiText
import okhttp3.Request
import okhttp3.ResponseBody
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The log tag of the free build's update check and download. */
private const val GITHUB_TAG = "GitHub"

// The same wait the Premium check gives Telegram.
private val CHECK_TIMEOUT = 30.seconds

private const val BUFFER_BYTES = 64 * 1024

/**
 * GitHub limits API calls without a token per address, and a shared mobile address can reach the limit. It
 * answers 403 or 429 then; a 429 already counts as a rate limit in the report.
 */
private val GITHUB_ROUTINE_CODES = setOf(HTTP_FORBIDDEN)

/**
 * The free build's updates come from its public GitHub releases: no account is needed. A check runs at start,
 * from the Check updates button, for the daily worker, and after a problem notice; the APK downloads into the cache
 * and installs from there.
 */
internal object AppUpdates {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private val checkHttp by lazy { sharedHttpClient.newBuilder().callTimeout(CHECK_TIMEOUT).build() }

    private val _status = MutableStateFlow<AppUpdateStatus>(AppUpdateStatus.Checking)
    val status: StateFlow<AppUpdateStatus> = _status.asStateFlow()

    private val _download = MutableStateFlow<AppUpdateDownload>(AppUpdateDownload.Idle)
    val download: StateFlow<AppUpdateDownload> = _download.asStateFlow()

    private var appContext: Context? = null
    private var runningCheck: Job? = null

    /** Whether the running check posts the update notification; a check that does joins one that does not. */
    @Volatile
    private var notifyRunning = false

    /** `elapsedRealtime` of the last check that read the releases; only in memory, so a new process checks. */
    private var readAt: Long? = null

    /** The APK the last check offered, which Download fetches. */
    @Volatile
    private var offered: GitHubApk? = null

    fun start(context: Context) {
        val app = context.applicationContext
        synchronized(lock) {
            if (appContext != null) return
            appContext = app
        }
        scheduleAppUpdateChecks(app)
        launchCheck(notify = true)
    }

    /** The Check updates button: the result shows in the section, so it posts no notification. */
    fun checkAgain() {
        launchCheck(notify = false)
    }

    /** A check for the daily worker, which posts the update notification, unless a recent check read the releases. */
    suspend fun checkInBackground() {
        if (!appRecheckDue(sinceRead())) return
        launchCheck(notify = true).join()
    }

    /**
     * A problem notice: the update a recent check found posts its notification again, or an older check is
     * repeated with the notification. Before [start] nothing runs: its own check posts it.
     */
    fun afterProblem() {
        val context = appContext ?: return
        val known = (status.value as? AppUpdateStatus.Read)?.update?.version
        when (val step = appUpdateAfterProblem(sinceRead(), known)) {
            AppUpdateAfterProblem.Check -> launchCheck(notify = true)
            is AppUpdateAfterProblem.Show -> showAppUpdateNotification(context, step.version.toString())
            AppUpdateAfterProblem.None -> Unit
        }
    }

    fun download(release: AppRelease) {
        val apk = offered?.takeIf { it.id == release.id } ?: return
        val dir = updatesDir() ?: return
        synchronized(lock) {
            if (_download.value is AppUpdateDownload.Running) return
            _download.value = AppUpdateDownload.Running(apk.id, 0f)
        }
        AppLog.i(GITHUB_TAG, "download started")
        scope.launch { _download.value = fetchApk(apk, dir) }
    }

    private fun sinceRead(): Duration? =
        synchronized(lock) { readAt }?.let { (SystemClock.elapsedRealtime() - it).milliseconds }

    /** Starts a check, or joins the one already running, so two never run at once. */
    private fun launchCheck(notify: Boolean): Job {
        synchronized(lock) {
            val running = runningCheck
            if (running != null && running.isActive) {
                if (notify) notifyRunning = true
                AppLog.i(GITHUB_TAG, "check joined")
                return running
            }
            notifyRunning = notify
            readAt = null
            _status.value = AppUpdateStatus.Checking
            AppLog.i(GITHUB_TAG, "check started")
            return scope.launch { _status.value = runCheck() }.also { runningCheck = it }
        }
    }

    private fun runCheck(): AppUpdateStatus {
        val request = Request.Builder()
            .url(GITHUB_LATEST_RELEASE_URL)
            .header("Accept", "application/vnd.github+json")
            .build()
        return try {
            checkHttp.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    read(githubLatestApk(response.body.string()))
                } else {
                    AppLog.w(GITHUB_TAG, "check failed HTTP ${response.code}")
                    Diagnostics.reportHttp(
                        GITHUB_TAG,
                        "releases latest",
                        response.code,
                        response.message,
                        GITHUB_ROUTINE_CODES,
                    )
                    AppUpdateStatus.Failed(UiText.Resource(R.string.github_error, listOf(response.code)))
                }
            }
        } catch (error: IOException) {
            AppLog.w(GITHUB_TAG, "check failed ${error.javaClass.simpleName}")
            AppUpdateStatus.Failed(UiText.Resource(R.string.github_did_not_answer))
        }
    }

    /** A check that answered: the offered APK, the files it leaves, and the update notice. */
    private fun read(apk: GitHubApk?): AppUpdateStatus {
        val installed = AppVersion.parse(BuildConfig.VERSION_NAME) ?: AppVersion.ZERO
        val newer = apk?.takeIf { it.version > installed }
        offered = newer
        val downloaded = updatesDir()?.let { dir ->
            dir.list()?.let { names -> staleUpdateFiles(names.toList(), newer).forEach { File(dir, it).delete() } }
            newer?.let { File(dir, githubApkFileName(it)) }?.takeIf { it.isFile }?.path
        }
        val update = newer?.let { AppRelease(it.id, it.version, downloaded) }
        AppLog.i(GITHUB_TAG, "check finished, latest ${apk?.version}, installed $installed")
        synchronized(lock) { readAt = SystemClock.elapsedRealtime() }
        val context = appContext
        if (context != null) {
            when (val notice = appUpdateNotice(update, notifyRunning)) {
                is AppUpdateNotice.Show -> showAppUpdateNotification(context, notice.version.toString())
                AppUpdateNotice.Clear -> cancelAppUpdateNotification(context)
                AppUpdateNotice.Keep -> Unit
            }
        }
        return AppUpdateStatus.Read(update, apk?.version)
    }

    private fun updatesDir(): File? = appContext?.cacheDir?.resolve("updates")?.apply { mkdirs() }

    /** Downloads into a part file and gives it the APK's name only once its size and digest match. */
    private fun fetchApk(apk: GitHubApk, dir: File): AppUpdateDownload {
        val part = File(dir, githubPartFileName(apk))
        return try {
            sharedHttpClient.newCall(Request.Builder().url(apk.url).build()).execute().use { response ->
                if (!response.isSuccessful) {
                    AppLog.w(GITHUB_TAG, "download failed HTTP ${response.code}")
                    Diagnostics.reportHttp(GITHUB_TAG, "release asset", response.code, response.message, emptySet())
                    val message = UiText.Resource(R.string.github_error, listOf(response.code))
                    return AppUpdateDownload.Failed(apk.id, message)
                }
                val total = apk.size.takeIf { it > 0 } ?: response.body.contentLength()
                val sha256 = copyWithProgress(response.body, part, total) { fraction ->
                    _download.value = AppUpdateDownload.Running(apk.id, fraction)
                }
                val target = File(dir, githubApkFileName(apk))
                if (githubDownloadIntact(apk, part.length(), sha256) && part.renameTo(target)) {
                    AppLog.i(GITHUB_TAG, "download finished")
                    AppUpdateDownload.Ready(apk.id, target.path)
                } else {
                    part.delete()
                    AppLog.w(GITHUB_TAG, "download damaged")
                    AppUpdateDownload.Failed(apk.id, UiText.Resource(R.string.github_download_damaged))
                }
            }
        } catch (error: IOException) {
            part.delete()
            AppLog.w(GITHUB_TAG, "download stopped ${error.javaClass.simpleName}")
            AppUpdateDownload.Failed(apk.id, UiText.Resource(R.string.app_update_download_stopped))
        }
    }
}

/**
 * Writes [body] to [file], passing its progress of [total] bytes to [onProgress], and returns the file's SHA-256 in
 * lowercase hex.
 */
private fun copyWithProgress(body: ResponseBody, file: File, total: Long, onProgress: (Float) -> Unit): String {
    val digest = MessageDigest.getInstance("SHA-256")
    var copied = 0L
    var shownPercent = 0
    body.byteStream().use { input ->
        file.outputStream().use { output ->
            val buffer = ByteArray(BUFFER_BYTES)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                output.write(buffer, 0, read)
                digest.update(buffer, 0, read)
                copied += read
                val fraction = downloadFraction(copied, total)
                // One update per percent: the progress bar shows no finer step.
                val percent = (fraction * 100).toInt()
                if (percent != shownPercent) {
                    shownPercent = percent
                    onProgress(fraction)
                }
            }
        }
    }
    return digest.digest().toLowerHex()
}
