package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.EmoteSources
import name.alexwayfer.customtv.data.ChatSettings
import name.alexwayfer.customtv.ui.components.rememberLastNonNull
import name.alexwayfer.customtv.ui.theme.TwitchBg
import name.alexwayfer.customtv.ui.theme.TwitchSurfaceAlt
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

/** An arrow's width on the plaque, narrower than its touch target. */
private val HistoryArrowWidth = 32.dp

/** Over the chat above the field: the completions of the word being typed, or else the opened history plaque. */
@Composable
internal fun ChatInputOverlay(
    input: ChatInput,
    composer: ChatComposerState,
    completion: ChatCompletion?,
    historyButton: Boolean,
    actions: ChatInputActions,
) {
    val editor = actions.editor
    ChatCompletionPanel(
        completion = completion,
        chatModesLocked = input.channel.modeControl is ChatModeControl.Locked,
        onPickCommand = editor::pickCommand,
        onPickNick = editor::pickNick,
        onPickEmote = editor::pickEmote,
    )
    ChatHistoryOverlay(
        visible = chatHistoryPlaqueShown(
            open = composer.historyOpen,
            buttonShown = historyButton,
            completionShown = completion != null,
        ),
        history = input.compose.sentHistory,
        shownId = composer.historyShownId,
        appearance = rememberHistoryAppearance(input),
        actions = actions.history,
    )
}

/** How the chat draws its rows, with the stored defaults until the chat settings are read. */
@Composable
private fun rememberHistoryAppearance(input: ChatInput): ChatAppearance {
    val settings = input.menu.settings ?: ChatSettings()
    val emotes = input.compose.emotes
    val meMessageItalic = input.compose.meMessageItalic
    return remember(settings.readableColors, settings.textSize, meMessageItalic, emotes) {
        chatAppearance(
            readableColors = settings.readableColors,
            textSize = settings.textSize,
            meMessageItalic = meMessageItalic,
            badgeUrls = emptyMap(),
            emotes = ThirdPartyEmotes(
                sources = EmoteSources(
                    sevenTv = emotes.sevenTvEnabled,
                    ffz = emotes.ffzEnabled,
                    bttv = emotes.bttvEnabled,
                ),
                sevenTv = emotes.sevenTv,
                ffz = emotes.ffz,
                bttv = emotes.bttv,
            ),
        )
    }
}

/** The history plaque over the field, unfolding upward from it as the completions do. */
@Composable
private fun ChatHistoryOverlay(
    visible: Boolean,
    history: List<ChatSentMessage>,
    shownId: String?,
    appearance: ChatAppearance,
    actions: ChatHistoryActions,
) {
    val shown = chatHistoryShown(history, shownId)
    val kept = rememberLastNonNull(shown)
    AnimatedVisibility(
        visible = visible && shown != null,
        enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
    ) {
        kept?.let { message ->
            ChatHistoryPlaque(
                shown = message,
                appearance = appearance,
                canOlder = chatHistoryCanStep(history, message.id, older = true),
                canNewer = chatHistoryCanStep(history, message.id, older = false),
                onStep = actions::step,
                onPick = actions::pick,
            )
        }
    }
}

/**
 * One of the user's own messages over the field, with buttons to the older and the newer one. A swipe down pages to
 * the older message and up to the newer one; a tap puts the message in the field. Nothing on it takes the focus, so
 * the keyboard stays up.
 */
