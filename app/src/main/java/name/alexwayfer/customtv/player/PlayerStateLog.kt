package name.alexwayfer.customtv.player

import android.os.Handler
import android.webkit.WebView
import name.alexwayfer.customtv.diagnostics.AppLog

// How long a reload waits for the page to tell its state; a renderer that hangs must not keep the page from reloading.
private const val PLAYER_STATE_TIMEOUT_MS = 1_000L

/**
 * One wait for the page's state: the first of its answer and the timeout gives the log line, the other one nothing.
 * The answer is the JSON the WebView made of the script's value, "null" when it gave none.
 */
internal class PlayerStateWait {
    private var finished = false

    fun finish(answer: String?): String? {
        if (finished) return null
        finished = true
        val state = when (answer) {
            null -> "no answer"
            "null" -> "none"
            else -> answer
        }
        return "Player state before the reload: $state"
    }
}

/**
 * Logs what the video and Twitch's player show before the app reloads a page that did not start, so the report tells
 * a player that loaded nothing from one that was about to play.
 */
internal class PlayerStateLog(
    private val handler: Handler,
    private val webView: WebView,
    private val scripts: TwitchPlayerScripts,
) {
    fun logThen(then: () -> Unit) {
        val wait = PlayerStateWait()
        val finish = { answer: String? ->
            wait.finish(answer)?.let { line ->
                AppLog.i(PLAYER_LOG_TAG, line)
                then()
            }
        }
        val timeout = Runnable { finish(null) }
        handler.postDelayed(timeout, PLAYER_STATE_TIMEOUT_MS)
        webView.evaluateJavascript(scripts.playerState) { answer ->
            handler.removeCallbacks(timeout)
            finish(answer)
        }
    }
}
