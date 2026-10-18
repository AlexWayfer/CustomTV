package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.FullscreenChatMode
import name.alexwayfer.customtv.data.FullscreenChatSide
import name.alexwayfer.customtv.data.GestureHint
import name.alexwayfer.customtv.player.PlayerEvents
import name.alexwayfer.customtv.player.PlayerGestures
import name.alexwayfer.customtv.player.TwitchPlayer
import name.alexwayfer.customtv.player.controlsShowChrome
import name.alexwayfer.customtv.player.minimizeDragHidesKeyboard
import name.alexwayfer.customtv.player.minimizeDragKeepsNavigationBar
import name.alexwayfer.customtv.player.playerTapShowsChrome
import name.alexwayfer.customtv.ui.components.VIDEO_ASPECT_RATIO
import name.alexwayfer.customtv.ui.theme.TwitchBg
import kotlin.math.roundToInt

/** The video with its minimize, full screen, and chat buttons, and the mini player's controls. */
@Composable
internal fun BoxScope.PlayerVideoBox(
    scene: PlayerScene,
    frame: PlayerFrame,
    layout: FullscreenChatLayout,
    fullscreenInfo: @Composable () -> Unit,
    seekbarOverlay: PlayerSeekbarOverlay?,
) {
    val touches = PlayerTouches(
        scene,
        layout,
        frame,
        PlayerTouchInput(
            LocalFocusManager.current,
            LocalSoftwareKeyboardController.current,
            WindowInsets.ime,
            LocalDensity.current,
        ),
    )
    val fullWidthPx = frame.contentConstraints?.maxWidth
    Box(
        modifier = Modifier.playerBox(this, scene, frame, layout)
            .then(
                if (scene.fullscreenActive) {
                    Modifier
                } else {
                    Modifier.fullscreenDragMotion(scene.fullscreen, scene.screenHeightPx)
                },
            )
            .clipToBounds()
            .background(Color.Black),
    ) {
        TwitchPlayer(
            target = scene.target,
            events = touches.events(),
            gestures = touches.gestures(),
            inPictureInPicture = scene.inPictureInPicture,
            fullscreen = scene.fullscreenActive,
            seekbarGap = seekbarOverlay?.gap ?: 0.dp,
            // The web page keeps its full size in the mini player too; only the picture shrinks.
            // A resize would show the old frame unscaled until the page draws at the new size.
            modifier = if (scene.inPictureInPicture || scene.fullscreenActive || fullWidthPx == null) {
                Modifier.fillMaxSize()
            } else {
                Modifier.playerScaledFromFullWidth(fullWidthPx)
            },
        )
        if (seekbarOverlay != null) PlayerSeekbarLayer(scene, seekbarOverlay)
        if (!scene.inPictureInPicture && !scene.compactSettled && scene.progress < MINIMIZE_BUTTON_UNTIL) {
            MinimizeButton(scene)
            FullscreenPlayerButtons(scene, frame, layout, fullscreenInfo)
        }
        if ((scene.compactSettled || frame.visualState.controlsAlpha > 0f) && !scene.inPictureInPicture) {
            MiniPlayerControls(
                expandAlpha = if (scene.compactSettled) 1f else frame.visualState.controlsAlpha,
                onExpand = { scene.expandPlayer() },
                onClose = scene.actions.onClose,
            )
        }
    }
}

/** The screen's layer over a recording's seek bar, once the page has told where the bar sits. */
@Composable
private fun BoxScope.PlayerSeekbarLayer(scene: PlayerScene, overlay: PlayerSeekbarOverlay) {
    val seekbar = scene.ui.seekbar ?: return
    if (scene.inPictureInPicture || scene.compactSettled) return
    val shown = playerSeekbarOverlayShown(
        controlsVisible = scene.chrome.playerControls,
        videoLoading = scene.chrome.videoLoading,
        settingsMenuOpen = scene.ui.twitchSettingsOpen,
        progress = scene.progress,
    )
    overlay.content(this, PlayerSeekbarSpot(seekbar, shown))
}

