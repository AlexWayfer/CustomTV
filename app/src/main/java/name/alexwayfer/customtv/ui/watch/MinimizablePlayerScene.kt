package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import name.alexwayfer.customtv.data.FullscreenChatMode
import name.alexwayfer.customtv.data.FullscreenChatSide
import name.alexwayfer.customtv.data.GestureHint
import name.alexwayfer.customtv.player.PlayerSeekbar
import name.alexwayfer.customtv.player.TwitchPlaybackTarget
import name.alexwayfer.customtv.ui.GestureHintReporter

/** What the player does when it is minimized, expanded, closed, or moved through, and what it reports. */
internal data class PlayerActions(
    val onMinimize: () -> Unit,
    val onExpand: () -> Unit,
    val onClose: () -> Unit,
    /** Back on the full player goes here instead of minimizing it, such as to the channel watched before. */
    val onBack: (() -> Unit)? = null,
    val onMinimizeDragDismissesKeyboard: () -> Unit = {},
    val onExpanded: () -> Unit = {},
    val onPositionSeconds: (Double) -> Unit = {},
    val onSeekSeconds: (Double) -> Unit = {},
    /** Opens the message field from Send chat over a full screen player; null where the chat takes no messages. */
    val onFullscreenSendChat: (() -> Unit)? = null,
)

/** The player's own state around the video: its control bar, the chat's composer, and the chat mode note. */
@Stable
internal class PlayerSceneState {
    var measuredControlBarPx by mutableStateOf<Int?>(null)

    // Where a recording's seek bar sits.
    var seekbar by mutableStateOf<PlayerSeekbar?>(null)
    var composerPanelPx by mutableIntStateOf(0)
    var chatComposerOpen by mutableStateOf(false)

    // Twitch's settings menu covers the bottom corner while it is open.
    var twitchSettingsOpen by mutableStateOf(false)
    var chatModeNotice by mutableStateOf<FullscreenChatModeNotice?>(null)
    val compactChatHeight = FullscreenCompactChatHeight()
    val keyboardMemory = FullscreenKeyboardMemory()

    /** The content stays composed behind the mini player, so expanding does not rebuild the chat. */
    val fullSize = FullSizeMemory()
}

/** The expanded player's constraints, kept while it is minimized; read during composition only. */
internal class FullSizeMemory {
    var constraints: Constraints? = null
}

/** What the player's parts read as of this composition. */
internal data class PlayerScene(
    val state: MinimizablePlayerState,
    val chrome: PlayerChromeVisibility,
    val ui: PlayerSceneState,
    val fullscreen: PlayerFullscreen,
    val chatSwipe: FullscreenChatSwipeState,
    val target: TwitchPlaybackTarget,
    val actions: PlayerActions,
    val minimized: Boolean,
    val inPictureInPicture: Boolean,
    /** How far the player is in the corner, from 0 full to 1 mini. */
    val progress: Float,
    val screenHeightPx: Float,
    val controlBarHeightPx: Int?,
    /** How far the room for the control bar is open; read when laid out. */
    val controlBarShown: State<Float>,
    val gestureHints: GestureHintReporter?,
) {
    /** The mini player rests in the corner; picture-in-picture shows the full one. */
    val compactSettled: Boolean get() = minimized && !inPictureInPicture
    val fullscreenActive: Boolean get() = fullscreen.active

    fun expandPlayer() {
        chrome.show(true)
        state.expand(minimized, actions.onExpand)
    }

    /** A hidden chat slides in from its side, as the swipe brings it. */
    fun switchChatMode(revealChatWidthPx: Int) {
        val next = fullscreenChatNextMode(fullscreen.chatMode)
        if (fullscreen.chatMode == FullscreenChatMode.Hidden) {
            chatSwipe.reveal(fullscreen.chatSide, revealChatWidthPx, fullscreen::showChat)
        } else {
            fullscreen.setChatMode(next)
        }
        ui.chatModeNotice = FullscreenChatModeNotice(next)
        if (next == FullscreenChatMode.Hidden) gestureHints?.slowPathUsed(GestureHint.FullscreenChatHide)
    }
}

/**
 * The control bar the player's buttons stand on. A recording's buttons clear its time labels by the room around
 * their icon, and glide as the measured top moves, such as when the labels load. A stream's rest on its control bar
 * as measured.
 */
@Composable
internal fun playerControlBarHeightPx(target: TwitchPlaybackTarget, measuredControlBarPx: Int?): Int? {
    val density = LocalDensity.current
    val recordingControlBarPx by animateIntAsState(
        targetValue = with(density) {
            (measuredControlBarPx ?: TwitchControlBarHeight.roundToPx()) + RecordingButtonsGap.roundToPx()
        },
        label = "recording control bar",
    )
    return if (target is TwitchPlaybackTarget.Video) recordingControlBarPx else measuredControlBarPx
}

