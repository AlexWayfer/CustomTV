package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.then
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.theme.TwitchHint
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.theme.TwitchSurfaceAlt
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

// Lower than Material's 56 dp field, so the chat keeps more room above the keyboard.
private val ChatFieldMinHeight = 40.dp
private val ChatFieldVerticalPadding = 8.dp

// Between the history button and the text: half of Material's 16 dp start padding, which the empty slot makes up.
private val ChatFieldHistoryGap = 8.dp

/** What lies over the message field and takes its taps instead of the keyboard. */
internal enum class ChatFieldCover { None, LogIn, Rules }

/** Logged out, a tap logs in; with rules to confirm, a tap on the unfocused field opens them. */
internal fun chatFieldCover(canSend: Boolean, focused: Boolean, rulesNeedConfirmation: Boolean): ChatFieldCover = when {
    loggedOutChatFieldOpensLogin(canSend) -> ChatFieldCover.LogIn
    !focused && rulesNeedConfirmation -> ChatFieldCover.Rules
    else -> ChatFieldCover.None
}

/** How the bar draws the field: its colors, the emotes it marks, whether the composer is open and shows history. */
internal class ChatFieldLook(
    val colors: TextFieldColors,
    val emotes: ChatDraftEmotes,
    val composerOpen: Boolean,
    val historyButton: Boolean,
)

/** The field's colors; its ground lets the video show through while the closed bar does. */
@Composable
internal fun chatFieldColors(barAlpha: Float): TextFieldColors = TextFieldDefaults.colors(
    focusedContainerColor = TwitchSurfaceAlt.copy(alpha = barAlpha),
    unfocusedContainerColor = TwitchSurfaceAlt.copy(alpha = barAlpha),
    disabledContainerColor = TwitchSurfaceAlt.copy(alpha = barAlpha),
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    focusedPlaceholderColor = TwitchHint,
    unfocusedPlaceholderColor = TwitchHint,
    disabledPlaceholderColor = TwitchHint,
    focusedTextColor = TwitchText,
    unfocusedTextColor = TwitchText,
    disabledTextColor = TwitchText,
    cursorColor = TwitchPurple,
)

/** The message field with its emote picker button, under a cover that opens Log in or the rules first. */
@Composable
internal fun RowScope.ChatMessageField(
    input: ChatInput,
    composer: ChatComposerState,
    field: ChatFieldState,
    actions: ChatInputActions,
    look: ChatFieldLook,
) {
    val cover = chatFieldCover(input.user.canSend, field.focused, input.rulesNeedConfirmation)
    Box(
        modifier = Modifier
            .weight(1f)
            .onGloballyPositioned { field.rightPx = it.boundsInWindow().right },
    ) {
        ChatTextField(input, composer, field, actions, look)
        ChatFieldCoverTarget(cover, input, field, actions)
    }
}

@Composable
private fun ChatTextField(
    input: ChatInput,
    composer: ChatComposerState,
    field: ChatFieldState,
    actions: ChatInputActions,
    look: ChatFieldLook,
) {
    val canSend = input.user.canSend
    val covered = chatFieldCover(canSend, field.focused, input.rulesNeedConfirmation) != ChatFieldCover.None
    val materialDecorator = chatFieldDecorator(input, composer, field, actions, look)
    val draft = input.compose.draft
    val output = look.emotes.output
    val textStyle = LocalTextStyle.current.merge(TextStyle(color = TwitchText))
    val emotePictures = rememberChatDraftEmotePictures(draft, output, textStyle)
    val emoteMarks = Modifier.chatDraftEmoteMarks(
        output = output,
        draft = { draft.text },
        textLayout = { field.textLayout?.invoke() },
        pictures = emotePictures,
        scrollState = field.scroll,
    )
    val inputTransformation = remember(output, field.limit) { ChatDraftEmoteBackspace(output).then(field.limit) }
    CompositionLocalProvider(LocalTextSelectionColors provides look.colors.textSelectionColors) {
        BasicTextField(
            state = draft,
            enabled = canSend,
            inputTransformation = inputTransformation,
            outputTransformation = output,
            lineLimits = TextFieldLineLimits.SingleLine,
            interactionSource = field.presses,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            onKeyboardAction = { actions.submit() },
            textStyle = textStyle,
            cursorBrush = SolidColor(TwitchPurple),
            onTextLayout = { getResult -> field.textLayout = getResult },
            scrollState = field.scroll,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(
                    minWidth = TextFieldDefaults.MinWidth,
                    minHeight = ChatFieldMinHeight,
                )
                .onPreviewKeyEvent { recallHistoryOnKey(it, actions) }
                .then(
                    if (covered) {
                        Modifier.clearAndSetSemantics {}
                    } else {
                        Modifier
                            .focusRequester(field.focus)
                            .onFocusChanged { focus -> field.onFocusChanged(focus.isFocused, composer) }
                    },
                ),
            decorator = TextFieldDecorator { innerTextField ->
                materialDecorator.Decoration {
                    Box(modifier = emoteMarks) {
                        innerTextField()
                    }
                }
            },
        )
    }
}

