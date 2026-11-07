package name.alexwayfer.customtv.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.media.MediaRecorder
import android.os.Handler
import android.webkit.WebView
import androidx.core.content.ContextCompat

// How often the page playing in the background is woken and told to play.
private const val KEEP_ALIVE_MS = 2_000L

/** Keeps the player page playing while the app is in the background, in picture-in-picture, or the screen is off. */
internal class PlayerKeepAlive(
    private val appContext: Context,
    private val handler: Handler,
    private val webView: WebView,
    scripts: TwitchPlayerScripts,
    private val isReleased: () -> Boolean,
    private val onBackgroundChanged: (Boolean) -> Unit,
) {
    private var inBackground = false
    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private val recordingCallback = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: List<AudioRecordingConfiguration>) {
            if (!isReleased()) reportAudioRecording(configs)
        }
    }
    private val keepAlive = object : Runnable {
        override fun run() {
            if (isReleased() || !inBackground) return
            webView.onResume()
            webView.resumeTimers()
            // Conversation mode can start before the microphone opens and outlive its recording callback.
            reportAudioRecording(audioManager.activeRecordingConfigurations)
            if (StreamPlayback.canAutomaticallyPlay) {
                webView.evaluateJavascript(scripts.play(PlayerPlaySource.Background), null)
            }
            handler.postDelayed(this, KEEP_ALIVE_MS)
        }
    }
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                keepAwake(true)
            }
        }
    }

    fun register() {
        audioManager.registerAudioRecordingCallback(recordingCallback, handler)
        reportAudioRecording(audioManager.activeRecordingConfigurations)
        ContextCompat.registerReceiver(
            appContext,
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    fun keepAwake(background: Boolean) {
        onBackgroundChanged(background)
        if (isReleased()) return
        if (background && inBackground) return
        webView.onResume()
        webView.resumeTimers()
        inBackground = background
        StreamPlayback.inferredPauseSuppressed = background
        handler.removeCallbacks(keepAlive)
        if (background) {
            handler.postDelayed(keepAlive, KEEP_ALIVE_MS)
        }
    }

    fun release() {
        inBackground = false
        StreamPlayback.inferredPauseSuppressed = false
        audioManager.unregisterAudioRecordingCallback(recordingCallback)
        try {
            appContext.unregisterReceiver(screenOffReceiver)
        } catch (_: IllegalArgumentException) {
            // The receiver was never registered.
        }
    }

    private fun reportAudioRecording(configs: List<AudioRecordingConfiguration>) {
        // Android exposes the source with no recording permission; the app identity and audio remain private.
        val communicating = when (audioManager.mode) {
            AudioManager.MODE_IN_COMMUNICATION, AudioManager.MODE_IN_CALL -> true
            else -> false
        }
        StreamInterruption.reportAudioRecording(
            recording = configs.isNotEmpty() || communicating,
            recognizingVoice = configs.any { it.clientAudioSource == MediaRecorder.AudioSource.VOICE_RECOGNITION },
        )
    }
}
