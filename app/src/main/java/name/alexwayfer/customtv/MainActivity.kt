package name.alexwayfer.customtv

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import name.alexwayfer.customtv.ui.CustomTvApp
import name.alexwayfer.customtv.ui.PremiumAccessGate
import name.alexwayfer.customtv.ui.theme.CustomTvTheme
import name.alexwayfer.customtv.ui.watch.SleepTimer

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
            val closedForGood = stillStopped && screenOn && !isChangingConfigurations
            if (!pipSession || !closedForGood) return
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
        offerLaunchRequests(this, intent)
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
        offerLaunchRequests(this, intent)
    }

    fun setPipEnabled(enabled: Boolean) {
        pipEnabled = enabled && hasPipFeature()
        applyPipParams(autoEnter = pipEnabled)
    }

    fun enterPip(): Boolean {
        if (!pipEnabled || isInPictureInPictureMode) return pipEnabled && isInPictureInPictureMode
        return try {
            enterPictureInPictureMode(pipParams(autoEnter = pipEnabled))
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && pipEnabled) {
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

    private companion object {
        const val PIP_CLOSE_CONFIRM_DELAY_MS = 500L
        const val PIP_CLOSE_RECHECK_DELAY_MS = 250L
        const val MAX_PIP_CLOSE_CHECK_ATTEMPTS = 10
    }
}
