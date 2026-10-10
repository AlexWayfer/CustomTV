package name.alexwayfer.customtv

import android.app.PictureInPictureParams
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Rational
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import name.alexwayfer.customtv.data.STREAM_ALERTS_LOGIN
import name.alexwayfer.customtv.data.STREAM_START_CHANNEL
import name.alexwayfer.customtv.data.StreamAlertsLoginRequest
import name.alexwayfer.customtv.data.StreamStartRequest
import name.alexwayfer.customtv.data.WhisperOpenRequest
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.ui.CustomTvApp
import name.alexwayfer.customtv.ui.PremiumAccessGate
import name.alexwayfer.customtv.ui.theme.CustomTvTheme
import name.alexwayfer.customtv.ui.watch.SleepTimer
import name.alexwayfer.customtv.update.APP_UPDATE_OPEN
import name.alexwayfer.customtv.update.AppUpdateOpenRequest

class MainActivity : ComponentActivity() {
    private val _inPictureInPicture = MutableStateFlow(false)
    val inPictureInPicture: StateFlow<Boolean> = _inPictureInPicture

    var onPipClosed: (() -> Unit)? = null
    var onResumeAfterPipClosed: (() -> Unit)? = null

    private var pipEnabled = false
    private var pipSession = false
    private var pipClosed = false
    private var leavingForExternalLink = false
    private val pipCloseHandler = Handler(Looper.getMainLooper())
    private val confirmPipClose: Runnable = object : Runnable {
        override fun run() {
            if (leavingForExternalLink) return
            val screenOn = getSystemService(PowerManager::class.java)?.isInteractive != false
            val stillStopped = !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            if (!pipSession || !stillStopped || !screenOn || isChangingConfigurations) return
            if (!isInPictureInPictureMode) {
                pipSession = false
                pipClosed = true
                // Closing picture-in-picture stops watching, as closing the app does; the timer goes with it.
                SleepTimer.cancel()
                onPipClosed?.invoke()
            } else if (pipCloseCheckAttempts < MAX_PIP_CLOSE_CHECK_ATTEMPTS) {
                pipCloseCheckAttempts += 1
                pipCloseHandler.postDelayed(this, PIP_CLOSE_RECHECK_DELAY_MS)
            }
        }
    }
    private var pipCloseCheckAttempts = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        StreamStartRequest.offer(intent.getStringExtra(STREAM_START_CHANNEL))
        offerStreamAlertsLogin(intent)
        offerAppUpdateOpen(intent)
        WhisperOpenRequest.offer(this, intent)
        setContent {
            CustomTvTheme {
                PremiumAccessGate {
                    CustomTvApp()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        StreamStartRequest.offer(intent.getStringExtra(STREAM_START_CHANNEL))
        offerStreamAlertsLogin(intent)
        offerAppUpdateOpen(intent)
        WhisperOpenRequest.offer(this, intent)
    }

    /** The extra is dropped once read, so recreating the activity does not start the login again. */
    private fun offerStreamAlertsLogin(intent: Intent) {
        StreamAlertsLoginRequest.offer(intent.getBooleanExtra(STREAM_ALERTS_LOGIN, false))
        intent.removeExtra(STREAM_ALERTS_LOGIN)
    }

    /** The extra is dropped once read, so recreating the activity does not open the settings again. */
    private fun offerAppUpdateOpen(intent: Intent) {
        AppUpdateOpenRequest.offer(intent.getBooleanExtra(APP_UPDATE_OPEN, false))
        intent.removeExtra(APP_UPDATE_OPEN)
    }

    fun setPipEnabled(enabled: Boolean) {
        pipEnabled = enabled && hasPipFeature()
        applyPipParams()
    }

    fun enterPip(): Boolean {
        if (!pipEnabled || isInPictureInPictureMode) return pipEnabled && isInPictureInPictureMode
        return try {
            enterPictureInPictureMode(pipParams())
        } catch (_: IllegalStateException) {
            false
        }
    }

    fun openExternalLink(url: String) {
        leavingForExternalLink = true
        enterPip()
        try {
            startActivity(
                Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        } catch (_: ActivityNotFoundException) {
            leavingForExternalLink = false
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT < 31 && pipEnabled) {
            enterPip()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        _inPictureInPicture.value = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            pipSession = true
        }
    }

    override fun onResume() {
        super.onResume()
        leavingForExternalLink = false
        if (pipClosed) {
            pipClosed = false
            onResumeAfterPipClosed?.invoke()
        }
        if (!isInPictureInPictureMode) {
            pipSession = false
        }
    }

    override fun onStart() {
        pipCloseHandler.removeCallbacks(confirmPipClose)
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
        if (leavingForExternalLink) return
        val screenOn = getSystemService(PowerManager::class.java)?.isInteractive != false
        if (shouldRetryPipOnStop(screenOn)) {
            // Auto-enter skips PiP when the switch goes through a translucent trampoline (X opened from its
            // notification) or the launcher's quick switch. The system still allows entering while stopping.
            val entered = enterPip()
            AppLog.i(PIP_LOG_TAG, "Retry PiP on stop: entered=$entered")
        }
        if (pipSession && screenOn) {
            pipCloseHandler.removeCallbacks(confirmPipClose)
            pipCloseCheckAttempts = 0
            pipCloseHandler.postDelayed(confirmPipClose, PIP_CLOSE_CONFIRM_DELAY_MS)
        }
    }

    override fun onDestroy() {
        pipCloseHandler.removeCallbacks(confirmPipClose)
        super.onDestroy()
    }

    private fun shouldRetryPipOnStop(screenOn: Boolean): Boolean {
        return Build.VERSION.SDK_INT >= 31 && pipEnabled && screenOn && !pipSession &&
            !isInPictureInPictureMode && !isFinishing && !isChangingConfigurations
    }

    private fun applyPipParams() {
        if (!hasPipFeature()) return
        try {
            setPictureInPictureParams(pipParams())
        } catch (_: IllegalStateException) {
            // The system has already dropped the activity, such as when its task is removed during destroy.
        }
    }

    private fun pipParams(): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
        if (Build.VERSION.SDK_INT >= 31) {
            builder.setAutoEnterEnabled(pipEnabled)
            builder.setSeamlessResizeEnabled(true)
            sourceRectHint()?.let { builder.setSourceRectHint(it) }
        }
        return builder.build()
    }

    private fun sourceRectHint(): Rect? {
        val content = findViewById<View>(android.R.id.content) ?: return null
        return Rect().takeIf { content.getGlobalVisibleRect(it) }
    }

    private fun hasPipFeature(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    private companion object {
        const val PIP_CLOSE_CONFIRM_DELAY_MS = 500L
        const val PIP_CLOSE_RECHECK_DELAY_MS = 250L
        const val MAX_PIP_CLOSE_CHECK_ATTEMPTS = 10
        const val PIP_LOG_TAG = "Pip"
    }
}
