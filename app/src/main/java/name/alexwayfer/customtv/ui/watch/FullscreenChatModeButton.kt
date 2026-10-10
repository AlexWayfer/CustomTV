package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.FullscreenChatMode
import name.alexwayfer.customtv.ui.components.rememberLastNonNull
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.theme.TwitchSurface
import name.alexwayfer.customtv.ui.theme.TwitchText
import kotlin.time.Duration.Companion.seconds

/** The mode the full screen chat button switches to: over the video, then beside it, then none, then over again. */
internal fun fullscreenChatNextMode(mode: FullscreenChatMode): FullscreenChatMode = when (mode) {
    FullscreenChatMode.Overlay -> FullscreenChatMode.Column
    FullscreenChatMode.Column -> FullscreenChatMode.Hidden
    FullscreenChatMode.Hidden -> FullscreenChatMode.Overlay
}

/** A note over the full screen player that the chat button switched to [mode]; a new one for every switch. */
internal class FullscreenChatModeNotice(val mode: FullscreenChatMode)

/**
 * Switches the full screen chat to its next mode; shows with the player's controls, beside the full screen icon. The
 * icon shows the current mode, and the column turns to the side the chat takes.
 */
@Composable
internal fun BoxScope.PlayerFullscreenChatModeButton(
    visible: Boolean,
    mode: FullscreenChatMode,
    chatOnLeft: Boolean,
    controlBarHeightPx: Int?,
    /** How far the controls show, from 0 to 1: the button goes down with the chat buttons as they hide. */
    raised: () -> Float,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = PlayerButtonSize, bottom = fullscreenPlayerButtonsBottom(controlBarHeightPx))
            .fullscreenControlsDrop(playerControlBarTop(controlBarHeightPx), raised),
        enter = HeaderFadeIn,
        exit = HeaderFadeOut,
    ) {
        val modeLabel = stringResource(mode.label)
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(PlayerButtonSize)
                .semantics { stateDescription = modeLabel },
        ) {
            Icon(
                painter = painterResource(mode.icon),
                contentDescription = stringResource(R.string.fullscreen_chat_mode),
                modifier = Modifier
                    .size(26.dp)
                    .graphicsLayer {
                        // The sidebar in the column icon stands on the chat's side.
                        if (mode == FullscreenChatMode.Column && chatOnLeft) scaleX = -1f
                    },
                tint = Color.White,
            )
        }
    }
}

/** The note at the top of the full screen player that the chat mode changed, with a way on to the next mode. */
@Composable
internal fun BoxScope.FullscreenChatModeNoticeBar(notice: FullscreenChatModeNotice?, onNextMode: () -> Unit) {
    val shown = rememberLastNonNull(notice)
    AnimatedVisibility(
        visible = notice != null,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 12.dp),
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            shape = RoundedCornerShape(12.dp),
            color = TwitchSurface,
            contentColor = TwitchText,
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource((shown?.mode ?: FullscreenChatMode.Overlay).noticeText),
                    modifier = Modifier.padding(vertical = 12.dp).weight(1f, fill = false),
                    style = MaterialTheme.typography.bodyLarge,
                )
                TextButton(onClick = onNextMode) {
                    Text(stringResource(R.string.fullscreen_chat_next_mode), color = TwitchPurple)
                }
            }
        }
    }
}

/** How long the note about a new chat mode stays. */
internal val FullscreenChatModeNoticeDuration = 3.seconds

private val FullscreenChatMode.icon: Int
    get() = when (this) {
        FullscreenChatMode.Overlay -> R.drawable.ic_fullscreen_chat_overlay
        FullscreenChatMode.Column -> R.drawable.ic_fullscreen_chat_column
        FullscreenChatMode.Hidden -> R.drawable.ic_fullscreen_chat_hidden
    }

private val FullscreenChatMode.noticeText: Int
    get() = when (this) {
        FullscreenChatMode.Overlay -> R.string.fullscreen_chat_changed_overlay
        FullscreenChatMode.Column -> R.string.fullscreen_chat_changed_column
        FullscreenChatMode.Hidden -> R.string.fullscreen_chat_changed_hidden
    }