private fun Modifier.playerBox(
    box: BoxScope,
    scene: PlayerScene,
    frame: PlayerFrame,
    layout: FullscreenChatLayout,
): Modifier =
    this then when {
        scene.inPictureInPicture -> with(box) { Modifier.align(Alignment.Center) }
            .fillMaxWidth()
            .aspectRatio(VIDEO_ASPECT_RATIO)
        scene.compactSettled -> Modifier.fillMaxSize()
        // The drag moves the video with the chat, both around the middle of the screen.
        scene.fullscreenActive -> Modifier
            .fillMaxSize()
            .background(Color.Black)
            .fullscreenDragMotion(scene.fullscreen, scene.screenHeightPx)
            .padding(horizontal = frame.fullscreenSide)
            .windowInsetsPadding(frame.cutout.only(WindowInsetsSides.Vertical))
            .drawBehind {
                val fillTop = fullscreenPickerFillTopPx(size.height.roundToInt(), scene.ui.composerPanelPx)
                if (fillTop != null) {
                    drawRect(
                        TwitchBg,
                        topLeft = Offset(0f, fillTop.toFloat()),
                        size = Size(size.width, size.height - fillTop),
                    )
                }
            }
            .graphicsLayer {
                translationX = fullscreenPlayerSwipeShiftPx(scene.chatSwipe.offsetX, layout.chat, layout.swipeTravelPx)
            }
            .fullscreenVideo(layout.placementAt)
        else -> {
            val bounds = frame.playerBounds
            val playerCorner = RoundedCornerShape(lerp(0.dp, MiniPlayerCorner, scene.progress))
            Modifier
                .offset { IntOffset(bounds.x, bounds.y) }
                .fixedPlayerLayout(bounds.width, bounds.height)
                .shadow(lerp(0.dp, 8.dp, scene.progress), playerCorner)
                .clip(playerCorner)
        }
    }

/** What the touches on the video read beyond the scene: the focus and keyboard they dismiss. */
internal class PlayerTouchInput(
    val focusManager: FocusManager,
    val keyboard: SoftwareKeyboardController?,
    val ime: WindowInsets,
    val density: Density,
)

