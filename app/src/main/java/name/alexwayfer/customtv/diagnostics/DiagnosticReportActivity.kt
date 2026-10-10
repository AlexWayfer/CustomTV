package name.alexwayfer.customtv.diagnostics

import android.app.Activity
import android.app.NotificationManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.content.FileProvider
import name.alexwayfer.customtv.R
import java.io.File
import java.io.IOException

private const val REPORT_ADDRESS = "customtv@alexwayfer.name"
private const val EXTRA_WITH_APP_LOG = "with_app_log"

/** Opens the share sheet with the report; [withAppLog] adds the whole log file. */
internal fun diagnosticReportIntent(context: Context, withAppLog: Boolean): Intent =
    Intent(context, DiagnosticReportActivity::class.java).putExtra(EXTRA_WITH_APP_LOG, withAppLog)

/** Opens the system share sheet with the report attached, then closes without showing anything. */
class DiagnosticReportActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getSystemService(NotificationManager::class.java)?.cancel(DIAGNOSTIC_NOTIFICATION_ID)
        val folder = File(cacheDir, "reports")
        val file = File(folder, diagnosticReportFileName(System.currentTimeMillis()))
        try {
            folder.mkdirs()
            // Each report has its own name now; the ones shared before are no longer needed.
            folder.listFiles()?.forEach { it.delete() }
            file.writeText(Diagnostics.report(this, intent.getBooleanExtra(EXTRA_WITH_APP_LOG, false)))
        } catch (error: IOException) {
            Log.w("Diagnostics", "report write failed ${error.javaClass.simpleName}")
            finish()
            return
        }
        val uri = FileProvider.getUriForFile(this, "$packageName.diagnostics", file)
        val summary = Diagnostics.shareText(this)
        Diagnostics.markSent(this)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(REPORT_ADDRESS))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.diagnostics_subject))
            putExtra(Intent.EXTRA_TEXT, summary)
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(send, getString(R.string.diagnostics_send)))
        finish()
    }
}
