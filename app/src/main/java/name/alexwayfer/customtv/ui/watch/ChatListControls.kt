package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.theme.TwitchText

private const val MORE_BELOW_ENTER_MS = 160
private const val MORE_BELOW_EXIT_MS = 120

/** How [MoreMessagesBelowButton] comes up from below the list, by half its height. */
internal val MoreMessagesBelowEnter: EnterTransition =
    fadeIn(animationSpec = tween(MORE_BELOW_ENTER_MS)) +
        slideInVertically(animationSpec = tween(MORE_BELOW_ENTER_MS)) { it / 2 }

internal val MoreMessagesBelowExit: ExitTransition =
    fadeOut(animationSpec = tween(MORE_BELOW_EXIT_MS)) +
        slideOutVertically(animationSpec = tween(MORE_BELOW_EXIT_MS)) { it / 2 }

/**
 * Whether a row lies wholly below the last one in view. The last row cut by the bottom edge, as a selected reply that
 * unfolds, is not one: the list scrolls it into view by itself.
 */
internal fun chatRowsBelow(lastVisibleIndex: Int?, totalItems: Int): Boolean =
    lastVisibleIndex != null && lastVisibleIndex < totalItems - 1

/** [chatRowsBelow] for the list's current layout. */
internal fun LazyListState.hasRowsBelow(): Boolean =
    chatRowsBelow(layoutInfo.visibleItemsInfo.lastOrNull()?.index, layoutInfo.totalItemsCount)

/**
 * Whether [MoreMessagesBelowButton] shows: the chat stays put and rows lie below it. A held row stops the chat before
 * any row comes below. [rowsBelow] is read only for a chat that stays put, so a following one does not recompose as
 * its rows slide in.
 */
internal fun moreMessagesBelowVisible(stickToBottom: Boolean, rowsBelow: () -> Boolean): Boolean =
    !stickToBottom && rowsBelow()

@Composable
internal fun MoreMessagesBelowButton(
    chatTextSize: Int,
    onClick: () -> Unit,
) {
    val label = stringResource(R.string.chat_more_messages_below)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = TwitchText,
            modifier = Modifier.size(chatDp(chatTextSize, normal = 22, large = 24)),
        )
        Text(
            text = label,
            color = TwitchText,
            fontSize = chatSp(chatTextSize, normal = 15, large = 17),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}
