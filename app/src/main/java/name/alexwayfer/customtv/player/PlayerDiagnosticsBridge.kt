package name.alexwayfer.customtv.player

import android.os.Handler
import android.webkit.JavascriptInterface
import name.alexwayfer.customtv.diagnostics.AppLog

/** Diagnostic snapshots do not change playback and are ignored once their page is replaced or released. */
internal class PlayerDiagnosticsBridge(
    private val handler: Handler,
    private val isReleased: () -> Boolean,
    private val isCurrentPage: () -> Boolean,
) {
    /** Only fixed event names and numeric ranges cross into the persistent problem report. */
    @JavascriptInterface
    @Suppress("unused")
    fun onDiagnostic(event: String, position: Double, paused: Boolean, buffered: String, quality: String) {
        if (!isPlayerDiagnosticEvent(event) || !position.isFinite() || position < 0.0) return
        if (buffered.length > MAX_BUFFER_LOG_LENGTH || !BUFFER_LOG.matches(buffered)) return
        if (quality.length > MAX_QUALITY_LOG_LENGTH || !QUALITY_LOG.matches(quality)) return
        handler.post {
            if (!isReleased() && isCurrentPage()) {
                AppLog.i(PLAYER_LOG_TAG, "$event at $position s, paused $paused, quality $quality, buffered $buffered")
            }
        }
    }

    @JavascriptInterface
    @Suppress("unused")
    fun onMediaNetwork(report: String) {
        if (report.length > MAX_MEDIA_NETWORK_LENGTH) return
        val rows = report.split('\n')
        if (rows.size > MAX_MEDIA_NETWORK_ROWS) return
        if (report != "unobserved" && rows.any { !MEDIA_NETWORK_ROW.matches(it) }) return
        handler.post {
            if (!isReleased() && isCurrentPage()) {
                val entries = rows.joinToString(" | ")
                AppLog.i(PLAYER_LOG_TAG, "Media network (kind state HTTP headersMs elapsedMs ageMs): $entries")
            }
        }
    }

    private companion object {
        const val MAX_BUFFER_LOG_LENGTH = 512
        const val MAX_QUALITY_LOG_LENGTH = 40
        const val MAX_MEDIA_NETWORK_LENGTH = 1_024
        const val MAX_MEDIA_NETWORK_ROWS = 16
        val BUFFER_LOG = Regex("[0-9., \\[\\]-]*")
        val QUALITY_LOG = Regex("[a-zA-Z0-9_]*")
        val MEDIA_NETWORK_ROW = Regex(
            "(?:manifest|playlist|segment) (?:open|headers|closed|http|aborted|failed|body-error) " +
                "(?:0|[1-5][0-9]{2}) (?:-1|[0-9]{1,9}) [0-9]{1,9} [0-9]{1,9}",
        )
    }
}
