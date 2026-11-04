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
        if (event !in DIAGNOSTIC_EVENTS || !position.isFinite() || position < 0.0) return
        if (buffered.length > MAX_BUFFER_LOG_LENGTH || !BUFFER_LOG.matches(buffered)) return
        if (quality.length > MAX_QUALITY_LOG_LENGTH || !QUALITY_LOG.matches(quality)) return
        handler.post {
            if (!isReleased() && isCurrentPage()) {
                AppLog.i(PLAYER_LOG_TAG, "$event at $position s, paused $paused, quality $quality, buffered $buffered")
            }
        }
    }

    private companion object {
        const val MAX_BUFFER_LOG_LENGTH = 512
        const val MAX_QUALITY_LOG_LENGTH = 40
        val BUFFER_LOG = Regex("[0-9., \\[\\]-]*")
        val QUALITY_LOG = Regex("[a-zA-Z0-9_]*")
        val DIAGNOSTIC_EVENTS = setOf(
            "bound", "pause-request", "play-request", "restart-request", "restart-play",
            "playing", "pause", "seeking", "seeked", "emptied", "ended", "error",
            "timeline-reset-before", "timeline-reset-after", "buffer-split-before", "buffer-split-after",
            "buffer-joined-before", "buffer-joined-after", "quality-before", "quality-after",
        )
    }
}
