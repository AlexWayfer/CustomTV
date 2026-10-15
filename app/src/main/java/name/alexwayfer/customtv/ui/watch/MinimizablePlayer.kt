package name.alexwayfer.customtv.ui.watch

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import name.alexwayfer.customtv.MainActivity
import name.alexwayfer.customtv.player.TwitchPlaybackTarget
import name.alexwayfer.customtv.ui.LiftSnackbar
import name.alexwayfer.customtv.ui.LocalGestureHints
import name.alexwayfer.customtv.ui.components.VIDEO_ASPECT_RATIO
import name.alexwayfer.customtv.ui.theme.TwitchBg
import kotlin.math.roundToInt

/** What the screen puts around the player: overlays such as sheets, the full screen stream info, and the content. */
private class PlayerSlots(
    val overlays: @Composable BoxScope.(compactSettled: Boolean) -> Unit,
    val fullscreenInfo: @Composable () -> Unit,
    val content: @Composable ColumnScope.(MinimizablePlayerLayout) -> Unit,
)

/**
 * The stream screen's player: tap shows the minimize button with the player controls, a drag
 * down shrinks it into the corner, and the mini player expands or swipes away.
 */
@Composable
internal fun MinimizablePlayer(
    state: MinimizablePlayerState,
    chromeVisibility: PlayerChromeVisibility,
    target: TwitchPlaybackTarget,
    minimized: Boolean,
    actions: PlayerActions,
    modifier: Modifier = Modifier,
    inPictureInPicture: Boolean = false,
    appNavigationBarHeightPx: Int = 0,
    overlays: @Composable BoxScope.(compactSettled: Boolean) -> Unit = {},
    // The stream info over a full screen player, shown with its controls.
    fullscreenInfo: @Composable () -> Unit = {},
    content: @Composable ColumnScope.(MinimizablePlayerLayout) -> Unit,
) {
    val activity = LocalActivity.current as MainActivity
    KeepScreenAwake(activity)
    val ui = remember { PlayerSceneState() }
    val chatSwipeScope = rememberCoroutineScope()
    val chatSwipe = remember { FullscreenChatSwipeState(chatSwipeScope) }
    val scene = PlayerScene(
        state = state,
        chrome = chromeVisibility,
        ui = ui,
        fullscreen = rememberPlayerFullscreen(minimized, inPictureInPicture),
        chatSwipe = chatSwipe,
        target = target,
        actions = actions,
        minimized = minimized,
        inPictureInPicture = inPictureInPicture,
        progress = when {
            inPictureInPicture -> 0f
            state.dragging -> state.dragProgress
            else -> state.collapse.value
        },
        screenHeightPx = LocalWindowInfo.current.containerSize.height.toFloat(),
        controlBarHeightPx = playerControlBarHeightPx(target, ui.measuredControlBarPx),
        controlBarShown = animateControlBarShown(chromeVisibility),
        gestureHints = LocalGestureHints.current,
    )
    MinimizablePlayerEffects(scene)
    val density = LocalDensity.current
    val controlBarTop = playerControlBarTop(scene.controlBarHeightPx)
    // A snackbar, such as a gesture tip, rises over the control bar while it shows, at the pace of the chat beside it.
    LiftSnackbar(active = scene.fullscreenActive) {
        with(density) { (controlBarTop.toPx() * scene.controlBarShown.value).roundToInt() }
    }
    val swipeOffsetX = if (state.swipeDragging) state.swipeDragX else state.swipeX.value
    val visualState = playerVisualState(scene, with(density) { MiniPlayerWidth.toPx() }, swipeOffsetX)
    BoxWithConstraints(modifier = modifier.playerFrame(scene, visualState, swipeOffsetX, Modifier.blockClicks())) {
        PlayerSceneBody(scene, visualState, appNavigationBarHeightPx, PlayerSlots(overlays, fullscreenInfo, content))
    }
}

private fun playerVisualState(scene: PlayerScene, miniWidthPx: Float, swipeOffsetX: Float): MiniPlayerVisualState {
    val state = scene.state
    val expandingChrome = state.dragFromMini ||
        (!state.dragging && !state.collapsing && state.collapse.targetValue == 0f && scene.progress > 0.001f)
    return miniPlayerVisualState(
        progress = scene.progress,
        swipeOffsetX = swipeOffsetX,
        playerWidthPx = miniWidthPx,
        expanding = expandingChrome,
    )
}

/**
 * The mini player's card in the corner, or the whole screen with its ground fading as the player shrinks; there
 * [clickBlocker] keeps taps from the screen under it.
 */
