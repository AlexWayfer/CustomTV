package name.alexwayfer.customtv.ui.watch

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.drop
import name.alexwayfer.customtv.BuildConfig
import name.alexwayfer.customtv.chat.sectionsWithFrequentlyUsed
import name.alexwayfer.customtv.chat.EmoteUse
import name.alexwayfer.customtv.data.ChatSendError
import name.alexwayfer.customtv.data.ChatSendRepository
import name.alexwayfer.customtv.data.chatRulesFingerprint
import name.alexwayfer.customtv.ui.components.rememberLastNonNull
import name.alexwayfer.customtv.ui.settings.animatedControlValue
import name.alexwayfer.customtv.ui.theme.TwitchBg

/** The input row's side padding: narrow at the end, where the end button's own padding makes up the rest. */
internal val CHAT_FIELD_START_PADDING = 10.dp
internal val CHAT_FIELD_END_PADDING = 2.dp

@Composable
internal fun ChatInputBar(
    channel: ChatInputChannel,
    user: ChatInputUser,
    compose: ChatCompose,
    menu: ChatMenu,
    bar: ChatInputBarLook,
) {
    val composer = remember(channel.login) { ChatComposerState() }
    val field = rememberChatFieldState()
    val scope = rememberCoroutineScope()
    val keyboardHeight = rememberKeyboardHeight()
    val panel = chatComposerPanel(composer, keyboardHeight)
    val rulesGate = rememberChatRulesGate(channel.broadcasterId)
    val rulesFingerprint = remember(channel.rules) { channel.rules?.let(::chatRulesFingerprint) }
    val warningGate = rememberChatWarningGate(channel.broadcasterId, user.userId)
    val input = ChatInput(
        channel = channel,
        user = user,
        compose = compose,
        menu = menu,
        bar = bar,
        rulesGate = rulesGate,
        rulesFingerprint = rulesFingerprint,
        rulesNeedConfirmation = rulesGate.needsConfirmation(user.canSend, channel.rules, rulesFingerprint),
        warningGate = warningGate,
        warned = warningGate.warning != null,
        imeOpen = panel.imeOpen,
        keyboard = LocalSoftwareKeyboardController.current,
        focusManager = LocalFocusManager.current,
        sheetKeyboard = LocalSheetKeyboard.current,
        imeDismissFocus = LocalImeDismissFocus.current,
        keyboardHeight = keyboardHeight,
        repository = remember { ChatSendRepository(BuildConfig.TWITCH_CLIENT_ID) },
    )
    val current = rememberUpdatedState(input)
    val actions = remember(composer) { ChatInputActions(composer, field, ChatDraftEditor(current), scope, current) }
    ChatComposerEffects(input, composer, field, panel)
    ChatFieldRequestEffects(input, composer, actions)
    ChatInputBarBody(input, composer, field, panel, actions)
    if (composer.rulesOpen) ChatInputRulesSheet(input, actions, onDismiss = { composer.rulesOpen = false })
    ChatInputMenuSheet(input, composer, field)
}

/** How the picker, the keyboard, and a warning hand the room under the field to one another. */
@Composable
private fun ChatComposerEffects(
    input: ChatInput,
    composer: ChatComposerState,
    field: ChatFieldState,
    panel: ChatComposerPanel,
) {
    LaunchedEffect(field.presses) {
        field.presses.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press && composer.pickerOpen) {
                // The tap brings the keyboard up in the picker's place.
                composer.pickerOpen = false
                composer.keyboardReturning = true
            }
        }
    }
    val focusManager = input.focusManager
    BackHandler(enabled = composer.pickerOpen || (field.focused && !panel.imeOpen)) {
        if (composer.pickerOpen) composer.pickerOpen = false else focusManager.clearFocus(force = true)
    }
    DismissWhenPlayerContentHidden {
        composer.pickerOpen = false
        if (field.focused) focusManager.clearFocus(force = true)
    }
    val pickerPanelPx = if (composer.pickerOpen || composer.keyboardReturning) panel.panelPx else 0
    val currentOnPickerPanelPx by rememberUpdatedState(input.bar.onPickerPanelPx)
    LaunchedEffect(pickerPanelPx) { currentOnPickerPanelPx(pickerPanelPx) }
    val canSend = input.user.canSend
    LaunchedEffect(canSend) { if (!canSend) composer.pickerOpen = false }
    val onComposerOpenChange = rememberUpdatedState(input.bar.onComposerOpenChange)
    LaunchedEffect(panel.composerOpen) { onComposerOpenChange.value(panel.composerOpen) }
    val warned = input.warned
    // The plaque takes the field's place, so the keyboard and the picker go with the field.
    LaunchedEffect(warned) {
        if (!warned) return@LaunchedEffect
        composer.pickerOpen = false
        focusManager.clearFocus(force = true)
    }
    val draft = input.compose.draft
    LaunchedEffect(draft) {
        snapshotFlow { draft.text.toString() to draft.selection }.drop(1).collect { composer.sendError = null }
    }
}

