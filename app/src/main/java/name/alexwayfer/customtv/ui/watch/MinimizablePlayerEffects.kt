package name.alexwayfer.customtv.ui.watch

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import name.alexwayfer.customtv.player.ShrinkChrome
import name.alexwayfer.customtv.player.chromeOnPlayerShrink
import name.alexwayfer.customtv.player.headerCollapseTimerRuns
import name.alexwayfer.customtv.player.headerCollapsesAfterTimer
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

/** Back, full screen, and the settling of the player into place or into the corner. */
@Composable
internal fun MinimizablePlayerEffects(scene: PlayerScene) {
    val state = scene.state
    val chrome = scene.chrome
    val minimized = scene.minimized
    val inPictureInPicture = scene.inPictureInPicture
    PlayerBackAndFullscreenEffects(scene)
    val currentOnExpanded by rememberUpdatedState(scene.actions.onExpanded)
    LaunchedEffect(chrome, minimized, inPictureInPicture, state.dragging, state.collapsing) {
        chrome.player.unsettle()
        if (state.dragging || state.collapsing) return@LaunchedEffect
        if (inPictureInPicture) {
            state.collapse.snapTo(0f)
        } else {
            val collapseTarget = if (minimized) 1f else 0f
            if (abs(state.collapse.value - collapseTarget) > COLLAPSE_SETTLED_DELTA) {
                state.collapse.animateTo(collapseTarget, PlayerCollapseTween)
            }
        }
        val playerShrunk = minimized || inPictureInPicture
        if (playerShrunk) chrome.player.request(false)
        chrome.player.settle()
        if (!playerShrunk) currentOnExpanded()
    }
    LaunchedEffect(state.collapsing) {
        if (controlsHideAsCollapseStarts(state.collapsing, minimized)) chrome.player.hideNow()
    }
    val compactSettled = scene.compactSettled
    LaunchedEffect(compactSettled) {
        if (!compactSettled) {
            state.swipeDragging = false
            state.swipeX.snapTo(0f)
        }
    }
    HeaderCollapseEffects(chrome, shrunk = minimized || inPictureInPicture)
}

/** Back minimizes the full player or leaves full screen; full screen drops the keyboard and the chat mode note. */
@Composable
private fun PlayerBackAndFullscreenEffects(scene: PlayerScene) {
    val state = scene.state
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(scene.ui.chatModeNotice) {
        if (scene.ui.chatModeNotice == null) return@LaunchedEffect
        delay(FullscreenChatModeNoticeDuration)
        scene.ui.chatModeNotice = null
    }
    // A full player minimizes on Back. A mini player leaves Back to the app: sections walk back,
    // and Home sends the player to picture-in-picture.
    val onBack = scene.actions.onBack
    BackHandler(enabled = !state.collapsing && !scene.inPictureInPicture && (onBack != null || !scene.minimized)) {
        if (onBack != null) onBack() else state.minimize(after = scene.actions.onMinimize)
    }
    val fullscreenActive = scene.fullscreenActive
    BackHandler(enabled = fullscreenActive) { scene.fullscreen.exit() }
    LaunchedEffect(fullscreenActive) {
        if (!fullscreenActive) scene.ui.chatModeNotice = null
        if (fullscreenActive) {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        }
    }
}

/**
 * The chrome shows again as the player leaves the corner or picture-in-picture, and the header collapses a while
 * after it shows.
 */
@Composable
private fun HeaderCollapseEffects(chrome: PlayerChromeVisibility, shrunk: Boolean) {
    var wasShrunk by remember { mutableStateOf(shrunk) }
    LaunchedEffect(shrunk) {
        when (chromeOnPlayerShrink(shrunk, wasShrunk)) {
            ShrinkChrome.Hide -> chrome.minimizeControl = false
            ShrinkChrome.Show -> chrome.show(true)
            ShrinkChrome.Keep -> Unit
        }
        wasShrunk = shrunk
    }
    LaunchedEffect(
        chrome,
        chrome.headerExpanded,
        chrome.minimizeControl,
        chrome.headerRevealCount,
        chrome.headerHeld,
        chrome.videoLoading,
        chrome.playerOutOfSight,
    ) {
        if (
            !headerCollapseTimerRuns(
                headerExpanded = chrome.headerExpanded,
                minimizeControl = chrome.minimizeControl,
                held = chrome.headerHeld,
                videoLoading = chrome.videoLoading,
                playerOutOfSight = chrome.playerOutOfSight,
            )
        ) {
            return@LaunchedEffect
        }
        delay(HEADER_COLLAPSE_AFTER)
        snapshotFlow { headerCollapsesAfterTimer(chrome.playerControls, chrome.playerOutOfSight) }.first { it }
        chrome.show(false)
    }
}

private val HEADER_COLLAPSE_AFTER = 5.seconds

/** A collapse this close to its target counts as there. */
private const val COLLAPSE_SETTLED_DELTA = 0.001f