/** What taps and drags on the video do: show the chrome, minimize, expand, swipe away, or bring in the chat. */
private class PlayerTouches(
    private val scene: PlayerScene,
    private val layout: FullscreenChatLayout,
    private val frame: PlayerFrame,
    private val input: PlayerTouchInput,
) {
    private val state = scene.state
    private val chrome = scene.chrome
    private val tapExpands = playerTapExpands(scene.inPictureInPicture, scene.compactSettled, scene.progress)

    fun events(): PlayerEvents = PlayerEvents(
        onPositionSeconds = scene.actions.onPositionSeconds,
        onSeekSeconds = scene.actions.onSeekSeconds,
        onTap = ::tap,
        onControlsVisible = ::controlsVisible,
        onControlBarHeightPx = { scene.ui.measuredControlBarPx = it },
        onSeekbar = { scene.ui.seekbar = it },
        onSettingsMenuOpen = { scene.ui.twitchSettingsOpen = it },
        onWaitingForVideo = { chrome.videoLoading = it },
        onDismissDrag = ::dismissDrag,
        onDismissDragEnd = ::dismissDragEnd,
        onHorizontalDrag = ::horizontalDrag,
        onHorizontalDragEnd = ::horizontalDragEnd,
        onZoomIn = ::zoomIn,
        chrome = chrome.player,
    )

    fun gestures(): PlayerGestures {
        val inPictureInPicture = scene.inPictureInPicture
        val compactSettled = scene.compactSettled
        return PlayerGestures(
            enableMinimizeSwipe = !inPictureInPicture && !compactSettled,
            enableExpandSwipe = !inPictureInPicture && (
                (compactSettled && !state.swipeDragging) ||
                    (!scene.minimized && !scene.fullscreenActive && !state.collapsing)
                ),
            enableHorizontalDismiss = !inPictureInPicture && (compactSettled || layout.chatOff),
            tapExpands = tapExpands,
        )
    }

    private fun controlsVisible(visible: Boolean) {
        chrome.playerControls = visible
        if (
            controlsShowChrome(
                controlsVisible = visible,
                headerExpanded = chrome.headerExpanded,
                minimizeControl = chrome.minimizeControl,
                minimized = scene.minimized,
            )
        ) {
            chrome.followPlayer(true)
        }
    }

    private fun tap(playerControlsVisible: Boolean, reachedPage: Boolean) {
        val showChrome = playerTapShowsChrome(playerControlsVisible)
        when {
            scene.inPictureInPicture -> Unit
            tapExpands -> scene.expandPlayer()
            // The page toggled its controls on the same tap; the chrome only follows.
            reachedPage -> chrome.followPlayer(showChrome)
            // The page did not get the tap: the controls follow on request even when the chrome
            // already matches them.
            else -> {
                chrome.followPlayer(showChrome)
                chrome.player.request(showChrome)
            }
        }
    }

    /** A zoomed picture shows neither the chrome nor the player's controls. */
    private fun zoomIn() {
        chrome.followPlayer(false)
        chrome.player.request(false)
    }

    private fun dismissDrag(offsetY: Float) {
        val inPictureInPicture = scene.inPictureInPicture
        val fullscreenDrag = !inPictureInPicture && scene.fullscreen.drag(
            offsetY,
            canEnter = !scene.minimized && !scene.compactSettled && !state.collapsing,
        )
        if (fullscreenDrag || state.collapsing || inPictureInPicture) return
        state.cancelSettle()
        if (!state.dragging) {
            state.dragFromMini = scene.compactSettled || scene.minimized
            if (minimizeDragHidesKeyboard(state.dragFromMini, offsetY)) {
                val imeBottomPx = input.ime.getBottom(input.density)
                input.focusManager.clearFocus(force = true)
                input.keyboard?.hide()
                if (minimizeDragKeepsNavigationBar(state.dragFromMini, offsetY, imeBottomPx)) {
                    scene.actions.onMinimizeDragDismissesKeyboard()
                }
            }
            if (state.dragFromMini && offsetY < 0f) {
                scene.actions.onExpand()
            }
        }
        state.dragging = true
        state.dragProgress = miniPlayerDragProgress(offsetY, scene.screenHeightPx, state.dragFromMini)
    }

    private fun dismissDragEnd(offsetY: Float, velocityY: Float) {
        val inPictureInPicture = scene.inPictureInPicture
        val fullscreenDrag = !inPictureInPicture &&
            scene.fullscreen.dragEnd(offsetY, velocityY, scene.screenHeightPx) { entering ->
                scene.gestureHints?.gestureUsed(
                    if (entering) GestureHint.EnterFullscreen else GestureHint.ExitFullscreen,
                )
            }
        if (!inPictureInPicture && !fullscreenDrag) {
            val fromMini = state.dragFromMini
            state.settleDrag(offsetY, velocityY, scene.screenHeightPx) {
                if (!fromMini) scene.gestureHints?.gestureUsed(GestureHint.Minimize)
                scene.actions.onMinimize()
            }
        }
    }

    private fun horizontalDrag(offsetX: Float) {
        if (layout.chatOff) {
            scene.chatSwipe.revealDrag(offsetX, layout.revealChatWidthPx)
        } else if (scene.compactSettled && !state.collapsing && !scene.inPictureInPicture) {
            state.cancelSettle()
            state.swipeDragging = true
            state.swipeDragX = offsetX
        }
    }

    private fun horizontalDragEnd(offsetX: Float, velocityX: Float) {
        if (scene.fullscreenActive && scene.chatSwipe.revealSide != null) {
            scene.chatSwipe.revealRelease(velocityX, layout.revealChatWidthPx) { side ->
                scene.gestureHints?.gestureUsed(GestureHint.FullscreenChatHide)
                scene.fullscreen.showChat(side)
            }
        } else if (!scene.inPictureInPicture && scene.compactSettled && !state.collapsing) {
            state.settleSwipe(
                offsetX = offsetX,
                velocityX = velocityX,
                rangePx = frame.miniWidthPx.coerceAtLeast(1f),
                miniPaddingPx = frame.miniPaddingPx,
                onClose = scene.actions.onClose,
            )
        }
    }
}

@Composable
private fun BoxScope.MinimizeButton(scene: PlayerScene) {
    // Fades with the stream info panel's details, which show and hide with the same controls.
    AnimatedVisibility(
        visible = scene.chrome.minimizeControl && !scene.fullscreenActive,
        modifier = Modifier.align(Alignment.TopStart),
        enter = HeaderFadeIn,
        exit = HeaderFadeOut,
    ) {
        IconButton(
            onClick = {
                scene.gestureHints?.slowPathUsed(GestureHint.Minimize)
                scene.state.minimize(after = scene.actions.onMinimize)
            },
            modifier = Modifier
                .size(44.dp)
                .graphicsLayer {
                    alpha = (1f - scene.progress / MINIMIZE_BUTTON_FADE).coerceIn(0f, 1f)
                },
        ) {
            MinimizeChevronIcon(
                modifier = Modifier.size(26.dp),
                contentDescription = stringResource(R.string.minimize),
            )
        }
    }
}

