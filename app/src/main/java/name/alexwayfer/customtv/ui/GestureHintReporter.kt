package name.alexwayfer.customtv.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.GestureHint
import name.alexwayfer.customtv.data.GestureHintsStore
import name.alexwayfer.customtv.diagnostics.AppLog
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Hears where the user used a gesture or did the same thing the slower way, and hands out a tip to the app's
 * snackbar once it is due, in full screen too.
 */
internal class GestureHintReporter(private val store: GestureHintsStore, private val scope: CoroutineScope) {
    private val gesturesUsed = mutableSetOf<GestureHint>()
    private val snackbarHints = Channel<GestureHint>(Channel.BUFFERED)
    private var holds by mutableIntStateOf(0)

    fun gestureUsed(hint: GestureHint) {
        if (!gesturesUsed.add(hint)) return
        AppLog.i(GESTURE_HINTS_LOG_TAG, "${hint.key}: gesture used")
        scope.launch { store.markGestureUsed(hint) }
    }

    fun slowPathUsed(hint: GestureHint) {
        if (hint in gesturesUsed) {
            AppLog.i(GESTURE_HINTS_LOG_TAG, "${hint.key}: slow use skipped, the gesture was used")
            return
        }
        scope.launch {
            val step = store.recordSlowUse(hint)
            AppLog.i(GESTURE_HINTS_LOG_TAG, "${hint.key}: slow use, ${step.progress}, show=${step.show}")
            if (step.show) snackbarHints.send(hint)
        }
    }

    /** Keeps snackbar tips back while [held], such as during a reply; see [HoldGestureTips]. */
    @Composable
    fun Hold(held: Boolean) {
        DisposableEffect(held) {
            if (held) holds++
            onDispose { if (held) holds-- }
        }
    }

    /** Shows the tips in [hostState], once nothing holds them and the keyboard is down. */
    @Composable
    fun Snackbars(hostState: SnackbarHostState) {
        val resources = LocalResources.current
        val density = LocalDensity.current
        val ime = WindowInsets.ime
        val accessibility = LocalAccessibilityManager.current
        val shownFor = accessibility?.calculateRecommendedTimeoutMillis(
            GestureTipDuration.inWholeMilliseconds,
            containsIcons = true,
            containsText = true,
        )?.milliseconds ?: GestureTipDuration
        LaunchedEffect(hostState, resources, density, ime, shownFor) {
            snackbarHints.receiveAsFlow().collect { hint ->
                // The tap that earned the tip may start a hold, such as a reply; it takes effect in the next frame's
                // composition, which the second frame follows.
                withFrameNanos {}
                withFrameNanos {}
                snapshotFlow { holds == 0 && ime.getBottom(density) == 0 }.first { it }
                AppLog.i(GESTURE_HINTS_LOG_TAG, "${hint.key}: snackbar shown")
                val visuals = GestureTipSnackbarVisuals(resources.getString(hint.text))
                coroutineScope {
                    // Counts from when the tip is on screen, not while another snackbar still holds the place.
                    val timer = launch {
                        snapshotFlow { hostState.currentSnackbarData }.first { it?.visuals === visuals }
                        delay(shownFor)
                        hostState.currentSnackbarData?.takeIf { it.visuals === visuals }?.dismiss()
                    }
                    hostState.showSnackbar(visuals)
                    timer.cancel()
                }
            }
        }
    }
}

/** How long a gesture tip stays in the snackbar, unless swiped away first. */
internal val GestureTipDuration = 5.seconds

/** A gesture tip in the app's snackbar, drawn with an info icon; it leaves after [GestureTipDuration]. */
internal class GestureTipSnackbarVisuals(override val message: String) : SnackbarVisuals {
    override val actionLabel: String? = null
    override val withDismissAction = false
    override val duration = SnackbarDuration.Indefinite
}

@Composable
internal fun rememberGestureHintReporter(): GestureHintReporter {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context, scope) { GestureHintReporter(GestureHintsStore(context), scope) }
}

/** Keeps gesture tips out of the snackbar while [held], as while the user writes a reply. */
@Composable
internal fun HoldGestureTips(held: Boolean) {
    LocalGestureHints.current?.Hold(held)
}

private const val GESTURE_HINTS_LOG_TAG = "GestureHints"

/** The app's [GestureHintReporter]; null outside the app, such as in a preview. */
internal val LocalGestureHints = staticCompositionLocalOf<GestureHintReporter?> { null }

@get:StringRes
internal val GestureHint.text: Int
    get() = when (this) {
        GestureHint.Reply -> R.string.gesture_hint_reply
        GestureHint.RemoveRecent -> R.string.gesture_hint_remove_recent
        GestureHint.Minimize -> R.string.gesture_hint_minimize
        GestureHint.EnterFullscreen -> R.string.gesture_hint_enter_fullscreen
        GestureHint.ExitFullscreen -> R.string.gesture_hint_exit_fullscreen
        GestureHint.FullscreenChatSide -> R.string.gesture_hint_fullscreen_chat_side
        GestureHint.FullscreenChatHide -> R.string.gesture_hint_fullscreen_chat_hide
    }