private fun Modifier.playerFrame(
    scene: PlayerScene,
    visualState: MiniPlayerVisualState,
    swipeOffsetX: Float,
    clickBlocker: Modifier,
): Modifier {
    val miniShape = RoundedCornerShape(MiniPlayerCorner)
    return this
        .then(
            if (scene.compactSettled) {
                Modifier
                    .graphicsLayer {
                        translationX = swipeOffsetX
                        alpha = visualState.swipeAlpha
                    }
                    .width(MiniPlayerWidth)
                    .aspectRatio(VIDEO_ASPECT_RATIO)
                    .shadow(8.dp, miniShape)
                    .clip(miniShape)
            } else {
                Modifier
                    .fillMaxSize()
                    .then(clickBlocker)
            },
        )
        .background(
            when {
                scene.inPictureInPicture -> Color.Black
                scene.compactSettled -> Color.Black
                else -> TwitchBg.copy(alpha = visualState.chromeAlpha)
            },
        )
}

@Composable
private fun BoxWithConstraintsScope.PlayerSceneBody(
    scene: PlayerScene,
    visualState: MiniPlayerVisualState,
    appNavigationBarHeightPx: Int,
    slots: PlayerSlots,
) {
    val density = LocalDensity.current
    val cutout = WindowInsets.displayCutout
    val layoutDirection = LocalLayoutDirection.current
    val fullscreenSidePx =
        fullscreenCutoutSidePx(cutout.getLeft(density, layoutDirection), cutout.getRight(density, layoutDirection))
    val systemNavBarPx = WindowInsets.navigationBars.getBottom(density)
    val navBarBottomPx = if (appNavigationBarHeightPx > 0) appNavigationBarHeightPx else systemNavBarPx
    val miniWidthPx = with(density) { MiniPlayerWidth.toPx() }
    val miniPaddingPx = with(density) { MiniPlayerPadding.toPx() }
    val fullSize = scene.ui.fullSize
    if (!scene.compactSettled) fullSize.constraints = constraints
    val frame = PlayerFrame(
        playerBounds = miniPlayerBounds(
            fullWidthPx = constraints.maxWidth.toFloat(),
            fullHeightPx = constraints.maxHeight.toFloat(),
            dock = MiniPlayerDock(
                miniWidthPx = miniWidthPx,
                miniPaddingPx = miniPaddingPx,
                statusBarTopPx = WindowInsets.statusBars.getTop(density).toFloat(),
                navigationBarBottomPx = navBarBottomPx.toFloat(),
            ),
            progress = scene.progress,
        ),
        maxHeightPx = constraints.maxHeight,
        constraints = constraints,
        contentConstraints = fullSize.constraints,
        fullscreenSidePx = fullscreenSidePx,
        fullscreenSide = with(density) { fullscreenSidePx.toDp() },
        cutout = cutout,
        visualState = visualState,
        miniWidthPx = miniWidthPx,
        miniPaddingPx = miniPaddingPx,
    )
    val layout = rememberFullscreenChatLayout(scene, constraints.maxWidth - 2 * fullscreenSidePx)
    PlayerContent(scene, frame, layout, slots.content)
    PlayerVideoBox(scene, frame, layout, slots.fullscreenInfo)
    // The expanded player covers the navigation bar and its unread dot.
    WhisperUnreadGlow(
        minimized = scene.minimized,
        inPictureInPicture = scene.inPictureInPicture,
        fade = { visualState.chromeAlpha },
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .zIndex(2f),
    )
    // Over the chat too, so the note about a new chat mode stays whole.
    Box(
        modifier = Modifier
            .matchParentSize()
            .zIndex(2f)
            .windowInsetsPadding(cutout),
    ) {
        FullscreenChatModeNoticeBar(
            notice = scene.ui.chatModeNotice,
            onNextMode = { scene.switchChatMode(layout.revealChatWidthPx) },
        )
    }
    if (!scene.inPictureInPicture) slots.overlays(this, scene.compactSettled || scene.fullscreenActive)
}

/** The content under the player, covered while the player moves so a second finger does not reach it. */
@Composable
private fun BoxScope.PlayerContent(
    scene: PlayerScene,
    frame: PlayerFrame,
    layout: FullscreenChatLayout,
    content: @Composable ColumnScope.(MinimizablePlayerLayout) -> Unit,
) {
    if (!playerContentComposed(scene.inPictureInPicture, scene.compactSettled, frame.contentConstraints != null)) return
    PlayerContentArea(scene, frame, layout, content)
    if (!playerContentTakesTouches(scene.state.dragging, scene.progress)) {
        Box(modifier = Modifier.matchParentSize().blockClicks())
    }
}
