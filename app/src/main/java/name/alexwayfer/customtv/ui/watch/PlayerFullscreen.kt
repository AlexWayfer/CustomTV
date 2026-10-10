package name.alexwayfer.customtv.ui.watch

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.provider.Settings
import android.view.OrientationEventListener
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChatSettings
import name.alexwayfer.customtv.data.ChatSettingsStore
import name.alexwayfer.customtv.data.FullscreenChatMode
import name.alexwayfer.customtv.data.FullscreenChatSide
import name.alexwayfer.customtv.data.GestureHint
import name.alexwayfer.customtv.ui.LocalGestureHints

/** Whether the player fills the screen in landscape, from the full screen button, a drag up, or turning the device. */
@Stable
internal class PlayerFullscreen(
    private val scope: CoroutineScope,
    private val expanded: State<Boolean>,
    private val displayLandscape: State<Boolean>,
    private val chatSettings: State<ChatSettings?>,
    private val store: ChatSettingsStore,
) {
    var lock by mutableStateOf(FullscreenLock.None)

    // A swipe shows its choice at once, before the store saves it and reads it back.
    private var chatModeChosen by mutableStateOf<FullscreenChatMode?>(null)
    private var chatSideChosen by mutableStateOf<FullscreenChatSide?>(null)

    val chatMode: FullscreenChatMode
        get() = chatModeChosen ?: (chatSettings.value ?: ChatSettings()).fullscreenChatMode

    val chatSide: FullscreenChatSide
        get() = chatSideChosen ?: (chatSettings.value ?: ChatSettings()).fullscreenChatSide

    /**
     * An overlaid chat takes the full height instead of its latest messages in the corner; it returns to the corner
     * on leaving full screen.
     */
    var overlayChatExpanded by mutableStateOf(false)

    fun hideChat() = setChatMode(FullscreenChatMode.Hidden)

    /** Switches the chat to [mode] at once, as the chat mode button does, and saves it. */
    fun setChatMode(mode: FullscreenChatMode) {
        chatModeChosen = mode
        scope.launch { store.setFullscreenChatMode(mode) }
    }

    /** Shows the hidden chat over the video on [side], as a swipe or the Chat button brings it. */
    fun showChat(side: FullscreenChatSide) {
        chatModeChosen = FullscreenChatMode.Overlay
        chatSideChosen = side
        scope.launch {
            store.setFullscreenChatMode(FullscreenChatMode.Overlay)
            store.setFullscreenChatSide(side)
        }
    }

    fun moveChatAcross() {
        val side = if (chatSide == FullscreenChatSide.Left) FullscreenChatSide.Right else FullscreenChatSide.Left
        chatSideChosen = side
        scope.launch { store.setFullscreenChatSide(side) }
    }

    /** Drops the swipe's choices once the stored settings show them. */
    fun settingsRead(settings: ChatSettings?) {
        if (settings?.fullscreenChatMode == chatModeChosen) chatModeChosen = null
        if (settings?.fullscreenChatSide == chatSideChosen) chatSideChosen = null
    }

    /** How far a drag into or out of full screen has moved the player; it returns to 0 when let go. */
    var dragOffsetY by mutableFloatStateOf(0f)
        private set

    /** Whether a drag has moved the player off its place; read in composition, it changes once per drag. */
    val dragMoved: Boolean by derivedStateOf { dragOffsetY != 0f }
    private var dragGesture = FullscreenDrag.None
    private var settleJob: Job? = null

    val available: Boolean
        get() = true

    val active: Boolean
        get() = playerFullscreen(expanded.value, displayLandscape.value)

    fun enter() {
        lock = FullscreenLock.Landscape
    }

    fun exit() {
        lock = lockAfterFullscreenExit(displayLandscape.value)
    }

    /**
     * Follows a vertical drag on the player. True when the drag belongs to full screen: any drag while it is on,
     * or a drag that starts upward when [canEnter]; the minimize gesture handles the rest.
     */
    fun drag(offsetY: Float, canEnter: Boolean): Boolean {
        if (dragGesture == FullscreenDrag.None) {
            dragGesture = fullscreenDragGesture(active, canEnter, offsetY)
            settleJob?.cancel()
        }
        dragOffsetY = fullscreenDragOffset(dragGesture, offsetY)
        return dragGesture.ownsDrag
    }

    /**
     * Ends a drag that [drag] took: past the threshold, enters or leaves full screen and keeps the player where the
     * drag left it until the screen turns ([releaseDrag]); short of it, slides the player back. [onToggle] runs when
     * the drag enters full screen (true) or leaves it (false).
     */
    fun dragEnd(offsetY: Float, velocityY: Float, screenHeightPx: Float, onToggle: (entering: Boolean) -> Unit): Boolean {
        val gesture = dragGesture
        dragGesture = FullscreenDrag.None
        if (!gesture.ownsDrag) return false
        if (fullscreenDragToggles(gesture, offsetY, velocityY, screenHeightPx)) {
            val entering = gesture != FullscreenDrag.Exit
            if (entering) enter() else exit()
            onToggle(entering)
        } else {
            val from = dragOffsetY
            settleJob = scope.launch {
                animate(from, 0f, animationSpec = DragSettleTween) { value, _ -> dragOffsetY = value }
            }
        }
        return true
    }

    /** Puts the player back in place once the layout switched, after a drag that toggled full screen. */
    fun releaseDrag() {
        if (dragGesture == FullscreenDrag.None) dragOffsetY = 0f
    }

    /** How much the player grows or shrinks at the current drag, by the share of the distance to the threshold. */
    fun dragScale(screenHeightPx: Float): Float = fullscreenDragScale(dragOffsetY, screenHeightPx)
}

