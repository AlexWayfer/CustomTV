package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.components.rememberLastNonNull

/**
 * The overlaid chat's own buttons, one group in the bottom corner away from the chat: the full height switch, filled
 * while the chat takes the full height, and Send chat, which opens the message field ([onSendChat]; null where the
 * chat takes no messages). They show all the time over the overlaid chat, resting on the control buttons like the
 * full screen icon while those show, and beside it and the chat mode button on the right while they show. When the
 * chat moves across, they slide to the other corner.
 */
@Composable
internal fun BoxScope.PlayerFullscreenChatActions(
    visible: Boolean,
    chatOnLeft: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSendChat: (() -> Unit)?,
    controlBarHeightPx: Int?,
    /**
     * How far the group stands raised on the controls, from 0 to 1: while the player's own buttons or Twitch's
     * controls show. Read when placed.
     */
    raised: () -> Float,
) {
    // A swipe can move the chat across; the fading buttons stay where they were.
    val shownChatOnLeft = rememberLastNonNull(chatOnLeft.takeIf { visible }) ?: chatOnLeft
    // 0 in the bottom left corner, 1 in the bottom right one.
    val towardEnd by animateFloatAsState(
        targetValue = if (shownChatOnLeft) 1f else 0f,
        label = "chat actions corner",
    )
    val restTop = playerControlBarTop(controlBarHeightPx)
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier
            .align(BiasAlignment(horizontalBias = towardEnd * 2f - 1f, verticalBias = 1f))
            .padding(
                start = ChatActionsSideMargin * (1f - towardEnd),
                end = ChatActionsSideMargin * towardEnd,
                // Resting on Twitch's control buttons, like the full screen icon beside it.
                bottom = restTop,
            )
            // With the controls it rises onto them and, on the right, steps clear of the player's buttons; without
            // them it goes down with the chat to the level of its lowest message, its edge gap above the bottom, and on
            // the right to the edge. One progress moves it both ways at once, read when placed, so the move does not
            // recompose the buttons.
            .offset {
                val shown = raised().coerceIn(0f, 1f)
                val hiddenDrop = fullscreenControlsHiddenDrop(restTop)
                val buttonsRoom = (PlayerButtonSize * 2 - ChatActionsSideMargin) * towardEnd
                IntOffset((-buttonsRoom * shown).roundToPx(), (hiddenDrop * (1f - shown)).roundToPx())
            },
        enter = HeaderFadeIn,
        exit = HeaderFadeOut,
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = Color.Black.copy(alpha = FULLSCREEN_OVERLAY_CHAT_ALPHA),
            contentColor = Color.White,
        ) {
            Row(
                modifier = Modifier.height(ChatActionsHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The switch stays on the side nearer the chat.
                if (shownChatOnLeft) FullHeightSwitch(expanded, onExpandedChange)
                if (onSendChat != null) {
                    if (shownChatOnLeft) ChatActionsDivider()
                    TextButton(
                        onClick = onSendChat,
                        modifier = Modifier.height(ChatActionsHeight),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                    ) {
                        Text(stringResource(R.string.fullscreen_chat_send))
                    }
                    if (!shownChatOnLeft) ChatActionsDivider()
                }
                if (!shownChatOnLeft) FullHeightSwitch(expanded, onExpandedChange)
            }
        }
    }
}

@Composable
private fun FullHeightSwitch(expanded: Boolean, onExpandedChange: (Boolean) -> Unit) {
    IconToggleButton(
        checked = expanded,
        onCheckedChange = onExpandedChange,
        modifier = Modifier.size(ChatActionsHeight),
        colors = IconButtonDefaults.iconToggleButtonColors(
            contentColor = Color.White,
            checkedContentColor = Color.White,
        ),
    ) {
        Icon(
            painter = painterResource(if (expanded) R.drawable.ic_chat_bubble else R.drawable.ic_chat_bubble_outline),
            contentDescription = stringResource(R.string.fullscreen_chat_full_height),
            // The bubble's tail hangs below its body, so the glyph sits high in its square; this centers the body.
            modifier = Modifier
                .offset(y = 2.dp)
                .size(22.dp),
        )
    }
}

@Composable
private fun ChatActionsDivider() {
    VerticalDivider(
        modifier = Modifier.height(ChatActionsHeight / 2),
        color = Color.White.copy(alpha = 0.3f),
    )
}

/**
 * How high the full screen player's own buttons in the bottom right corner sit: their middle level with the middle of
 * the chat buttons' group, which rests on the control buttons.
 */
@Composable
internal fun fullscreenPlayerButtonsBottom(controlBarHeightPx: Int?): Dp =
    (playerControlBarTop(controlBarHeightPx) + (ChatActionsHeight - PlayerButtonSize) / 2).coerceAtLeast(0.dp)

/**
 * How far the full screen player's bottom buttons go down as the controls hide: from resting on the control buttons
 * ([restTop]) to the chat's edge gap above the bottom. The chat buttons' group and the player's own buttons go together.
 */
internal fun fullscreenControlsHiddenDrop(restTop: Dp): Dp = (restTop - OverlayChatEdgeGap).coerceAtLeast(0.dp)

/** Moves a full screen bottom button down as the controls hide, by [raised] from 0 to 1, read when placed. */
internal fun Modifier.fullscreenControlsDrop(restTop: Dp, raised: () -> Float): Modifier = offset {
    val hidden = 1f - raised().coerceIn(0f, 1f)
    IntOffset(0, (fullscreenControlsHiddenDrop(restTop) * hidden).roundToPx())
}

private val ChatActionsHeight = 40.dp
private val ChatActionsSideMargin = 8.dp
