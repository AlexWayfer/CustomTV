package name.alexwayfer.customtv.ui.watch

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LocalLifecycleOwner
import name.alexwayfer.customtv.data.GestureHint
import name.alexwayfer.customtv.ui.components.VIDEO_ASPECT_RATIO

/** The content under the player, such as the stream info and the chat, or the chat beside a full screen player. */
@Composable
internal fun PlayerContentArea(
    scene: PlayerScene,
    frame: PlayerFrame,
    layout: FullscreenChatLayout,
    content: @Composable ColumnScope.(MinimizablePlayerLayout) -> Unit,
) {
    val playerLayout = rememberMinimizablePlayerLayout(scene, frame, layout)
    // Back handlers of hidden content register on a dispatcher that no Back reaches.
    val hiddenBack = rememberHiddenBackOwner()
    val backOwner = LocalOnBackPressedDispatcherOwner.current
    val chatSheetMaxHeightPx = remember(playerLayout) {
        { chatSheetMaxHeightPx(playerLayout.containerHeightPx, playerLayout.playerBottomPx()) }
    }
    val contentHidden = scene.compactSettled || (scene.fullscreenActive && layout.chat == null)
    CompositionLocalProvider(
        LocalPlayerContentHidden provides contentHidden,
        LocalPlayerContentFullscreen provides scene.fullscreenActive,
        LocalChatSheetMaxHeightPx provides chatSheetMaxHeightPx,
        LocalOnBackPressedDispatcherOwner provides
            if (contentHidden || backOwner == null) hiddenBack else backOwner,
    ) {
        Column(modifier = Modifier.playerContent(scene, frame, layout, contentHidden, Modifier.blockClicks())) {
            if (!scene.fullscreenActive) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(VIDEO_ASPECT_RATIO),
                )
            }
            content(playerLayout)
        }
    }
}

@Composable
private fun rememberMinimizablePlayerLayout(
    scene: PlayerScene,
    frame: PlayerFrame,
    layout: FullscreenChatLayout,
): MinimizablePlayerLayout {
    val chatBackgroundAlpha by rememberUpdatedState(
        if (layout.chat?.overPlayer == true) FULLSCREEN_OVERLAY_CHAT_ALPHA else frame.visualState.chromeAlpha,
    )
    val playerBottomPx by rememberUpdatedState(frame.playerBounds.y + frame.playerBounds.height)
    val chatOverVideo by rememberUpdatedState(layout.chat?.overPlayer == true)
    val containerHeightPx = frame.contentConstraints?.maxHeight ?: frame.maxHeightPx
    val fullscreenActive = scene.fullscreenActive
    val overlayChat = layout.overlayChat
    val ui = scene.ui
    val fullscreen = scene.fullscreen
    val overlayExpandProgress = layout.overlayExpandProgress
    return remember(containerHeightPx, fullscreenActive, overlayChat) {
        MinimizablePlayerLayout(
            chatBackgroundAlpha = { chatBackgroundAlpha },
            containerHeightPx = containerHeightPx,
            playerBottomPx = { playerBottomPx },
            fullscreen = fullscreenActive,
            onComposerPanelPx = { ui.composerPanelPx = it },
            chatOverVideo = { chatOverVideo },
            overlayChat = overlayChat,
            onOverlayChatExpand = { fullscreen.overlayChatExpanded = true },
            overlayChatMorph = {
                overlayChatMorphProgress(
                    overlaid = overlayChat == FullscreenOverlayChatHeight.Full,
                    expandProgress = overlayExpandProgress.value,
                )
            },
            compactChatHeight = ui.compactChatHeight,
            onComposerOpenChange = { ui.chatComposerOpen = it },
        )
    }
}

@Composable
private fun rememberHiddenBackOwner(): OnBackPressedDispatcherOwner {
    val lifecycleOwner = LocalLifecycleOwner.current
    return remember(lifecycleOwner) {
        object : OnBackPressedDispatcherOwner {
            override val onBackPressedDispatcher = OnBackPressedDispatcher()
            override val lifecycle get() = lifecycleOwner.lifecycle
        }
    }
}

/** Keeps the content at full size behind the mini player, and lays the chat beside or over a full screen player. */
private fun Modifier.playerContent(
    scene: PlayerScene,
    frame: PlayerFrame,
    layout: FullscreenChatLayout,
    hidden: Boolean,
    clickBlocker: Modifier,
): Modifier = this
    .hiddenAtFullSize(hidden, frame.contentConstraints ?: frame.constraints)
    .fillMaxSize()
    .then(layout.chat?.let { chat -> Modifier.fullscreenChatFrame(scene, frame, layout, chat) } ?: Modifier)
    .then(clickBlocker)
    .then(
        if (frame.visualState.chromeAlpha < 1f) {
            Modifier.graphicsLayer { alpha = frame.visualState.chromeAlpha }
        } else {
            Modifier
        },
    )
    .statusBarsPadding()

private fun Modifier.fullscreenChatFrame(
    scene: PlayerScene,
    frame: PlayerFrame,
    layout: FullscreenChatLayout,
    chat: FullscreenChatBounds,
): Modifier {
    val overlayChat = layout.overlayChat
    val fullscreen = scene.fullscreen
    // Above the player box, whose black background fills the screen.
    return this
        .zIndex(1f)
        .fullscreenDragMotion(fullscreen, scene.screenHeightPx)
        .padding(horizontal = frame.fullscreenSide)
        .windowInsetsPadding(frame.cutout.only(WindowInsetsSides.Vertical))
        .fullscreenChat(
            compact = overlayChat == FullscreenOverlayChatHeight.Compact,
            compactHeight = scene.ui.compactChatHeight,
            // Only the overlaid chat rises and sinks; a column or a typing chat stays whole.
            expandProgress = {
                if (overlayChat == FullscreenOverlayChatHeight.Full) layout.overlayExpandProgress.value else 1f
            },
            placement = layout.placementAt,
        )
        .fullscreenChatSwipe(
            swipe = scene.chatSwipe,
            track = FullscreenChatSwipeTrack(chat.chatOnLeft, chat.chatWidth, layout.swipeTravelPx),
            onHide = {
                scene.gestureHints?.gestureUsed(GestureHint.FullscreenChatHide)
                fullscreen.hideChat()
            },
            onMoveAcross = {
                scene.gestureHints?.gestureUsed(GestureHint.FullscreenChatSide)
                fullscreen.moveChatAcross()
            },
        )
        // The compact chat rounds each message instead.
        .then(
            if (overlayChat == FullscreenOverlayChatHeight.Compact) {
                Modifier
            } else {
                Modifier.clip(fullscreenChatShape(chat))
            },
        )
}
