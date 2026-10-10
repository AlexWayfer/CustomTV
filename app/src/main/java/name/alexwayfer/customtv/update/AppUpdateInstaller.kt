package name.alexwayfer.customtv.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.provider.Settings
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.BuildConfig
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.ui.UiText

internal const val APP_UPDATE_TAG = "AppUpdate"

/** Installs a downloaded release over this app; Android asks the user to confirm. */
internal object AppUpdateInstaller {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _busy = MutableStateFlow(false)
    private val _error = MutableStateFlow<UiText?>(null)

    /** True while the file is copied into the install session. */
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    /** Why the last install failed, as Android words it; null after a success or a cancel. */
    val error: StateFlow<UiText?> = _error.asStateFlow()

    /** Android lets an app install others only after the user allows it once in the system settings. */
    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    /** The system settings page where the user allows this app to install updates. */
    fun installPermissionIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())

    fun install(context: Context, path: String, version: String) {
        val app = context.applicationContext
        if (_busy.value) return
        AppLog.i(APP_UPDATE_TAG, "install ${BuildConfig.VERSION_NAME} -> $version")
        _busy.value = true
        _error.value = null
        scope.launch {
            try {
                commit(app, File(path))
                AppLog.i(APP_UPDATE_TAG, "install session committed")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                AppLog.w(APP_UPDATE_TAG, "install session failed ${error.javaClass.simpleName}")
                _error.value = UiText.Raw(error.message ?: error.javaClass.simpleName)
            } finally {
                _busy.value = false
            }
        }
    }

    private fun commit(app: Context, file: File) {
        val installer = app.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(app.packageName)
            setSize(file.length())
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("update.apk", 0, file.length()).use { output ->
                file.inputStream().use { it.copyTo(output) }
                session.fsync(output)
            }
            // Android fills the status extras in, so the intent stays mutable; it names our receiver.
            val status = PendingIntent.getBroadcast(
                app,
                sessionId,
                Intent(app, AppUpdateInstallReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(status.intentSender)
        }
    }

    fun onStatus(status: Int, message: String?) {
        AppLog.i(APP_UPDATE_TAG, "install status $status")
        _error.value = appInstallError(status, message)
    }
}

/** The error to show for an install result: none after a success or when the user canceled. */
internal fun appInstallError(status: Int, message: String?): UiText? = when (status) {
    PackageInstaller.STATUS_SUCCESS,
    PackageInstaller.STATUS_FAILURE_ABORTED,
    -> null
    else -> message?.takeIf { it.isNotBlank() }?.let { UiText.Raw(it) }
        ?: UiText.Resource(R.string.app_update_install_failed, listOf(status))
}

class AppUpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            val confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java) ?: return
            context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        AppUpdateInstaller.onStatus(status, intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE))
    }
}
