package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChatModePlaque
import name.alexwayfer.customtv.chat.ChatNick
import name.alexwayfer.customtv.chat.PickerEmote
import name.alexwayfer.customtv.data.ChatSendError
import name.alexwayfer.customtv.ui.components.rememberLastNonNull
import name.alexwayfer.customtv.ui.theme.TwitchBg
import name.alexwayfer.customtv.ui.theme.TwitchLive
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

/** What the list above the chat field offers for the word being typed. */
internal sealed interface ChatCompletion {
    data class Commands(val commands: List<ChatCommand>) : ChatCompletion
    data class Nicks(val nicks: List<ChatNick>) : ChatCompletion
    data class Emotes(val emotes: List<PickerEmote>) : ChatCompletion
}

/** Matching commands win over nicks, and nicks over emotes; nothing to offer closes the list. */
internal fun chatCompletion(commands: List<ChatCommand>, nicks: List<ChatNick>, emotes: List<PickerEmote>): ChatCompletion? = when {
    commands.isNotEmpty() -> ChatCompletion.Commands(commands)
    nicks.isNotEmpty() -> ChatCompletion.Nicks(nicks)
    emotes.isNotEmpty() -> ChatCompletion.Emotes(emotes)
    else -> null
}

/**
 * The completion list over the chat field. It unfolds upward from the field and folds back into it,
 * on an opaque background over the newest chat messages, so the chat keeps its place.
 */
@Composable
internal fun ChatCompletionPanel(
    completion: ChatCompletion?,
    onPickCommand: (ChatCommand) -> Unit,
    onPickNick: (ChatNick) -> Unit,
    onPickEmote: (PickerEmote) -> Unit,
) {
    val shown = rememberLastNonNull(completion)
    val listModifier = Modifier.padding(start = CHAT_FIELD_START_PADDING, end = CHAT_FIELD_END_PADDING, top = 8.dp)
    AnimatedVisibility(
        visible = completion != null,
        enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
    ) {
        // A new set of suggestions fades in over the old one while the list takes its new height. Both stay
        // on the field and only the top edge moves, so rows that are gone fade out rather than vanish.
        AnimatedContent(
            targetState = shown,
            modifier = Modifier.fillMaxWidth().blockClicks().background(TwitchBg),
            transitionSpec = { (fadeIn() togetherWith fadeOut()).using(SizeTransform(clip = true)) },
            contentAlignment = Alignment.BottomStart,
            label = "chatCompletionSet",
        ) { set ->
            when (set) {
                is ChatCompletion.Commands -> ChatCommandCompletionList(
                    commands = set.commands,
                    onPick = onPickCommand,
                    modifier = listModifier,
                )
                is ChatCompletion.Nicks -> NickCompletionList(nicks = set.nicks, onPick = onPickNick, modifier = listModifier)
                is ChatCompletion.Emotes -> EmoteCompletionList(emotes = set.emotes, onPick = onPickEmote, modifier = listModifier)
                null -> Unit
            }
        }
    }
}

/**
 * The room above a bar of [barHeightPx] for content laid over what is above it: whatever of
 * [maxHeightPx] the bar leaves. Unbounded stays unbounded; never below zero.
 */
internal fun overlayRoomAbovePx(maxHeightPx: Int, barHeightPx: Int): Int =
    if (maxHeightPx == Constraints.Infinity) Constraints.Infinity else (maxHeightPx - barHeightPx).coerceAtLeast(0)

/**
 * Lays [bar] out as usual and [overlay] right above it, over the content above, without taking room
 * in the column: what is above does not move when the overlay opens. The overlay gets the height the
 * column has left above the bar, so with the keyboard up it shrinks and scrolls there.
 */
@Composable
internal fun BarWithOverlayAbove(
    overlay: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    bar: @Composable () -> Unit,
) {
    Layout(contents = listOf(bar, overlay), modifier = modifier) { (barMeasurables, overlayMeasurables), constraints ->
        val barPlaceables = barMeasurables.map { it.measure(constraints) }
        val width = barPlaceables.maxOfOrNull { it.width } ?: constraints.minWidth
        val height = barPlaceables.maxOfOrNull { it.height } ?: constraints.minHeight
        val room = overlayRoomAbovePx(constraints.maxHeight, height)
        val overlayPlaceables = overlayMeasurables.map {
            it.measure(constraints.copy(minHeight = 0, maxHeight = room))
        }
        layout(width, height) {
            barPlaceables.forEach { it.place(0, 0) }
            overlayPlaceables.forEach { it.place(0, -it.height) }
        }
    }
}

/**
 * The chat mode plaques and the last send error over the field. Each rises from the field as it
 * fades in and sinks back as it fades out, so the field glides instead of jumping.
 */
@Composable
internal fun ColumnScope.ChatFieldNotes(
    plaques: List<ChatModePlaque>,
    signedIn: Boolean,
    sendError: ChatSendError?,
    onFollow: () -> Unit,
    onSubscribe: () -> Unit,
) {
    val shownPlaques = rememberLastNonNull(plaques.takeIf { it.isNotEmpty() })
    AnimatedVisibility(
        visible = plaques.isNotEmpty(),
        enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
    ) {
        ChatModePlaqueList(
            plaques = shownPlaques.orEmpty(),
            signedIn = signedIn,
            onFollow = onFollow,
            onSubscribe = onSubscribe,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
    val shownError = rememberLastNonNull(sendError)
    AnimatedVisibility(
        visible = sendError != null,
        enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
    ) {
        shownError?.let { error ->
            Text(
                text = chatSendErrorText(error),
                color = TwitchLive,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 4.dp, end = 8.dp),
            )
        }
    }
}

/** What the button at the field's end shows. */
internal enum class ChatFieldEndIcon { Sending, Send, Menu }

/** A message on its way shows progress; a typed message can be sent; otherwise the button opens the menu. */
internal fun chatFieldEndIcon(sending: Boolean, hasMessage: Boolean): ChatFieldEndIcon = when {
    sending -> ChatFieldEndIcon.Sending
    hasMessage -> ChatFieldEndIcon.Send
    else -> ChatFieldEndIcon.Menu
}

/** The end button's icon; one icon fades and shrinks away as the next one grows in. */
@Composable
internal fun ChatFieldEndIconContent(icon: ChatFieldEndIcon) {
    AnimatedContent(
        targetState = icon,
        transitionSpec = { (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut()) },
        contentAlignment = Alignment.Center,
        label = "chatFieldEndIcon",
    ) { shown ->
        when (shown) {
            ChatFieldEndIcon.Sending -> CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = TwitchPurple,
            )
            ChatFieldEndIcon.Send -> Icon(
                Icons.AutoMirrored.Filled.Send,
                stringResource(R.string.chat_send),
                tint = TwitchPurple,
            )
            ChatFieldEndIcon.Menu -> Icon(Icons.Filled.MoreVert, stringResource(R.string.chat_menu), tint = TwitchTextSecondary)
        }
    }
}