@Composable
internal fun rememberPlayerFullscreen(minimized: Boolean, inPictureInPicture: Boolean): PlayerFullscreen {
    val activity = checkNotNull(LocalActivity.current)
    val scope = rememberCoroutineScope()
    val expanded = rememberUpdatedState(!minimized && !inPictureInPicture)
    // The configuration, not the window size: on some devices the size Compose reads at a rotation is the old one
    // and stays until the next rotation, which left full screen on in portrait.
    val displayLandscape = rememberUpdatedState(
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE,
    )
    val chatSettings = ChatSettingsStore.restored.collectAsStateWithLifecycle()
    val fullscreen = remember {
        PlayerFullscreen(scope, expanded, displayLandscape, chatSettings, ChatSettingsStore(activity))
    }
    LaunchedEffect(chatSettings.value) {
        fullscreen.settingsRead(chatSettings.value)
    }

    LaunchedEffect(minimized) {
        if (minimized) fullscreen.lock = lockAfterMinimize(fullscreen.lock)
    }
    LaunchedEffect(fullscreen.lock) {
        activity.requestedOrientation = when (fullscreen.lock) {
            FullscreenLock.None -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            FullscreenLock.Landscape -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            FullscreenLock.Portrait -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
    DisposableEffect(fullscreen.lock) {
        val held = fullscreen.lock
        if (held == FullscreenLock.None) return@DisposableEffect onDispose {}
        val listener = object : OrientationEventListener(activity) {
            override fun onOrientationChanged(orientation: Int) {
                val next = lockAfterDeviceTurn(held, deviceTurn(orientation)) {
                    Settings.System.getInt(activity.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
                }
                if (next != held) fullscreen.lock = next
            }
        }
        listener.enable()
        onDispose { listener.disable() }
    }
    val active = fullscreen.active
    LaunchedEffect(active) {
        fullscreen.releaseDrag()
        if (!active) fullscreen.overlayChatExpanded = false
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (active) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    DisposableEffect(activity) {
        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                .show(WindowInsetsCompat.Type.systemBars())
        }
    }
    return fullscreen
}

/**
 * Moves and scales the player with a drag into or out of full screen. The layer exists only while the player is off
 * its place: around the WebView at rest, it makes the renderer draw every video frame into an offscreen layer.
 */
internal fun Modifier.fullscreenDragMotion(fullscreen: PlayerFullscreen, screenHeightPx: Float): Modifier =
    if (!fullscreen.dragMoved) this else graphicsLayer {
        translationY = fullscreen.dragOffsetY
        val scale = fullscreen.dragScale(screenHeightPx)
        scaleX = scale
        scaleY = scale
    }

/**
 * Enters or leaves full screen; shows with the player's controls, in the corner right above the control buttons
 * ([controlBarHeightPx], measured on the page) or above the bar's usual height until the page reports it. In full
 * screen it sits level with the chat buttons' group beside it.
 */
@Composable
internal fun BoxScope.PlayerFullscreenButton(
    fullscreen: PlayerFullscreen,
    visible: Boolean,
    controlBarHeightPx: Int?,
    /** In full screen, how far the controls show, from 0 to 1: the button goes down with the chat buttons as they hide. */
    raised: () -> Float,
) {
    val bottom = if (fullscreen.active) {
        fullscreenPlayerButtonsBottom(controlBarHeightPx)
    } else {
        playerButtonsBottom(controlBarHeightPx)
    }
    val restTop = playerControlBarTop(controlBarHeightPx)
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(bottom = bottom)
            .then(if (fullscreen.active) Modifier.fullscreenControlsDrop(restTop, raised) else Modifier),
        enter = HeaderFadeIn,
        exit = HeaderFadeOut,
    ) {
        val active = fullscreen.active
        val gestureHints = LocalGestureHints.current
        IconButton(
            onClick = {
                if (active) {
                    gestureHints?.slowPathUsed(GestureHint.ExitFullscreen)
                    fullscreen.exit()
                } else {
                    gestureHints?.slowPathUsed(GestureHint.EnterFullscreen)
                    fullscreen.enter()
                }
            },
            modifier = Modifier.size(PlayerButtonSize),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_screen_rotation),
                contentDescription = stringResource(
                    if (active) R.string.player_fullscreen_exit else R.string.player_fullscreen,
                ),
                modifier = Modifier.size(IconSize),
                tint = Color.White,
            )
        }
    }
}

/**
 * How high the player's own buttons in the bottom corners sit: their 44 dp row with the icon resting on the control
 * buttons ([controlBarHeightPx], measured on the page), or on the bar's usual height until the page reports it.
 */
@Composable
internal fun playerButtonsBottom(controlBarHeightPx: Int?): Dp =
    (playerControlBarTop(controlBarHeightPx) - (PlayerButtonSize - IconSize) / 2).coerceAtLeast(0.dp)

/** How high the top of the player's control buttons is, measured on the page or the bar's usual height. */
@Composable
internal fun playerControlBarTop(controlBarHeightPx: Int?): Dp {
    val density = LocalDensity.current
    return controlBarHeightPx?.let { with(density) { it.toDp() } } ?: TwitchControlBarHeight
}

/** The touch target of the player's own buttons. */
internal val PlayerButtonSize = 44.dp

/** The usual height of Twitch's control bar, until the page reports its own. */
internal val TwitchControlBarHeight = 40.dp
private val IconSize = 26.dp

/** How far a recording's buttons stay above its time labels: the room around the icon, so the whole button clears them. */
internal val RecordingButtonsGap = (PlayerButtonSize - IconSize) / 2
private val DragSettleTween = tween<Float>(durationMillis = 220, easing = FastOutSlowInEasing)
