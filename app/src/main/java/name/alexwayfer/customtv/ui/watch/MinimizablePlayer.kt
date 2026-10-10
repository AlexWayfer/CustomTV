package name.alexwayfer.customtv.ui.watch

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.MainActivity
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.FullscreenChatMode
import name.alexwayfer.customtv.data.FullscreenChatSide
import name.alexwayfer.customtv.data.GestureHint
import name.alexwayfer.customtv.player.PlayerChrome
import name.alexwayfer.customtv.player.TwitchPlaybackTarget
import name.alexwayfer.customtv.player.TwitchPlayer
import name.alexwayfer.customtv.player.chromeChangeSyncsControls
import name.alexwayfer.customtv.player.controlsShowChrome
import name.alexwayfer.customtv.player.minimizeDragHidesKeyboard
import name.alexwayfer.customtv.player.minimizeDragKeepsNavigationBar
import name.alexwayfer.customtv.player.headerCollapseTimerRuns
import name.alexwayfer.customtv.player.headerCollapsesAfterTimer
import name.alexwayfer.customtv.player.playerTapShowsChrome
import name.alexwayfer.customtv.ui.LiftSnackbar
import name.alexwayfer.customtv.ui.LocalGestureHints
import name.alexwayfer.customtv.ui.theme.TwitchBg
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The header and the minimize button appear together with the player's own controls. */
@Stable
internal class PlayerChromeVisibility {
    /** Moves the player's own controls along when [show] changes the chrome. */
    val player = PlayerChrome()

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
        val syncControls = chromeChangeSyncsControls(headerExpanded, minimizeControl, visible)
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
                    direction * (rangePx + miniPaddingPx + 72f),
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
internal class MinimizablePlayerLayout(
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

/**
 * The stream screen's player: tap shows the minimize button with the player controls, a drag
 * down shrinks it into the corner, and the mini player expands or swipes away.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MinimizablePlayer(
    state: MinimizablePlayerState,
    chromeVisibility: PlayerChromeVisibility,
    target: TwitchPlaybackTarget,
    minimized: Boolean,
    onMinimize: () -> Unit,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    inPictureInPicture: Boolean = false,
    appNavigationBarHeightPx: Int = 0,
    onMinimizeDragDismissesKeyboard: () -> Unit = {},
    onExpanded: () -> Unit = {},
    onPositionSeconds: (Double) -> Unit = {},
    onSeekSeconds: (Double) -> Unit = {},
    overlays: @Composable BoxScope.(compactSettled: Boolean) -> Unit = {},
    // The stream info over a full screen player, shown with its controls.
    fullscreenInfo: @Composable () -> Unit = {},
    // Opens the message field from Send chat over a full screen player; null where the chat takes no messages.
    onFullscreenSendChat: (() -> Unit)? = null,
    content: @Composable ColumnScope.(MinimizablePlayerLayout) -> Unit,
) {
    val activity = LocalActivity.current as MainActivity
    KeepScreenAwake(activity)
    val compactSettled = minimized && !inPictureInPicture
    val progress = when {
        inPictureInPicture -> 0f
        state.dragging -> state.dragProgress
        else -> state.collapse.value
    }
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val screenHeightPx = LocalWindowInfo.current.containerSize.height.toFloat()
    val fullscreen = rememberPlayerFullscreen(minimized, inPictureInPicture)
    val fullscreenActive = fullscreen.active
    var measuredControlBarPx by remember { mutableStateOf<Int?>(null) }
    // A recording's buttons clear its time labels by the room around their icon, and glide as the measured top moves,
    // such as when the labels load. A stream's rest on its control bar as measured.
    val recordingControlBarPx by animateIntAsState(
        targetValue = with(density) {
            (measuredControlBarPx ?: TwitchControlBarHeight.roundToPx()) + RecordingButtonsGap.roundToPx()
        },
        label = "recording control bar",
    )
    val controlBarHeightPx = if (target is TwitchPlaybackTarget.Video) recordingControlBarPx else measuredControlBarPx
    var composerPanelPx by remember { mutableIntStateOf(0) }
    var chatComposerOpen by remember { mutableStateOf(false) }
    val compactChatHeight = remember { FullscreenCompactChatHeight() }
    // Twitch's settings menu covers the bottom corner while it is open.
    var twitchSettingsOpen by remember { mutableStateOf(false) }
    // An overlaid chat and its buttons rise above the control bar while either Twitch's controls or the player's own
    // buttons show, at the pace of those buttons, so neither jumps while Twitch's bar comes and goes as the stream
    // loads. Read when laid out, so the chat resizes without recomposing.
    val controlsUp = chromeVisibility.playerControls || chromeVisibility.minimizeControl
    val controlBarShown = animateFloatAsState(
        targetValue = if (controlsUp) 1f else 0f,
        animationSpec = tween(if (controlsUp) HEADER_FADE_IN_MS else HEADER_FADE_OUT_MS),
        label = "control bar room",
    )
    val chatSwipeScope = rememberCoroutineScope()
    val chatSwipe = remember { FullscreenChatSwipeState(chatSwipeScope) }
    var chatModeNotice by remember { mutableStateOf<FullscreenChatModeNotice?>(null) }
    LaunchedEffect(chatModeNotice) {
        if (chatModeNotice == null) return@LaunchedEffect
        delay(FullscreenChatModeNoticeDuration)
        chatModeNotice = null
    }
    val gestureHints = LocalGestureHints.current
    val controlBarTop = playerControlBarTop(controlBarHeightPx)
    // A snackbar, such as a gesture tip, rises over the control bar while it shows, at the pace of the chat beside it.
    LiftSnackbar(active = fullscreenActive) {
        with(density) { (controlBarTop.toPx() * controlBarShown.value).roundToInt() }
    }

    fun expandPlayer() {
        chromeVisibility.show(true)
        state.expand(minimized, onExpand)
    }

    // A full player minimizes on Back. A mini player leaves Back to the app: sections walk back,
    // and Home sends the player to picture-in-picture.
    BackHandler(enabled = !state.collapsing && !inPictureInPicture && (onBack != null || !minimized)) {
        if (onBack != null) onBack() else state.minimize(after = onMinimize)
    }
    BackHandler(enabled = fullscreenActive) { fullscreen.exit() }
    LaunchedEffect(fullscreenActive) {
        if (!fullscreenActive) chatModeNotice = null
        if (fullscreenActive) {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        }
    }
    val currentOnExpanded by rememberUpdatedState(onExpanded)
    LaunchedEffect(chromeVisibility, minimized, inPictureInPicture, state.dragging, state.collapsing) {
        chromeVisibility.player.unsettle()
        if (state.dragging || state.collapsing) return@LaunchedEffect
        if (inPictureInPicture) {
            state.collapse.snapTo(0f)
        } else {
            val target = if (minimized) 1f else 0f
            if (abs(state.collapse.value - target) > 0.001f) {
                state.collapse.animateTo(target, PlayerCollapseTween)
            }
        }
        val playerShrunk = minimized || inPictureInPicture
        if (playerShrunk) chromeVisibility.player.request(false)
        chromeVisibility.player.settle()
        if (!playerShrunk) currentOnExpanded()
    }
    LaunchedEffect(state.collapsing) {
        if (controlsHideAsCollapseStarts(state.collapsing, minimized)) chromeVisibility.player.hideNow()
    }
    LaunchedEffect(compactSettled) {
        if (!compactSettled) {
            state.swipeDragging = false
            state.swipeX.snapTo(0f)
        }
    }
    var wasMinimized by remember { mutableStateOf(minimized) }
    LaunchedEffect(minimized) {
        if (minimized) {
            chromeVisibility.minimizeControl = false
        } else if (wasMinimized) {
            chromeVisibility.show(true)
        }
        wasMinimized = minimized
    }
    LaunchedEffect(
        chromeVisibility,
        chromeVisibility.headerExpanded,
        chromeVisibility.minimizeControl,
        chromeVisibility.headerRevealCount,
        chromeVisibility.headerHeld,
        chromeVisibility.videoLoading,
    ) {
        if (
            !headerCollapseTimerRuns(
                headerExpanded = chromeVisibility.headerExpanded,
                minimizeControl = chromeVisibility.minimizeControl,
                held = chromeVisibility.headerHeld,
                videoLoading = chromeVisibility.videoLoading,
            )
        ) {
            return@LaunchedEffect
        }
        delay(HEADER_COLLAPSE_AFTER)
        snapshotFlow { headerCollapsesAfterTimer(chromeVisibility.playerControls) }.first { it }
        chromeVisibility.show(false)
    }

    val statusBarTopPx = WindowInsets.statusBars.getTop(density)
    val layoutDirection = LocalLayoutDirection.current
    val cutout = WindowInsets.displayCutout
    val fullscreenSidePx =
        fullscreenCutoutSidePx(cutout.getLeft(density, layoutDirection), cutout.getRight(density, layoutDirection))
    val fullscreenSide = with(density) { fullscreenSidePx.toDp() }
    val ime = WindowInsets.ime
    val imeTarget = WindowInsets.imeAnimationTarget
    val keyboardMemory = remember { FullscreenKeyboardMemory() }
    val systemNavBarPx = WindowInsets.navigationBars.getBottom(density)
    val navBarBottomPx = if (appNavigationBarHeightPx > 0) appNavigationBarHeightPx else systemNavBarPx
    val miniWidthPx = with(density) { MiniPlayerWidth.toPx() }
    val miniPaddingPx = with(density) { MiniPlayerPadding.toPx() }
    val miniShape = RoundedCornerShape(MiniPlayerCorner)
    val swipeOffsetX = if (state.swipeDragging) state.swipeDragX else state.swipeX.value
    val expandingChrome = state.dragFromMini ||
        (!state.dragging && !state.collapsing && state.collapse.targetValue == 0f && progress > 0.001f)
    val visualState = miniPlayerVisualState(
        progress = progress,
        swipeOffsetX = swipeOffsetX,
        playerWidthPx = miniWidthPx,
        expanding = expandingChrome,
    )
    val playerCorner = RoundedCornerShape(lerp(0.dp, MiniPlayerCorner, progress))

    BoxWithConstraints(
        modifier = modifier
            .then(
                if (compactSettled) {
                    Modifier
                        .graphicsLayer {
                            translationX = swipeOffsetX
                            alpha = visualState.swipeAlpha
                        }
                        .width(MiniPlayerWidth)
                        .aspectRatio(16f / 9f)
                        .shadow(8.dp, miniShape)
                        .clip(miniShape)
                } else {
                    Modifier
                        .fillMaxSize()
                        .blockClicks()
                },
            )
            .background(
                when {
                    inPictureInPicture -> Color.Black
                    compactSettled -> Color.Black
                    else -> TwitchBg.copy(alpha = visualState.chromeAlpha)
                },
            ),
    ) {
        val maxHeightPx = constraints.maxHeight
        val playerBounds = miniPlayerBounds(
            fullWidthPx = constraints.maxWidth.toFloat(),
            fullHeightPx = maxHeightPx.toFloat(),
            miniWidthPx = miniWidthPx,
            miniPaddingPx = miniPaddingPx,
            statusBarTopPx = statusBarTopPx.toFloat(),
            navigationBarBottomPx = navBarBottomPx.toFloat(),
            progress = progress,
        )
        // The content stays composed behind the mini player, so expanding does not rebuild the chat.
        val fullSize = remember { FullSizeMemory() }
        if (!compactSettled) fullSize.constraints = constraints
        val contentConstraints = fullSize.constraints
        val fullWidthPx = contentConstraints?.maxWidth
        val fullscreenAreaWidthPx = constraints.maxWidth - 2 * fullscreenSidePx
        val fullscreenChat = if (fullscreenActive) {
            // A hidden chat coming in from a side lies over the video there until it settles.
            val revealSide = chatSwipe.revealSide
            fullscreenChatBounds(
                mode = if (revealSide != null) FullscreenChatMode.Overlay else fullscreen.chatMode,
                side = revealSide ?: fullscreen.chatSide,
                areaWidthPx = fullscreenAreaWidthPx,
            )
        } else {
            null
        }
        // Off in the settings, even while a swipe or the Chat button brings it in.
        val fullscreenChatOff = fullscreenActive && fullscreen.chatMode == FullscreenChatMode.Hidden
        // The full chat stays while it sinks back into the compact one; read when drawn, so it does not recompose.
        val overlayExpandProgress = animateFloatAsState(
            targetValue = if (fullscreen.overlayChatExpanded) 1f else 0f,
            animationSpec = tween(COMPACT_CHAT_MOTION_MS),
            label = "overlay chat expand",
        )
        val overlaySinking by remember { derivedStateOf { overlayExpandProgress.value > 0f } }
        val overlayChat = fullscreenOverlayChatHeight(
            overPlayer = fullscreenChat?.overPlayer == true,
            expanded = fullscreen.overlayChatExpanded || overlaySinking,
            composerOpen = chatComposerOpen,
        )
        val revealChatWidthPx =
            fullscreenChatBounds(FullscreenChatMode.Overlay, FullscreenChatSide.Right, fullscreenAreaWidthPx)?.chatWidth ?: 0

        // A hidden chat slides in from its side, as the swipe brings it.
        fun switchChatMode() {
            val next = fullscreenChatNextMode(fullscreen.chatMode)
            if (fullscreen.chatMode == FullscreenChatMode.Hidden) {
                chatSwipe.reveal(fullscreen.chatSide, revealChatWidthPx, fullscreen::showChat)
            } else {
                fullscreen.setChatMode(next)
            }
            chatModeNotice = FullscreenChatModeNotice(next)
            if (next == FullscreenChatMode.Hidden) gestureHints?.slowPathUsed(GestureHint.FullscreenChatHide)
        }

        val chatSwipeTravelPx = fullscreenAreaWidthPx - (fullscreenChat?.chatWidth ?: 0)
        // Read when laid out, so the keyboard moves the video and the chat without recomposing them. The emote
        // picker counts as the keyboard: it takes its place.
        val placementAt: MeasureScope.(Constraints) -> FullscreenPlacement = { area ->
            val (keyboardHeightPx, keyboardProgress) = keyboardMemory.measure(
                imePx = ime.getBottom(this),
                imeTargetPx = imeTarget.getBottom(this),
                pickerPx = composerPanelPx,
            )
            fullscreenPlacement(
                bounds = fullscreenChat,
                areaWidthPx = area.maxWidth,
                areaHeightPx = area.maxHeight,
                keyboardHeightPx = keyboardHeightPx,
                keyboardProgress = keyboardProgress,
                // The bar's usual height until the page reports it, as the player's own buttons take it, so the
                // chat does not sit on the screen's bottom and then jump up to the bar.
                controlBarPx = controlBarHeightPx ?: TwitchControlBarHeight.roundToPx(),
                controlBarShown = controlBarShown.value,
                chatEdgePx = OverlayChatEdgeGap.roundToPx(),
            )
        }
        if (playerContentComposed(inPictureInPicture, compactSettled, contentConstraints != null)) {
            val chatBackgroundAlpha by rememberUpdatedState(
                if (fullscreenChat?.overPlayer == true) FULLSCREEN_OVERLAY_CHAT_ALPHA else visualState.chromeAlpha,
            )
            val playerBottomPx by rememberUpdatedState(playerBounds.y + playerBounds.height)
            val chatOverVideo by rememberUpdatedState(fullscreenChat?.overPlayer == true)
            val containerHeightPx = contentConstraints?.maxHeight ?: maxHeightPx
            val layout = remember(containerHeightPx, fullscreenActive, overlayChat) {
                MinimizablePlayerLayout(
                    chatBackgroundAlpha = { chatBackgroundAlpha },
                    containerHeightPx = containerHeightPx,
                    playerBottomPx = { playerBottomPx },
                    fullscreen = fullscreenActive,
                    onComposerPanelPx = { composerPanelPx = it },
                    chatOverVideo = { chatOverVideo },
                    overlayChat = overlayChat,
                    onOverlayChatExpand = { fullscreen.overlayChatExpanded = true },
                    overlayChatMorph = {
                        overlayChatMorphProgress(
                            overlaid = overlayChat == FullscreenOverlayChatHeight.Full,
                            expandProgress = overlayExpandProgress.value,
                        )
                    },
                    compactChatHeight = compactChatHeight,
                    onComposerOpenChange = { chatComposerOpen = it },
                )
            }
            // Back handlers of hidden content register on a dispatcher that no Back reaches.
            val lifecycleOwner = LocalLifecycleOwner.current
            val hiddenBack = remember(lifecycleOwner) {
                object : OnBackPressedDispatcherOwner {
                    override val onBackPressedDispatcher = OnBackPressedDispatcher()
                    override val lifecycle get() = lifecycleOwner.lifecycle
                }
            }
            val backOwner = LocalOnBackPressedDispatcherOwner.current
            val chatSheetMaxHeightPx = remember(layout) { { chatSheetMaxHeightPx(layout.containerHeightPx, layout.playerBottomPx()) } }
            val contentHidden = compactSettled || (fullscreenActive && fullscreenChat == null)
            CompositionLocalProvider(
                LocalPlayerContentHidden provides contentHidden,
                LocalPlayerContentFullscreen provides fullscreenActive,
                LocalChatSheetMaxHeightPx provides chatSheetMaxHeightPx,
                LocalOnBackPressedDispatcherOwner provides if (contentHidden || backOwner == null) hiddenBack else backOwner,
            ) {
                Column(
                    modifier = Modifier
                        .hiddenAtFullSize(contentHidden, contentConstraints ?: constraints)
                        .fillMaxSize()
                        .then(
                            if (fullscreenChat != null) {
                                // Above the player box, whose black background fills the screen.
                                Modifier
                                    .zIndex(1f)
                                    .fullscreenDragMotion(fullscreen, screenHeightPx)
                                    .padding(horizontal = fullscreenSide)
                                    .windowInsetsPadding(cutout.only(WindowInsetsSides.Vertical))
                                    .fullscreenChat(
                                        compact = overlayChat == FullscreenOverlayChatHeight.Compact,
                                        compactHeight = compactChatHeight,
                                        // Only the overlaid chat rises and sinks; a column or a typing chat stays
                                        // whole.
                                        expandProgress = {
                                            if (overlayChat == FullscreenOverlayChatHeight.Full) {
                                                overlayExpandProgress.value
                                            } else {
                                                1f
                                            }
                                        },
                                        placement = placementAt,
                                    )
                                    .fullscreenChatSwipe(
                                        swipe = chatSwipe,
                                        chatOnLeft = fullscreenChat.chatOnLeft,
                                        chatWidthPx = fullscreenChat.chatWidth,
                                        travelPx = chatSwipeTravelPx,
                                        onHide = {
                                            gestureHints?.gestureUsed(GestureHint.FullscreenChatHide)
                                            fullscreen.hideChat()
                                        },
                                        onMoveAcross = {
                                            gestureHints?.gestureUsed(GestureHint.FullscreenChatSide)
                                            fullscreen.moveChatAcross()
                                        },
                                    )
                                    // The compact chat rounds each message instead.
                                    .then(
                                        if (overlayChat == FullscreenOverlayChatHeight.Compact) {
                                            Modifier
                                        } else {
                                            Modifier.clip(fullscreenChatShape(fullscreenChat))
                                        },
                                    )
                            } else {
                                Modifier
                            },
                        )
                        .blockClicks()
                        .then(
                            if (visualState.chromeAlpha < 1f) {
                                Modifier.graphicsLayer { alpha = visualState.chromeAlpha }
                            } else {
                                Modifier
                            },
                        )
                        .statusBarsPadding(),
                ) {
                    if (!fullscreenActive) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f),
                        )
                    }
                    content(layout)
                }
            }
        }

        val tapExpands = playerTapExpands(inPictureInPicture, compactSettled, progress)
        val playerBoxModifier = when {
            inPictureInPicture -> Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
            compactSettled -> Modifier.fillMaxSize()
            // The drag moves the video with the chat, both around the middle of the screen.
            fullscreenActive -> Modifier
                .fillMaxSize()
                .background(Color.Black)
                .fullscreenDragMotion(fullscreen, screenHeightPx)
                .padding(horizontal = fullscreenSide)
                .windowInsetsPadding(cutout.only(WindowInsetsSides.Vertical))
                .drawBehind {
                    val fillTop = fullscreenPickerFillTopPx(size.height.roundToInt(), composerPanelPx)
                    if (fillTop != null) {
                        drawRect(TwitchBg, topLeft = Offset(0f, fillTop.toFloat()), size = Size(size.width, size.height - fillTop))
                    }
                }
                .graphicsLayer {
                    translationX = fullscreenPlayerSwipeShiftPx(chatSwipe.offsetX, fullscreenChat, chatSwipeTravelPx)
                }
                .fullscreenVideo(placementAt)
            else -> {
                Modifier
                    .offset { IntOffset(playerBounds.x, playerBounds.y) }
                    .fixedPlayerLayout(playerBounds.width, playerBounds.height)
                    .shadow(lerp(0.dp, 8.dp, progress), playerCorner)
                    .clip(playerCorner)
            }
        }

        Box(
            modifier = playerBoxModifier
                .then(if (fullscreenActive) Modifier else Modifier.fullscreenDragMotion(fullscreen, screenHeightPx))
                .clipToBounds()
                .background(Color.Black),
        ) {
            TwitchPlayer(
                target = target,
                onPositionSeconds = onPositionSeconds,
                onSeekSeconds = onSeekSeconds,
                onControlsVisible = { visible ->
                    chromeVisibility.playerControls = visible
                    if (
                        controlsShowChrome(
                            controlsVisible = visible,
                            headerExpanded = chromeVisibility.headerExpanded,
                            minimizeControl = chromeVisibility.minimizeControl,
                            minimized = minimized,
                        )
                    ) {
                        chromeVisibility.followPlayer(true)
                    }
                },
                onControlBarHeightPx = { measuredControlBarPx = it },
                onSettingsMenuOpen = { twitchSettingsOpen = it },
                onWaitingForVideo = { chromeVisibility.videoLoading = it },
                onTap = { playerControlsVisible, reachedPage ->
                    val showChrome = playerTapShowsChrome(playerControlsVisible)
                    when {
                        inPictureInPicture -> Unit
                        tapExpands -> expandPlayer()
                        // The page toggled its controls on the same tap; the chrome only follows.
                        reachedPage -> chromeVisibility.followPlayer(showChrome)
                        // The page did not get the tap: the controls follow on request even when the chrome
                        // already matches them.
                        else -> {
                            chromeVisibility.followPlayer(showChrome)
                            chromeVisibility.player.request(showChrome)
                        }
                    }
                },
                onDismissDrag = { offsetY ->
                    val fullscreenDrag = !inPictureInPicture && fullscreen.drag(
                        offsetY,
                        canEnter = !minimized && !compactSettled && !state.collapsing,
                    )
                    if (!fullscreenDrag && !state.collapsing && !inPictureInPicture) {
                        state.cancelSettle()
                        if (!state.dragging) {
                            state.dragFromMini = compactSettled || minimized
                            if (minimizeDragHidesKeyboard(state.dragFromMini, offsetY)) {
                                val imeBottomPx = ime.getBottom(density)
                                focusManager.clearFocus(force = true)
                                keyboard?.hide()
                                if (minimizeDragKeepsNavigationBar(state.dragFromMini, offsetY, imeBottomPx)) {
                                    onMinimizeDragDismissesKeyboard()
                                }
                            }
                            if (state.dragFromMini && offsetY < 0f) {
                                onExpand()
                            }
                        }
                        state.dragging = true
                        state.dragProgress = miniPlayerDragProgress(
                            offsetY,
                            screenHeightPx,
                            state.dragFromMini,
                        )
                    }
                },
                onDismissDragEnd = { offsetY, velocityY ->
                    val fullscreenDrag = !inPictureInPicture &&
                        fullscreen.dragEnd(offsetY, velocityY, screenHeightPx) { entering ->
                            gestureHints?.gestureUsed(
                                if (entering) GestureHint.EnterFullscreen else GestureHint.ExitFullscreen,
                            )
                        }
                    if (!inPictureInPicture && !fullscreenDrag) {
                        val fromMini = state.dragFromMini
                        state.settleDrag(offsetY, velocityY, screenHeightPx) {
                            if (!fromMini) gestureHints?.gestureUsed(GestureHint.Minimize)
                            onMinimize()
                        }
                    }
                },
                onHorizontalDrag = { offsetX ->
                    if (fullscreenChatOff) {
                        chatSwipe.revealDrag(offsetX, revealChatWidthPx)
                    } else if (compactSettled && !state.collapsing && !inPictureInPicture) {
                        state.cancelSettle()
                        state.swipeDragging = true
                        state.swipeDragX = offsetX
                    }
                },
                onHorizontalDragEnd = { offsetX, velocityX ->
                    if (fullscreenActive && chatSwipe.revealSide != null) {
                        chatSwipe.revealRelease(velocityX, revealChatWidthPx) { side ->
                            gestureHints?.gestureUsed(GestureHint.FullscreenChatHide)
                            fullscreen.showChat(side)
                        }
                    } else if (!inPictureInPicture && compactSettled && !state.collapsing) {
                        state.settleSwipe(
                            offsetX = offsetX,
                            velocityX = velocityX,
                            rangePx = miniWidthPx.coerceAtLeast(1f),
                            miniPaddingPx = miniPaddingPx,
                            onClose = onClose,
                        )
                    }
                },
                enableMinimizeSwipe = !inPictureInPicture && !compactSettled,
                enableExpandSwipe = !inPictureInPicture && (
                    (compactSettled && !state.swipeDragging) ||
                        (fullscreen.available && !minimized && !fullscreenActive && !state.collapsing)
                    ),
                enableHorizontalDismiss = !inPictureInPicture && (compactSettled || fullscreenChatOff),
                tapExpands = tapExpands,
                inPictureInPicture = inPictureInPicture,
                chrome = chromeVisibility.player,
                // The web page keeps its full size in the mini player too; only the picture shrinks.
                // A resize would show the old frame unscaled until the page draws at the new size.
                modifier = if (inPictureInPicture || fullscreenActive || fullWidthPx == null) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.playerScaledFromFullWidth(fullWidthPx)
                },
            )
            if (!inPictureInPicture && !compactSettled && progress < 0.2f) {
                // Fades with the stream info panel's details, which show and hide with the same controls.
                AnimatedVisibility(
                    visible = chromeVisibility.minimizeControl && !fullscreenActive,
                    modifier = Modifier.align(Alignment.TopStart),
                    enter = HeaderFadeIn,
                    exit = HeaderFadeOut,
                ) {
                    IconButton(
                        onClick = {
                            gestureHints?.slowPathUsed(GestureHint.Minimize)
                            state.minimize(after = onMinimize)
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .graphicsLayer {
                                alpha = (1f - progress / 0.18f).coerceIn(0f, 1f)
                            },
                    ) {
                        MinimizeChevronIcon(
                            modifier = Modifier.size(26.dp),
                            contentDescription = stringResource(R.string.minimize),
                        )
                    }
                }
                // The buttons make room for the chat the settings show, not one a swipe is still bringing in, and
                // slide along when it comes or goes.
                val settledChat = if (fullscreenActive) {
                    fullscreenChatBounds(fullscreen.chatMode, fullscreen.chatSide, fullscreenAreaWidthPx)
                } else {
                    null
                }
                val restPlacement = fullscreenPlacement(
                    bounds = settledChat,
                    areaWidthPx = fullscreenAreaWidthPx,
                    areaHeightPx = maxHeightPx,
                    keyboardHeightPx = 0,
                    keyboardProgress = 0f,
                    controlBarPx = 0,
                )
                val buttonsEndTarget = with(density) { fullscreenPlayerButtonsEndPx(settledChat, restPlacement).toDp() }
                val buttonsEnd by animateDpAsState(targetValue = buttonsEndTarget, label = "player buttons end")
                // The compact chat keeps to its bottom corner, so the stream info and the gear take the whole width.
                val topEnd by animateDpAsState(
                    targetValue = if (overlayChat == FullscreenOverlayChatHeight.Compact) 0.dp else buttonsEndTarget,
                    label = "player top end",
                )
                Box(modifier = Modifier.matchParentSize().padding(end = topEnd)) {
                    PlayerFullscreenInfo(
                        visible = chromeVisibility.minimizeControl && fullscreenActive,
                        content = fullscreenInfo,
                    )
                    PlayerFullscreenChatButton(visible = chromeVisibility.minimizeControl && fullscreenActive)
                    PlayerSleepTimerButton(
                        visible = chromeVisibility.minimizeControl,
                        fullscreen = fullscreenActive,
                    )
                }
                Box(modifier = Modifier.matchParentSize().padding(end = buttonsEnd)) {
                    PlayerFullscreenButton(
                        fullscreen = fullscreen,
                        visible = chromeVisibility.minimizeControl,
                        controlBarHeightPx = controlBarHeightPx,
                        raised = { controlBarShown.value },
                    )
                    PlayerFullscreenChatModeButton(
                        visible = chromeVisibility.minimizeControl && fullscreenActive,
                        mode = fullscreen.chatMode,
                        chatOnLeft = fullscreen.chatSide == FullscreenChatSide.Left,
                        controlBarHeightPx = controlBarHeightPx,
                        raised = { controlBarShown.value },
                        onClick = ::switchChatMode,
                    )
                    PlayerFullscreenChatActions(
                        visible = fullscreenActive && !chatComposerOpen && !twitchSettingsOpen &&
                            fullscreen.chatMode == FullscreenChatMode.Overlay && chatSwipe.revealSide == null,
                        chatOnLeft = fullscreen.chatSide == FullscreenChatSide.Left,
                        expanded = fullscreen.overlayChatExpanded,
                        onExpandedChange = { fullscreen.overlayChatExpanded = it },
                        onSendChat = onFullscreenSendChat,
                        controlBarHeightPx = controlBarHeightPx,
                        raised = { controlBarShown.value },
                    )
                }
            }
            if ((compactSettled || visualState.controlsAlpha > 0f) && !inPictureInPicture) {
                MiniPlayerControls(
                    expandAlpha = if (compactSettled) 1f else visualState.controlsAlpha,
                    onExpand = { expandPlayer() },
                    onClose = onClose,
                )
            }
        }

        // Over the chat too, so the note about a new chat mode stays whole.
        Box(
            modifier = Modifier
                .matchParentSize()
                .zIndex(2f)
                .windowInsetsPadding(cutout),
        ) {
            FullscreenChatModeNoticeBar(notice = chatModeNotice, onNextMode = ::switchChatMode)
        }
        if (!inPictureInPicture) overlays(compactSettled || fullscreenActive)
    }
}

/** The expanded player's constraints, kept while it is minimized; read during composition only. */
private class FullSizeMemory {
    var constraints: Constraints? = null
}

private val MiniPlayerWidth = 220.dp
private val MiniPlayerPadding = 12.dp
private val MiniPlayerCorner = 10.dp
private val PlayerCollapseTween = tween<Float>(
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
private val HEADER_COLLAPSE_AFTER = 5.seconds