/** Up and Down on a hardware keyboard, without a modifier key, bring back the user's own messages, as on a computer. */
private fun recallHistoryOnKey(event: KeyEvent, actions: ChatInputActions): Boolean {
    val modified = event.isShiftPressed || event.isAltPressed || event.isCtrlPressed || event.isMetaPressed
    return event.type == KeyEventType.KeyDown && !modified && when (event.key) {
        Key.DirectionUp -> actions.history.recall(older = true)
        Key.DirectionDown -> actions.history.recall(older = false)
        else -> false
    }
}

/** The Material field around the text: its ground, the hint, the history button, and the emote picker button. */
@Composable
private fun chatFieldDecorator(
    input: ChatInput,
    composer: ChatComposerState,
    field: ChatFieldState,
    actions: ChatInputActions,
    look: ChatFieldLook,
): TextFieldDecorator {
    val canSend = input.user.canSend
    val historySlot = canSend && input.compose.historyEnabled
    return TextFieldDefaults.decorator(
        state = input.compose.draft,
        enabled = canSend,
        lineLimits = TextFieldLineLimits.SingleLine,
        outputTransformation = look.emotes.output,
        interactionSource = field.presses,
        placeholder = {
            Text(
                stringResource(if (canSend) R.string.chat_input_hint else R.string.chat_input_logged_out),
                color = TwitchHint,
            )
        },
        // Kept while history is on, so the text keeps its place while the button grows in and out of it.
        leadingIcon = if (historySlot) {
            { ChatHistoryButton(look.historyButton, composer.historyOpen, onClick = actions.history::toggle) }
        } else {
            null
        },
        trailingIcon = if (canSend) {
            { EmotePickerButton(composer.pickerOpen, field, onClick = actions::togglePicker) }
        } else {
            null
        },
        colors = look.colors,
        // The history slot makes up the rest of the start padding while its button is away.
        contentPadding = if (historySlot) {
            TextFieldDefaults.contentPaddingWithoutLabel(
                start = ChatFieldHistoryGap,
                top = ChatFieldVerticalPadding,
                bottom = ChatFieldVerticalPadding,
            )
        } else {
            TextFieldDefaults.contentPaddingWithoutLabel(
                top = ChatFieldVerticalPadding,
                bottom = ChatFieldVerticalPadding,
            )
        },
        container = {
            TextFieldDefaults.Container(
                enabled = canSend,
                isError = false,
                interactionSource = field.presses,
                colors = look.colors,
                shape = RoundedCornerShape(8.dp),
            )
        },
    )
}

@Composable
private fun EmotePickerButton(pickerOpen: Boolean, field: ChatFieldState, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.onGloballyPositioned { field.pickerButtonLeftPx = it.boundsInWindow().left },
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_emote_picker),
            contentDescription = stringResource(R.string.emote_picker_open),
            tint = if (pickerOpen) TwitchPurple else TwitchTextSecondary,
        )
    }
}

/**
 * The button that opens the plaque of the user's own messages. It grows in from the field's start while the keyboard
 * or the picker is up and takes no focus, so the keyboard stays.
 */
@Composable
private fun ChatHistoryButton(visible: Boolean, open: Boolean, onClick: () -> Unit) {
    // Without the button the slot keeps the gap, so the text stays where a field without history has it.
    Box(Modifier.widthIn(min = ChatFieldHistoryGap)) {
        AnimatedVisibility(
            visible = visible,
            enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
            exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(),
        ) {
            IconToggleButton(
                checked = open,
                onCheckedChange = { onClick() },
                modifier = Modifier.focusProperties { canFocus = false },
                colors = IconButtonDefaults.iconToggleButtonColors(
                    contentColor = TwitchTextSecondary,
                    checkedContentColor = TwitchPurple,
                ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_chat_history),
                    contentDescription = stringResource(R.string.chat_history),
                )
            }
        }
    }
}

/** Over the field: Log in when logged out, or the rules, which leave the picker button to open them and then it. */
@Composable
private fun BoxScope.ChatFieldCoverTarget(
    cover: ChatFieldCover,
    input: ChatInput,
    field: ChatFieldState,
    actions: ChatInputActions,
) {
    when (cover) {
        ChatFieldCover.LogIn -> {
            val loginLabel = stringResource(R.string.chat_input_logged_out)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .semantics { contentDescription = loginLabel }
                    .clickable(role = Role.Button, onClick = input.user.onLogIn),
            )
        }
        ChatFieldCover.Rules -> {
            val fieldLabel = stringResource(R.string.chat_input_hint)
            val pickerLabel = stringResource(R.string.emote_picker_open)
            val pickerInsetPx = (field.rightPx - field.pickerButtonLeftPx).coerceAtLeast(0f)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(end = with(LocalDensity.current) { pickerInsetPx.toDp() })
                    .semantics {
                        contentDescription = fieldLabel
                        // The cleared field hides the picker button from TalkBack.
                        customActions = listOf(
                            CustomAccessibilityAction(pickerLabel) {
                                actions.openRules(ChatRulesTrigger.EmotePicker)
                                true
                            },
                        )
                    }
                    .clickable(role = Role.Button) { actions.openRules(ChatRulesTrigger.MessageField) },
            )
        }
        ChatFieldCover.None -> Unit
    }
}