/**
 * The stream info, the chat button, and the sleep timer at the top; full screen, the chat mode, and the overlaid
 * chat's actions at the bottom.
 */
@Composable
private fun BoxScope.FullscreenPlayerButtons(
    scene: PlayerScene,
    frame: PlayerFrame,
    layout: FullscreenChatLayout,
    fullscreenInfo: @Composable () -> Unit,
) {
    val fullscreen = scene.fullscreen
    val fullscreenActive = scene.fullscreenActive
    val shown = scene.chrome.minimizeControl
    // The buttons make room for the chat the settings show, not one a swipe is still bringing in, and
    // slide along when it comes or goes.
    val settledChat = if (fullscreenActive) {
        fullscreenChatBounds(fullscreen.chatMode, fullscreen.chatSide, layout.areaWidthPx)
    } else {
        null
    }
    val restPlacement = fullscreenPlacement(
        bounds = settledChat,
        areaWidthPx = layout.areaWidthPx,
        areaHeightPx = frame.maxHeightPx,
        keyboard = FullscreenKeyboard(),
        insets = FullscreenChatInsets(controlBarPx = 0),
    )
    val buttonsEndTarget = with(LocalDensity.current) {
        fullscreenPlayerButtonsEndPx(settledChat, restPlacement).toDp()
    }
    val buttonsEnd by animateDpAsState(targetValue = buttonsEndTarget, label = "player buttons end")
    // The compact chat keeps to its bottom corner, so the stream info and the gear take the whole width.
    val topEnd by animateDpAsState(
        targetValue = if (layout.overlayChat == FullscreenOverlayChatHeight.Compact) 0.dp else buttonsEndTarget,
        label = "player top end",
    )
    Box(modifier = Modifier.matchParentSize().padding(end = topEnd)) {
        PlayerFullscreenInfo(visible = shown && fullscreenActive, content = fullscreenInfo)
        PlayerFullscreenChatButton(visible = shown && fullscreenActive)
        PlayerSleepTimerButton(visible = shown, fullscreen = fullscreenActive)
    }
    PlayerBottomButtons(scene, layout, buttonsEnd)
}

@Composable
private fun BoxScope.PlayerBottomButtons(scene: PlayerScene, layout: FullscreenChatLayout, end: Dp) {
    val fullscreen = scene.fullscreen
    val fullscreenActive = scene.fullscreenActive
    val shown = scene.chrome.minimizeControl
    val chatOnLeft = fullscreen.chatSide == FullscreenChatSide.Left
    val raised = { scene.controlBarShown.value }
    Box(modifier = Modifier.matchParentSize().padding(end = end)) {
        PlayerFullscreenButton(
            fullscreen = fullscreen,
            visible = shown,
            controlBarHeightPx = scene.controlBarHeightPx,
            raised = raised,
        )
        PlayerFullscreenChatModeButton(
            visible = shown && fullscreenActive,
            mode = fullscreen.chatMode,
            chatOnLeft = chatOnLeft,
            controlBarHeightPx = scene.controlBarHeightPx,
            raised = raised,
            onClick = { scene.switchChatMode(layout.revealChatWidthPx) },
        )
        PlayerFullscreenChatActions(
            visible = fullscreenActive && !scene.ui.chatComposerOpen && !scene.ui.twitchSettingsOpen &&
                fullscreen.chatMode == FullscreenChatMode.Overlay && scene.chatSwipe.revealSide == null,
            chatOnLeft = chatOnLeft,
            expanded = fullscreen.overlayChatExpanded,
            onExpandedChange = { fullscreen.overlayChatExpanded = it },
            onSendChat = scene.actions.onFullscreenSendChat,
            controlBarHeightPx = scene.controlBarHeightPx,
            raised = raised,
        )
    }
}

internal val MiniPlayerWidth = 220.dp
internal val MiniPlayerPadding = 12.dp
internal val MiniPlayerCorner = 10.dp

// The minimize button shows only near the full player and fades out on the way to the corner.
private const val MINIMIZE_BUTTON_UNTIL = 0.2f
private const val MINIMIZE_BUTTON_FADE = 0.18f
