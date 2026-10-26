package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.player.PlayerChrome
import name.alexwayfer.customtv.player.chromeChangeSyncsControls

/**
 * The header and the minimize button appear together with the player's own controls. [outOfSight] tells when the
 * player is hidden for the chat: the header then shows and collapses on its own, apart from the paused player.
 */
@Stable
internal class PlayerChromeVisibility(private val outOfSight: () -> Boolean = { false }) {
    /** Moves the player's own controls along when [show] changes the chrome. */
    val player = PlayerChrome()

    /** The player is hidden for the chat, so the header goes its own way. */
    val playerOutOfSight: Boolean
        get() = outOfSight()

    var headerExpanded by mutableStateOf(true)
    var minimizeControl by mutableStateOf(false)

    /** True while a menu on the stream info is open; the collapse timer waits and restarts after. */
    var headerHeld by mutableStateOf(false)

    /** The player's own controls are on screen; the header does not collapse until they hide. */
    var playerControls by mutableStateOf(false)

    /** The video has not started yet; the collapse timer starts once it does. */
    var videoLoading by mutableStateOf(true)

    /** Restarts the collapse timer even when the header is already expanded. */
    var headerRevealCount by mutableIntStateOf(0)
        private set

    /** Shows or hides the chrome from the app side; the player's controls follow. */
    fun show(visible: Boolean) {
        val syncControls = chromeChangeSyncsControls(headerExpanded, minimizeControl, visible, playerOutOfSight)
        followPlayer(visible)
        if (syncControls) player.request(visible)
    }

    /** Mirrors a tap the player already handled: its controls toggle on their own. */
    fun followPlayer(visible: Boolean) {
        headerExpanded = visible
        minimizeControl = visible
    }

    /** Expands only the header, without the player's controls, when the stream info changes. */
    fun revealHeader() {
        headerExpanded = true
        headerRevealCount++
    }
}

/** Collapse, drag, and swipe state of a player that minimizes into the corner. */
@Stable
internal class MinimizablePlayerState(
    private val scope: CoroutineScope,
    initiallyMinimized: Boolean,
) {
    var collapsing by mutableStateOf(false)
        private set
    internal var dragging by mutableStateOf(false)
    internal var dragFromMini by mutableStateOf(false)
    internal var dragProgress by mutableFloatStateOf(0f)
    internal var swipeDragging by mutableStateOf(false)
    internal var swipeDragX by mutableFloatStateOf(0f)
    internal val collapse = Animatable(if (initiallyMinimized) 1f else 0f)
    internal val swipeX = Animatable(0f)
    private var settleJob: Job? = null

    /** Runs [before] now, animates into the corner, then reports [after]. */
    fun minimize(before: () -> Unit = {}, after: () -> Unit) {
        if (collapsing) return
        settleJob?.cancel()
        before()
        settleJob = scope.launch {
            collapsing = true
            collapse.animateTo(1f, PlayerCollapseTween)
            collapsing = false
            after()
        }
    }

    internal fun cancelSettle() {
        settleJob?.cancel()
    }

    internal fun expand(minimized: Boolean, onExpand: () -> Unit) {
        settleJob?.cancel()
        collapsing = false
        dragging = false
        swipeDragging = false
        if (minimized) {
            onExpand()
        } else {
            settleJob = scope.launch {
                collapse.animateTo(0f, PlayerCollapseTween)
            }
        }
    }

    internal fun settleDrag(offsetY: Float, velocityY: Float, screenHeightPx: Float, onMinimize: () -> Unit) {
        if (collapsing) return
        val fromMini = dragFromMini
        val p = miniPlayerDragProgress(offsetY, screenHeightPx, fromMini)
        val stayMini = shouldStayMinimized(p, velocityY, fromMini)
        settleJob?.cancel()
        settleJob = scope.launch {
            collapse.snapTo(p)
            if (stayMini) {
                collapsing = true
                dragging = false
                collapse.animateTo(1f, PlayerCollapseTween)
                collapsing = false
                onMinimize()
            } else {
                dragging = false
            }
        }
    }

    internal fun settleSwipe(
        offsetX: Float,
        velocityX: Float,
        rangePx: Float,
        miniPaddingPx: Float,
        onClose: () -> Unit,
    ) {
        val shouldClose = shouldCloseMiniPlayer(offsetX, velocityX, rangePx)
        settleJob?.cancel()
        settleJob = scope.launch {
            swipeX.snapTo(offsetX)
            swipeDragging = false
            if (shouldClose) {
                collapsing = true
                val direction = if (offsetX >= 0f) 1f else -1f
                swipeX.animateTo(
                    direction * (rangePx + miniPaddingPx + SWIPE_OUT_OVERSHOOT_PX),
                    PlayerSwipeTween,
                )
                collapsing = false
                onClose()
            } else {
                swipeX.animateTo(0f, PlayerSwipeSnapTween)
            }
        }
    }
}

@Composable
internal fun rememberMinimizablePlayerState(minimized: Boolean): MinimizablePlayerState {
    val scope = rememberCoroutineScope()
    return remember { MinimizablePlayerState(scope, minimized) }
}

/**
 * What the content under the player needs to follow the collapse. The changing values are read
 * when drawn or tapped, so the content does not recompose on each animation frame.
 */
internal data class MinimizablePlayerLayout(
    val chatBackgroundAlpha: () -> Float,
    val containerHeightPx: Int,
    val playerBottomPx: () -> Int,
    /** The content is the chat beside or over a full screen player; the stream info stays hidden. */
    val fullscreen: Boolean,
    /** Reports the emote picker's height, so the full screen chat widens for it as for the keyboard. */
    val onComposerPanelPx: (Int) -> Unit,
    /** The full screen chat lies over the video on a see-through background. */
    val chatOverVideo: () -> Boolean,
    /** How tall a chat laid over the full screen video is. */
    val overlayChat: FullscreenOverlayChatHeight,
    /** Expands the compact overlaid chat to the full height. */
    val onOverlayChatExpand: () -> Unit,
    /** How compact the full overlaid chat looks as it rises or sinks; read when laid out and drawn. */
    val overlayChatMorph: () -> Float,
    /** Where the chat tells the height its rows sink to while it turns into the compact one. */
    val compactChatHeight: FullscreenCompactChatHeight,
    /** Reports whether the keyboard or the emote picker is open, so an overlaid chat takes its full column. */
    val onComposerOpenChange: (Boolean) -> Unit,
)

internal val PlayerCollapseTween = tween<Float>(
    durationMillis = 460,
    easing = FastOutSlowInEasing,
)
private val PlayerSwipeTween = tween<Float>(
    durationMillis = 130,
    easing = FastOutLinearInEasing,
)
private val PlayerSwipeSnapTween = tween<Float>(
    durationMillis = 220,
    easing = FastOutSlowInEasing,
)

/** How much further than its swipe range a closed mini player slides. */
private const val SWIPE_OUT_OVERSHOOT_PX = 72f