/**
 * An overlaid chat and its buttons rise above the control bar while either Twitch's controls or the player's own
 * buttons show, at the pace of those buttons, so neither jumps while Twitch's bar comes and goes as the stream
 * loads. Read when laid out, so the chat resizes without recomposing.
 */
@Composable
internal fun animateControlBarShown(chrome: PlayerChromeVisibility): State<Float> {
    val controlsUp = chrome.playerControls || chrome.minimizeControl
    return animateFloatAsState(
        targetValue = if (controlsUp) 1f else 0f,
        animationSpec = tween(if (controlsUp) HEADER_FADE_IN_MS else HEADER_FADE_OUT_MS),
        label = "control bar room",
    )
}

/** Where the player sits in its frame this composition: its bounds, the content's size, and the cutout's room. */
internal data class PlayerFrame(
    val playerBounds: MiniPlayerBounds,
    val maxHeightPx: Int,
    val constraints: Constraints,
    /** The full player's constraints, kept while it is minimized; null until it was first full. */
    val contentConstraints: Constraints?,
    val fullscreenSidePx: Int,
    val fullscreenSide: Dp,
    val cutout: WindowInsets,
    val visualState: MiniPlayerVisualState,
    val miniWidthPx: Float,
    val miniPaddingPx: Float,
)

/** Where the chat and the video go on a full screen player. */
internal data class FullscreenChatLayout(
    val areaWidthPx: Int,
    val chat: FullscreenChatBounds?,
    /** Off in the settings, even while a swipe or the Chat button brings it in. */
    val chatOff: Boolean,
    val overlayChat: FullscreenOverlayChatHeight,
    /** The full chat stays while it sinks back into the compact one; read when drawn, so it does not recompose. */
    val overlayExpandProgress: State<Float>,
    val revealChatWidthPx: Int,
    val swipeTravelPx: Int,
    /**
     * Read when laid out, so the keyboard moves the video and the chat without recomposing them. The emote picker
     * counts as the keyboard: it takes its place.
     */
    val placementAt: MeasureScope.(Constraints) -> FullscreenPlacement,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun rememberFullscreenChatLayout(scene: PlayerScene, areaWidthPx: Int): FullscreenChatLayout {
    val fullscreen = scene.fullscreen
    val chat = if (scene.fullscreenActive) {
        // A hidden chat coming in from a side lies over the video there until it settles.
        val revealSide = scene.chatSwipe.revealSide
        fullscreenChatBounds(
            mode = if (revealSide != null) FullscreenChatMode.Overlay else fullscreen.chatMode,
            side = revealSide ?: fullscreen.chatSide,
            areaWidthPx = areaWidthPx,
        )
    } else {
        null
    }
    val overlayExpandProgress = animateFloatAsState(
        targetValue = if (fullscreen.overlayChatExpanded) 1f else 0f,
        animationSpec = tween(COMPACT_CHAT_MOTION_MS),
        label = "overlay chat expand",
    )
    val overlaySinking by remember { derivedStateOf { overlayExpandProgress.value > 0f } }
    val revealChatBounds = fullscreenChatBounds(FullscreenChatMode.Overlay, FullscreenChatSide.Right, areaWidthPx)
    val ime = WindowInsets.ime
    val imeTarget = WindowInsets.imeAnimationTarget
    val ui = scene.ui
    val controlBarHeightPx = scene.controlBarHeightPx
    val controlBarShown = scene.controlBarShown
    return FullscreenChatLayout(
        areaWidthPx = areaWidthPx,
        chat = chat,
        chatOff = scene.fullscreenActive && fullscreen.chatMode == FullscreenChatMode.Hidden,
        overlayChat = fullscreenOverlayChatHeight(
            overPlayer = chat?.overPlayer == true,
            expanded = fullscreen.overlayChatExpanded || overlaySinking,
            composerOpen = ui.chatComposerOpen,
        ),
        overlayExpandProgress = overlayExpandProgress,
        revealChatWidthPx = revealChatBounds?.chatWidth ?: 0,
        swipeTravelPx = areaWidthPx - (chat?.chatWidth ?: 0),
        placementAt = { area ->
            val (keyboardHeightPx, keyboardProgress) = ui.keyboardMemory.measure(
                imePx = ime.getBottom(this),
                imeTargetPx = imeTarget.getBottom(this),
                pickerPx = ui.composerPanelPx,
            )
            fullscreenPlacement(
                bounds = chat,
                areaWidthPx = area.maxWidth,
                areaHeightPx = area.maxHeight,
                keyboard = FullscreenKeyboard(keyboardHeightPx, keyboardProgress),
                insets = FullscreenChatInsets(
                    // The bar's usual height until the page reports it, as the player's own buttons take it, so the
                    // chat does not sit on the screen's bottom and then jump up to the bar.
                    controlBarPx = controlBarHeightPx ?: TwitchControlBarHeight.roundToPx(),
                    controlBarShown = controlBarShown.value,
                    chatEdgePx = OverlayChatEdgeGap.roundToPx(),
                ),
            )
        },
    )
}