/** What asks for the field once: confirmed rules, a shared text, and Reply or Share. */
@Composable
private fun ChatFieldRequestEffects(input: ChatInput, composer: ChatComposerState, actions: ChatInputActions) {
    val warned = input.warned
    val draft = input.compose.draft
    // Runs after the confirmed rules removed the tap cover, so the field's focus requester is attached.
    LaunchedEffect(composer.focusAfterRules) {
        if (!composer.focusAfterRules) return@LaunchedEffect
        // A warning that came meanwhile took the field away, and its focus requester with it.
        if (!warned) actions.focusField()
        composer.focusAfterRules = false
    }
    val shareOffer = input.compose.shareOffer
    // An offer's text, such as an unlocked emote, goes in the field once, before the field asks for the keyboard.
    LaunchedEffect(shareOffer?.prefill) {
        val shared = shareOffer?.prefill?.take() ?: return@LaunchedEffect
        draft.setTextAndPlaceCursorAtEnd(draftWithShared(draft.text.toString(), shared))
    }
    // Choosing Reply asks for the field once per choice, not again when the bar returns from picture-in-picture.
    // Choosing Share asks for the field once, like Reply.
    LaunchedEffect(shareOffer?.focusRequest) {
        shareOffer?.focusRequest?.take() ?: return@LaunchedEffect
        actions.askForField()
    }
    val replyFocusRequest = input.compose.replyFocusRequest
    LaunchedEffect(replyFocusRequest) {
        replyFocusRequest?.take() ?: return@LaunchedEffect
        actions.askForField()
    }
}