@Composable
internal fun ChatHistoryPlaque(
    shown: ChatSentMessage,
    appearance: ChatAppearance,
    canOlder: Boolean,
    canNewer: Boolean,
    onStep: (older: Boolean) -> Unit,
    onPick: (ChatSentMessage) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Every step starts here, so the plaque knows which way the next message slides in.
    var steppedOlder by remember { mutableStateOf(true) }
    val step = rememberUpdatedState { older: Boolean ->
        if (if (older) canOlder else canNewer) {
            steppedOlder = older
            onStep(older)
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .blockClicks()
            .background(TwitchBg)
            .historySwipe { older -> step.value(older) }
            .padding(start = CHAT_FIELD_START_PADDING, end = CHAT_FIELD_END_PADDING, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChatHistoryMessage(shown, appearance, steppedOlder, onPick)
        // The arrows stand closer than their 48 dp: Compose still gives each a whole touch target, and a touch between
        // them goes to the nearer one.
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            ChatHistoryStepButton(
                icon = Icons.Filled.KeyboardArrowUp,
                description = stringResource(R.string.chat_history_older),
                enabled = canOlder,
                onClick = { step.value(true) },
            )
            ChatHistoryStepButton(
                icon = Icons.Filled.KeyboardArrowDown,
                description = stringResource(R.string.chat_history_newer),
                enabled = canNewer,
                onClick = { step.value(false) },
            )
        }
    }
}

/**
 * A swipe over the plaque, past the touch slop: [onSwipe] gets whether it pages to the older message, once it ends.
 * Its moves are consumed, so the tap under it does not pick the message.
 */
private fun Modifier.historySwipe(onSwipe: (older: Boolean) -> Unit): Modifier = pointerInput(Unit) {
    var dragged = Offset.Zero
    detectDragGestures(
        onDragStart = { dragged = Offset.Zero },
        onDragEnd = { chatHistorySwipeOlder(dragged.x, dragged.y)?.let(onSwipe) },
        onDragCancel = { dragged = Offset.Zero },
    ) { change, amount ->
        change.consume()
        dragged += amount
    }
}

/**
 * The shown message, drawn as the chat draws its rows, with emotes and their effects; an older one comes from above,
 * as the chat runs, and a newer one from below.
 */
@Composable
private fun RowScope.ChatHistoryMessage(
    shown: ChatSentMessage,
    appearance: ChatAppearance,
    steppedOlder: Boolean,
    onPick: (ChatSentMessage) -> Unit,
) {
    // The ground sits on its own node around the animation. On the phone, a ground on AnimatedContent's own node
    // was sometimes gone once a page ended, while the text and the node's bounds stayed.
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(TwitchSurfaceAlt)
            .focusProperties { canFocus = false }
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.chat_history_use),
                onClick = { onPick(shown) },
            ),
    ) {
        ChatHistoryMessageContent(shown, appearance, steppedOlder)
    }
}

@Composable
private fun ChatHistoryMessageContent(shown: ChatSentMessage, appearance: ChatAppearance, steppedOlder: Boolean) {
    AnimatedContent(
        targetState = shown,
        transitionSpec = {
            val sign = if (steppedOlder) -1 else 1
            (slideInVertically { sign * it } + fadeIn())
                .togetherWith(slideOutVertically { -sign * it } + fadeOut())
                .using(SizeTransform(clip = true))
        },
        contentAlignment = Alignment.CenterStart,
        label = "chatHistoryMessage",
    ) { sent ->
        val sizes = chatRowSizes(appearance.textSize)
        Box(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            ChatUserMessage(
                // The 7TV, FFZ, and BTTV emotes go into the text as the chat's rows put them.
                message = rememberDisplayMessage(sent.message, appearance),
                readableColors = appearance.readableColors,
                timestampText = null,
                textSize = sizes.text,
                timestampSize = sizes.timestamp,
                lineHeight = sizes.lineHeight,
                emoteSize = sizes.emote,
                gifSize = sizes.gif,
                badgeSize = sizes.badge,
                badgeUrls = emptyMap(),
                meMessageItalic = appearance.meMessageItalic,
                includeAuthor = false,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                showGifs = false,
                linksClickable = false,
            )
            // An emote takes a tap for its preview; over the plaque the tap puts the whole message in the field.
            Box(Modifier.matchParentSize().blockChatTouches())
        }
    }
}

@Composable
private fun ChatHistoryStepButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .width(HistoryArrowWidth)
            .focusProperties { canFocus = false },
        // Material dims the disabled arrow from this color.
        colors = IconButtonDefaults.iconButtonColors(contentColor = TwitchTextSecondary),
    ) {
        Icon(imageVector = icon, contentDescription = description)
    }
}
