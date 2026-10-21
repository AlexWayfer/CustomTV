package name.alexwayfer.customtv.diagnostics

import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.theme.CustomTvTheme
import java.io.File
import java.io.IOException

private const val REPORT_ADDRESS = "customtv@alexwayfer.name"
private const val EXTRA_WITH_APP_LOG = "with_app_log"

/** Offers to send the report; [withAppLog] adds the whole log file. */
internal fun diagnosticReportIntent(context: Context, withAppLog: Boolean): Intent =
    Intent(context, DiagnosticReportActivity::class.java).putExtra(EXTRA_WITH_APP_LOG, withAppLog)

/**
 * Warns what personal data the report holds, then hands it to an email app or the system share sheet, over
 * whatever was on screen, and closes. It is `singleTop` in the manifest: a second tap on Send report before
 * this window covers the screen reaches the open one instead of stacking a second dialog under it.
 */
class DiagnosticReportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getSystemService(NotificationManager::class.java)?.cancel(DIAGNOSTIC_NOTIFICATION_ID)
        val send = writeReport()
        if (send == null) {
            finish()
            return
        }
        val items = diagnosticPrivateItems(diagnosticAccounts(this))
        setContent {
            CustomTvTheme {
                DiagnosticSendDialog(
                    items = items,
                    onEmail = { share(send, emailOnly = true) },
                    onOther = { share(send, emailOnly = false) },
                    onDismiss = ::finish,
                )
            }
        }
    }

    /** Writes the report file and returns the intent that sends it, or null when the file cannot be written. */
    private fun writeReport(): Intent? {
        val folder = File(cacheDir, "reports")
        val file = File(folder, diagnosticReportFileName(System.currentTimeMillis()))
        try {
            folder.mkdirs()
            // Each report has its own name now; the ones shared before are no longer needed.
            folder.listFiles()?.forEach { it.delete() }
            file.writeText(Diagnostics.report(this, intent.getBooleanExtra(EXTRA_WITH_APP_LOG, false)))
        } catch (error: IOException) {
            Log.w("Diagnostics", "report write failed ${error.javaClass.simpleName}")
            return null
        }
        val uri = FileProvider.getUriForFile(this, "$packageName.diagnostics", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(REPORT_ADDRESS))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.diagnostics_subject))
            putExtra(Intent.EXTRA_TEXT, Diagnostics.shareText(this@DiagnosticReportActivity))
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * [emailOnly] offers only the apps that answer `mailto:`, and falls back to the share sheet on a phone without
     * one. A `mailto:` selector on the send intent found no app on Android 16, so the email apps are looked up here.
     */
    private fun share(send: Intent, emailOnly: Boolean) {
        DiagnosticJournal.markSent(this)
        val title = getString(R.string.diagnostics_send)
        val chooser = Intent.createChooser(send, title)
        val target = if (emailOnly) reportEmailShare(emailPackages()) else ReportEmailShare.ShareSheet
        try {
            startActivity(
                when (target) {
                    ReportEmailShare.ShareSheet -> chooser
                    is ReportEmailShare.Direct -> Intent(send).setPackage(target.packageName)
                    is ReportEmailShare.Choose -> {
                        val apps = target.packageNames.map { Intent(send).setPackage(it) }
                        Intent.createChooser(apps.first(), title)
                            .putExtra(Intent.EXTRA_INITIAL_INTENTS, apps.drop(1).toTypedArray())
                    }
                },
            )
        } catch (_: ActivityNotFoundException) {
            // An email app that takes `mailto:` but not an attachment.
            startActivity(chooser)
        }
        finish()
    }

    private fun emailPackages(): List<String> {
        val mailto = Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
        val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(mailto, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(mailto, 0)
        }
        return activities.map { it.activityInfo.packageName }
    }
}