@Composable
private fun ChatInputBarBody(
    input: ChatInput,
    composer: ChatComposerState,
    field: ChatFieldState,
    panel: ChatComposerPanel,
    actions: ChatInputActions,
) {
    val catalog = rememberChatPickerCatalog(input)
    val draftEmotes = rememberDraftEmotes(input, catalog.sections)
    val completion = rememberChatCompletion(input, composer.pickerOpen, draftEmotes)
    val editor = actions.editor
    BarWithOverlayAbove(
        overlay = {
            ChatCompletionPanel(
                completion = completion,
                onPickCommand = editor::pickCommand,
                onPickNick = editor::pickNick,
                onPickEmote = editor::pickEmote,
            )
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        val barAlpha by animateFloatAsState(
            targetValue = chatInputBarAlpha(input.bar.overVideo, panel.composerOpen),
            label = "chat input bar alpha",
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .blockClicks()
                .background(TwitchBg.copy(alpha = barAlpha)),
        ) {
            Column(
                modifier = Modifier.padding(
                    start = CHAT_FIELD_START_PADDING,
                    end = CHAT_FIELD_END_PADDING,
                    top = 8.dp,
                    bottom = 8.dp,
                ),
            ) {
                ChatFieldNotesArea(input, composer.sendError)
                val look = ChatFieldLook(chatFieldColors(barAlpha), draftEmotes, panel.composerOpen)
                ChatFieldRow(input, composer, field, actions, look)
            }
            if (panel.panelPx > 0) ChatEmotePanel(panel, composer.pickerOpen, catalog, draftEmotes.usage, actions)
        }
    }
}

/** Above the field: what Send shares, the chat's modes and send errors, and a warning to acknowledge. */
@Composable
private fun ColumnScope.ChatFieldNotesArea(input: ChatInput, sendError: ChatSendError?) {
    val shareOffer = input.compose.shareOffer
    val shownOffer = rememberLastNonNull(shareOffer)
    AnimatedVisibility(
        visible = shareOffer != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        shownOffer?.note?.invoke()
    }
    ChatFieldNotes(
        plaques = input.bar.plaques,
        signedIn = input.user.canSend,
        sendError = sendError,
        onFollow = input.bar.onFollow,
        onSubscribe = input.bar.onSubscribe,
    )
    val context = LocalContext.current
    val shownWarning = rememberLastNonNull(input.warningGate.warning)
    AnimatedVisibility(
        visible = input.warned,
        enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
    ) {
        shownWarning?.let { warning ->
            ChatWarningPlaque(
                gate = input.warningGate,
                warning = warning,
                // As Open chat in browser in the chat menu: a sheet up to the player.
                onOpenInBrowser = {
                    openTwitchChatInBrowser(context, input.channel.login, input.channel.browserSheetHeightPx())
                },
                modifier = Modifier.padding(end = CHAT_FIELD_START_PADDING - CHAT_FIELD_END_PADDING),
            )
        }
    }
}

/** The channel points button, the field, and Send or the chat menu after it. */
@Composable
private fun ColumnScope.ChatFieldRow(
    input: ChatInput,
    composer: ChatComposerState,
    field: ChatFieldState,
    actions: ChatInputActions,
    look: ChatFieldLook,
) {
    val sharing = input.compose.shareOffer?.takeIf { it.composing }
    val hasMessage = chatTextToSubmit(
        input.compose.draft.text.toString(),
        emptyAllowed = sharing?.emptyAllowed == true,
    ) != null
    val sideButtons = chatFieldSideButtons(
        look.composerOpen,
        hasMessage = hasMessage,
        sending = composer.sending,
        fullscreen = input.bar.fullscreen,
    )
    // A warning to acknowledge first takes the field's place, as on the Twitch web.
    AnimatedVisibility(
        visible = !input.warned,
        enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
    ) {
        // The icon buttons keep their touch targets without holding the low field's row at the 48 dp they take.
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (input.user.canSend) input.bar.channelPoints(sideButtons.start)
                ChatMessageField(input, composer, field, actions, look)
                ChatFieldEnd(
                    visible = sideButtons.endButton,
                    icon = chatFieldEndIcon(composer.sending, hasMessage = hasMessage),
                    onClick = { actions.tapEndButton(hasMessage) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.ChatFieldEnd(visible: Boolean, icon: ChatFieldEndIcon, onClick: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = expandHorizontally(expandFrom = Alignment.End) + slideInHorizontally { it },
        exit = shrinkHorizontally(shrinkTowards = Alignment.End) + slideOutHorizontally { it },
    ) {
        IconButton(onClick = onClick) {
            ChatFieldEndIconContent(icon)
        }
    }
    // Without the end button the row's narrow end padding would put the field at the edge;
    // this makes up the rest of the start padding.
    val endGap by animateDpAsState(
        if (visible) 0.dp else CHAT_FIELD_START_PADDING - CHAT_FIELD_END_PADDING,
        label = "chat field end gap",
    )
    Spacer(Modifier.width(endGap))
}

/** The room under the field, where the emote picker takes the keyboard's place. */
@Composable
private fun ChatEmotePanel(
    panel: ChatComposerPanel,
    pickerOpen: Boolean,
    catalog: EmotePickerCatalog,
    usage: List<EmoteUse>,
    actions: ChatInputActions,
) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(with(density) { panel.panelPx.toDp() })
            .background(TwitchBg),
    ) {
        if (pickerOpen) {
            EmotePickerPanel(
                catalog = catalog.copy(sections = sectionsWithFrequentlyUsed(catalog.sections, usage)),
                onPick = actions.editor::insertEmote,
                onSearch = actions::searchEmotes,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = with(density) { panel.navigationPx.toDp() }),
            )
        }
    }
}

@Composable
private fun ChatInputRulesSheet(input: ChatInput, actions: ChatInputActions, onDismiss: () -> Unit) {
    val rules = input.channel.rules ?: return
    ChatRulesSheet(
        channelLogin = input.channel.login,
        channelName = input.channel.owner.displayName,
        rules = rules,
        onConfirm = if (input.rulesNeedConfirmation) actions::confirmRules else null,
        onDismiss = onDismiss,
    )
}

@Composable
private fun ChatInputMenuSheet(input: ChatInput, composer: ChatComposerState, field: ChatFieldState) {
    val menu = input.menu
    val sheetSettings = animatedControlValue(menu.settings)
    if (!field.menuOpen || sheetSettings == null) return
    ChatSettingsSheet(
        channelLogin = input.channel.login,
        browserSheetHeightPx = input.channel.browserSheetHeightPx,
        readableColors = sheetSettings.readableColors,
        timestamps = sheetSettings.timestamps,
        textSize = sheetSettings.textSize,
        smoothChatScroll = sheetSettings.smoothChatScroll,
        onReadableColorsChange = menu.onReadableColorsChange,
        onTimestampsChange = menu.onTimestampsChange,
        onTextSizeChange = menu.onTextSizeChange,
        onSmoothChatScrollChange = menu.onSmoothChatScrollChange,
        onRefreshEmotes = menu.onRefreshEmotes,
        onRefreshLabels = menu.onRefreshLabels,
        onOpenOwnProfile = menu.onOpenOwnProfile?.let { open ->
            {
                field.menuOpen = false
                open()
            }
        },
        onViewChatRules = if (input.channel.rules.isNullOrEmpty()) {
            null
        } else {
            {
                field.menuOpen = false
                composer.rulesTrigger = ChatRulesTrigger.Settings
                composer.rulesOpen = true
            }
        },
        onDismiss = {
            field.menuOpen = false
            input.sheetKeyboard.onDismiss()
        },
    )
}
