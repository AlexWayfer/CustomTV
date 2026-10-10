package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedVisibility
import name.alexwayfer.customtv.ui.components.rememberLastNonNull
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.data.ChatSettings
import name.alexwayfer.customtv.ui.settings.animatedControlValue
import name.alexwayfer.customtv.BuildConfig
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.CHAT_MESSAGE_MAX_LENGTH
import name.alexwayfer.customtv.data.ChatSendError
import name.alexwayfer.customtv.data.ChatSendRepository
import name.alexwayfer.customtv.data.ChatSendResult
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatModePlaque
import name.alexwayfer.customtv.chat.ChatNick
import name.alexwayfer.customtv.chat.PickerEmote
import name.alexwayfer.customtv.chat.chatSendWarned
import name.alexwayfer.customtv.chat.SevenTvEmote
import name.alexwayfer.customtv.chat.emotePanelHeightPx
import name.alexwayfer.customtv.chat.emoteQueryAtCursor
import name.alexwayfer.customtv.chat.matchingPickerEmotes
import name.alexwayfer.customtv.chat.matchingSessionNicks
import name.alexwayfer.customtv.chat.nickQueryAtCursor
import name.alexwayfer.customtv.chat.pickerEmotesForCompletion
import name.alexwayfer.customtv.chat.sectionsWithFrequentlyUsed
import name.alexwayfer.customtv.chat.textWithCompletedEmote
import name.alexwayfer.customtv.chat.textWithCompletedNick
import name.alexwayfer.customtv.chat.textWithEmoteSearchColon
import name.alexwayfer.customtv.chat.textWithInsertedEmote
import name.alexwayfer.customtv.data.chatMessageToSend
import name.alexwayfer.customtv.data.chatSendError
import name.alexwayfer.customtv.data.chatRulesFingerprint
import name.alexwayfer.customtv.ui.theme.TwitchBg
import name.alexwayfer.customtv.ui.theme.TwitchHint
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.theme.TwitchSurfaceAlt
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

/** The input row's side padding: narrow at the end, where the end button's own padding makes up the rest. */
internal val CHAT_FIELD_START_PADDING = 10.dp
internal val CHAT_FIELD_END_PADDING = 2.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChatInputBar(
    channelLogin: String,
    browserSheetHeightPx: () -> Int,
    canSend: Boolean,
    onLogIn: () -> Unit,
    broadcasterId: String?,
    chatRules: List<String>?,
    senderId: String?,
    liveChatMessages: Flow<ChatMessage>,
    accessToken: suspend () -> String?,
    replyParentMessageId: String?,
    replyFocusRequest: OneShotRequest<Unit>?,
    /** Read only while a nick is typed or a command sent, so new chatters do not recompose the input. */
    sessionChatters: () -> Map<String, String>,
    channelOwner: ChatNick,
    chatSettings: ChatSettings?,
    onReadableColorsChange: (Boolean) -> Unit,
    onTimestampsChange: (Boolean) -> Unit,
    onTextSizeChange: (Int) -> Unit,
    onSmoothChatScrollChange: (Boolean) -> Unit,
    onRefreshEmotes: () -> Job,
    onRefreshLabels: () -> Job,
    signedInLogin: String?,
    signedInDisplayName: String?,
    /** The channel points button before the field; nothing in the free build. */
    channelPoints: @Composable (visible: Boolean) -> Unit,
    /** Opens the signed-in user's own chatter card from the chat menu; null when logged out. */
    onOpenOwnProfile: (() -> Unit)?,
    /** Opens the chatter card of a login typed after `/user`. */
    onOpenChatterCard: (login: String) -> Unit,
    /** What Send posts instead of a plain message, such as a watch streak; null for plain chat. */
    shareOffer: ChatShareOffer?,
    draft: TextFieldState,
    sevenTvEnabled: Boolean,
    bttvEnabled: Boolean,
    ffzEnabled: Boolean,
    emoteCompletionWithoutColon: Boolean,
    keepKeyboardAfterSend: Boolean,
    sevenTvEmotes: Map<String, SevenTvEmote>,
    bttvEmotes: Map<String, SevenTvEmote>,
    ffzEmotes: Map<String, SevenTvEmote>,
    onSent: () -> Unit,
    plaques: List<ChatModePlaque>,
    slowSendBlocked: Boolean,
    onFollow: () -> Unit,
    onSubscribe: () -> Unit,
    onComposerOpenChange: (Boolean) -> Unit,
    /** The field sits in the narrow chat beside a full screen player. */
    fullscreen: Boolean,
    /** The chat lies over the video, so the closed bar lets it show through. */
    overVideo: Boolean,
    /** The emote picker's height while it takes the keyboard's place; 0 otherwise. */
    onPickerPanelPx: (Int) -> Unit,
) {
    val repository = remember { ChatSendRepository(BuildConfig.TWITCH_CLIENT_ID) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val imeDismissFocus = LocalImeDismissFocus.current
    val readToken = rememberUpdatedState(accessToken)
    val readBroadcasterId = rememberUpdatedState(broadcasterId)
    val readSenderId = rememberUpdatedState(senderId)
    val readReplyParentId = rememberUpdatedState(replyParentMessageId)
    var menuOpen by remember { mutableStateOf(false) }
    var pickerOpen by remember(channelLogin) { mutableStateOf(false) }
    var keyboardReturning by remember(channelLogin) { mutableStateOf(false) }
    val fieldPresses = remember { MutableInteractionSource() }
    LaunchedEffect(fieldPresses) {
        fieldPresses.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press && pickerOpen) {
                // The tap brings the keyboard up in the picker's place.
                pickerOpen = false
                keyboardReturning = true
            }
        }
    }
    var fieldFocused by remember { mutableStateOf(false) }
    val rulesGate = rememberChatRulesGate(broadcasterId)
    val rulesFingerprint = remember(chatRules) { chatRules?.let(::chatRulesFingerprint) }
    val rulesNeedConfirmation = rulesGate.needsConfirmation(canSend, chatRules, rulesFingerprint)
    var rulesOpen by remember(channelLogin) { mutableStateOf(false) }
    var rulesTrigger by remember(channelLogin) { mutableStateOf(ChatRulesTrigger.MessageField) }
    var focusAfterRules by remember(channelLogin) { mutableStateOf(false) }
    // Window x of the field's right edge and of the emote picker button, so the cover that opens
    // the rules leaves the button to open the rules and then the picker.
    var fieldRightPx by remember { mutableFloatStateOf(0f) }
    var pickerButtonLeftPx by remember { mutableFloatStateOf(0f) }
    val draftLimit = remember { ChatDraftLimit(CHAT_MESSAGE_MAX_LENGTH) }
    val fieldScroll = rememberScrollState()
    var fieldTextLayout by remember { mutableStateOf<(() -> TextLayoutResult?)?>(null) }
    val fieldFocus = remember { FocusRequester() }
    val keyboardHeight = rememberKeyboardHeight()
    val density = LocalDensity.current
    val imePx = WindowInsets.ime.getBottom(density)
    val imeTargetPx = WindowInsets.imeAnimationTarget.getBottom(density)
    val navigationPx = WindowInsets.navigationBars.getBottom(density)
    val rememberedImePx = keyboardHeight.heightPx()
    if (keyboardReturning && keyboardReturnFinished(imePx, imeTargetPx)) keyboardReturning = false
    val imeOpen = imePx > 0
    BackHandler(enabled = pickerOpen || (fieldFocused && !imeOpen)) {
        if (pickerOpen) pickerOpen = false else focusManager.clearFocus(force = true)
    }
    DismissWhenPlayerContentHidden {
        pickerOpen = false
        if (fieldFocused) focusManager.clearFocus(force = true)
    }
    val panelPx = emotePanelHeightPx(
        navigationBarPx = navigationPx,
        imeBottomPx = imePx,
        pickerOpen = pickerOpen,
        keyboardReturning = keyboardReturning,
        rememberedImePx = rememberedImePx,
        fallbackPx = with(density) { 280.dp.roundToPx() },
    )
    val pickerPanelPx = if (pickerOpen || keyboardReturning) panelPx else 0
    val currentOnPickerPanelPx by rememberUpdatedState(onPickerPanelPx)
    LaunchedEffect(pickerPanelPx) {
        currentOnPickerPanelPx(pickerPanelPx)
    }
    LaunchedEffect(canSend) {
        if (!canSend) pickerOpen = false
    }
    val composerOpen = chatComposerOpen(imeTargetPx, pickerOpen, keyboardReturning)
    val onComposerOpenChangeState = rememberUpdatedState(onComposerOpenChange)
    LaunchedEffect(composerOpen) { onComposerOpenChangeState.value(composerOpen) }
    var sending by remember(channelLogin) { mutableStateOf(false) }
    var sendError by remember(channelLogin) { mutableStateOf<ChatSendError?>(null) }
    val warningGate = rememberChatWarningGate(broadcasterId, senderId)
    val warned = warningGate.warning != null
    // The plaque takes the field's place, so the keyboard and the picker go with the field.
    LaunchedEffect(warned) {
        if (!warned) return@LaunchedEffect
        pickerOpen = false
        focusManager.clearFocus(force = true)
    }
    LaunchedEffect(draft) {
        snapshotFlow { draft.text.toString() to draft.selection }
            .drop(1)
            .collect { sendError = null }
    }
    val draftText = draft.text.toString()
    val outgoing = chatTextToSubmit(draftText, emptyAllowed = shareOffer?.takeIf { it.composing }?.emptyAllowed == true)
    val pickerCatalog = rememberEmotePickerSections(
        enabled = canSend,
        channelId = broadcasterId,
        channelLabel = channelOwner.displayName.ifBlank { channelLogin },
        ownUserId = senderId,
        ownLabel = signedInDisplayName?.ifBlank { null } ?: signedInLogin,
        sevenTvEnabled = sevenTvEnabled,
        bttvEnabled = bttvEnabled,
        ffzEnabled = ffzEnabled,
        sevenTvEmotes = sevenTvEmotes,
        bttvEmotes = bttvEmotes,
        ffzEmotes = ffzEmotes,
        accessToken = accessToken,
    )
    val completableEmotes = remember(pickerCatalog.sections) {
        pickerEmotesForCompletion(pickerCatalog.sections)
    }
    val draftEmoteNames = remember(pickerCatalog.sections) {
        pickerCatalog.sections.flatMapTo(HashSet()) { section -> section.emotes.map { it.name } }
    }
    // The field and its plaques read the current rule from this state, so emotes that load after
    // the text was typed get highlighted without replacing the field's output transformation.
    val currentDraftEmoteCode = rememberUpdatedState(
        remember(draftEmoteNames, bttvEnabled, ffzEnabled) {
            { token: String -> chatDraftIsEmoteCode(token, draftEmoteNames, bttvEnabled, ffzEnabled) }
        },
    )
    val isDraftEmoteCode = remember { { token: String -> currentDraftEmoteCode.value(token) } }
    val draftEmoteGaps = remember { ChatDraftEmoteGaps(isDraftEmoteCode) }
    val emoteUsage = rememberEmoteUsage(
        channelId = broadcasterId,
        ownUserId = senderId,
        liveMessages = liveChatMessages,
        emotes = completableEmotes,
    )
    val commandMatches = chatCommandQueryAtCursor(draftText, draft.selection.end)
        ?.takeIf { canSend }
        ?.let { matchingChatCommands(it.text) }
        .orEmpty()
    val nickQuery = nickQueryAtCursor(draftText, draft.selection.end)
    // Matching sorts every candidate, so it runs again only when the query or the candidates change.
    val nickQueryText = nickQuery?.text?.takeIf { canSend }
    // The chatters are read only with a nick query, so a busy chat does not recompose the input otherwise.
    val chatters = if (nickQueryText == null) null else sessionChatters()
    val nickMatches = remember(nickQueryText, chatters, channelOwner) {
        if (nickQueryText == null || chatters == null) {
            emptyList()
        } else {
            matchingSessionNicks(chatters, nickQueryText, channelOwner)
        }
    }
    val emoteQuery = emoteQueryAtCursor(
        draftText,
        draft.selection.end,
        withoutColon = emoteCompletionWithoutColon,
    )
    val emoteQueryText = emoteQuery?.text?.takeIf { canSend && !pickerOpen }
    val emoteMatches = remember(emoteQueryText, completableEmotes, emoteUsage) {
        if (emoteQueryText == null) emptyList() else matchingPickerEmotes(completableEmotes, emoteQueryText, emoteUsage)
    }
    fun replaceDraft(text: String, cursor: Int) {
        draft.edit {
            replace(0, length, text)
            selection = TextRange(cursor)
        }
    }
    // The lists keep rows built earlier with an older ::pickEmote, whose captured values are stale.
    // Read the draft and settings at the tap instead, so text and cursor always come from one state.
    val completeWithoutColon by rememberUpdatedState(emoteCompletionWithoutColon)
    fun pickEmote(emote: PickerEmote) {
        val inserted = textWithCompletedEmote(
            draft.text.toString(),
            draft.selection.end,
            emote.name,
            CHAT_MESSAGE_MAX_LENGTH,
            withoutColon = completeWithoutColon,
        ) ?: return
        replaceDraft(inserted.text, inserted.cursor)
    }
    fun pickCommand(command: ChatCommand) {
        val completed = textWithCompletedCommand(
            draft.text.toString(),
            draft.selection.end,
            command,
            CHAT_MESSAGE_MAX_LENGTH,
        ) ?: return
        replaceDraft(completed.text, completed.cursor)
    }
    fun pickNick(nick: ChatNick) {
        val completed = textWithCompletedNick(
            draft.text.toString(),
            draft.selection.end,
            nick.displayName,
            CHAT_MESSAGE_MAX_LENGTH,
        ) ?: return
        replaceDraft(completed.text, completed.cursor)
    }
    fun insertEmote(name: String) {
        val inserted = textWithInsertedEmote(
            draft.text.toString(),
            draft.selection.start,
            draft.selection.end,
            name,
            CHAT_MESSAGE_MAX_LENGTH,
        ) ?: return
        replaceDraft(inserted.text, inserted.cursor)
    }
    // The picker hands its search to the `:` completion, which shows matches above the keyboard.
    fun searchEmotes() {
        textWithEmoteSearchColon(
            draft.text.toString(),
            draft.selection.start,
            draft.selection.end,
            CHAT_MESSAGE_MAX_LENGTH,
        )?.let { replaceDraft(it.text, it.cursor) }
        pickerOpen = false
        keyboardReturning = true
        fieldFocus.requestFocus()
        keyboard?.show()
    }
    val sheetKeyboard = LocalSheetKeyboard.current
    fun openRules(trigger: ChatRulesTrigger) {
        rulesTrigger = trigger
        pickerOpen = false
        sheetKeyboard.open { rulesOpen = true }
    }
    fun togglePicker() {
        if (!canSend) return
        if (!pickerOpen && rulesNeedConfirmation) {
            openRules(ChatRulesTrigger.EmotePicker)
            return
        }
        if (pickerOpen) {
            pickerOpen = false
            keyboardReturning = true
            fieldFocus.requestFocus()
            keyboard?.show()
            return
        }
        if (imeOpen) imeDismissFocus.keepFocus = true
        keyboard?.hide()
        // Heights of a keyboard the user has since switched away from would size the picker wrong.
        keyboardHeight.useCurrentKeyboard()
        pickerOpen = true
    }
    val onSentState = rememberUpdatedState(onSent)
    val readShareOffer = rememberUpdatedState(shareOffer)
    val readKeepKeyboard = rememberUpdatedState(keepKeyboardAfterSend)
    val readChatters = rememberUpdatedState(sessionChatters)
    val openChatterCardState = rememberUpdatedState(onOpenChatterCard)
    fun submit() {
        // `/user` posts nothing, so neither the rules nor slow mode hold it.
        when (val command = chatUserCommand(draft.text.toString(), readChatters.value())) {
            is ChatUserCommand.Open -> {
                draft.clearText()
                openChatterCardState.value(command.login)
                return
            }
            ChatUserCommand.Invalid -> return
            null -> Unit
        }
        val sharing = readShareOffer.value?.takeIf { it.composing }
        val message = chatTextToSubmit(draft.text.toString(), emptyAllowed = sharing?.emptyAllowed == true) ?: return
        if (slowSendBlocked || !canSend || sending) return
        // Rules that arrived after the field was already focused still come before the first message.
        if (rulesNeedConfirmation) {
            openRules(ChatRulesTrigger.Send)
            return
        }
        sending = true
        sendError = null
        if (chatSendHidesKeyboard(readKeepKeyboard.value)) {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        }
        scope.launch {
            if (sharing != null) {
                val error = sharing.share(message)
                sending = false
                if (error == null) {
                    draft.clearText()
                    onSentState.value()
                } else {
                    sendError = error
                }
                return@launch
            }
            val token = readToken.value()?.takeIf { it.isNotBlank() }
            val channelId = readBroadcasterId.value?.takeIf { it.isNotBlank() }
            val userId = readSenderId.value?.takeIf { it.isNotBlank() }
            val result = if (token == null || userId == null) {
                ChatSendResult.Rejected(401, "")
            } else if (channelId == null) {
                ChatSendResult.Unavailable
            } else {
                repository.send(
                    token,
                    channelId,
                    userId,
                    message,
                    readReplyParentId.value,
                )
            }
            sending = false
            val draftMatches = chatMessageToSend(draft.text.toString()) == message
            if (chatSendReturnsToLatest(result, draftMatches)) {
                readShareOffer.value?.onPlainSent?.invoke()
                draft.clearText()
                onSentState.value()
            } else if (chatSendWarned(result)) {
                // The plaque says it, and the draft stays for after the warning is acknowledged.
                warningGate.sendRefused()
            } else {
                sendError = chatSendError(result)
            }
        }
    }
    // Runs after the confirmed rules removed the tap cover, so the field's focus requester is attached.
    LaunchedEffect(focusAfterRules) {
        if (!focusAfterRules) return@LaunchedEffect
        // A warning that came meanwhile took the field away, and its focus requester with it.
        if (!warned) {
            fieldFocus.requestFocus()
            keyboard?.show()
        }
        focusAfterRules = false
    }
    // An offer's text, such as an unlocked emote, goes in the field once, before the field asks for the keyboard.
    LaunchedEffect(shareOffer?.prefill) {
        val shared = shareOffer?.prefill?.take() ?: return@LaunchedEffect
        draft.setTextAndPlaceCursorAtEnd(draftWithShared(draft.text.toString(), shared))
    }
    // Choosing Reply asks for the field once per choice, not again when the bar returns from picture-in-picture.
    // Choosing Share asks for the field once, like Reply.
    LaunchedEffect(shareOffer?.focusRequest) {
        shareOffer?.focusRequest?.take() ?: return@LaunchedEffect
        when (chatReplyComposer(canSend, warned, rulesNeedConfirmation)) {
            ChatReplyComposer.Keyboard -> {
                fieldFocus.requestFocus()
                keyboard?.show()
            }
            ChatReplyComposer.Rules -> openRules(ChatRulesTrigger.MessageField)
            ChatReplyComposer.Nothing -> Unit
        }
    }
    LaunchedEffect(replyFocusRequest) {
        replyFocusRequest?.take() ?: return@LaunchedEffect
        when (chatReplyComposer(canSend, warned, rulesNeedConfirmation)) {
            ChatReplyComposer.Keyboard -> {
                fieldFocus.requestFocus()
                keyboard?.show()
            }
            ChatReplyComposer.Rules -> openRules(ChatRulesTrigger.MessageField)
            ChatReplyComposer.Nothing -> Unit
        }
    }
    val completion = chatCompletion(commandMatches, nickMatches, emoteMatches).takeUnless { warned }
    BarWithOverlayAbove(
        overlay = {
            ChatCompletionPanel(
                completion = completion,
                onPickCommand = ::pickCommand,
                onPickNick = ::pickNick,
                onPickEmote = ::pickEmote,
            )
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
    val barAlpha by animateFloatAsState(
        targetValue = chatInputBarAlpha(overVideo, composerOpen),
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
        val shownOffer = rememberLastNonNull(shareOffer)
        AnimatedVisibility(
            visible = shareOffer != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            shownOffer?.note?.invoke()
        }
        ChatFieldNotes(
            plaques = plaques,
            signedIn = canSend,
            sendError = sendError,
            onFollow = onFollow,
            onSubscribe = onSubscribe,
        )
        val context = LocalContext.current
        val shownWarning = rememberLastNonNull(warningGate.warning)
        AnimatedVisibility(
            visible = warned,
            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
        ) {
            shownWarning?.let { warning ->
                ChatWarningPlaque(
                    gate = warningGate,
                    warning = warning,
                    // As Open chat in browser in the chat menu: a sheet up to the player.
                    onOpenInBrowser = { openTwitchChatInBrowser(context, channelLogin, browserSheetHeightPx()) },
                    modifier = Modifier.padding(end = CHAT_FIELD_START_PADDING - CHAT_FIELD_END_PADDING),
                )
            }
        }
        val sideButtons = chatFieldSideButtons(composerOpen, hasMessage = outgoing != null, sending = sending, fullscreen = fullscreen)
        // A warning to acknowledge first takes the field's place, as on the Twitch web.
        AnimatedVisibility(
            visible = !warned,
            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
        ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (canSend) channelPoints(sideButtons.start)
            val loggedOutOpensLogin = loggedOutChatFieldOpensLogin(canSend)
            val tapOpensRules = !fieldFocused && rulesNeedConfirmation
            Box(
                modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned { fieldRightPx = it.boundsInWindow().right },
            ) {
            val fieldColors = TextFieldDefaults.colors(
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
            val materialDecorator = TextFieldDefaults.decorator(
                state = draft,
                enabled = canSend,
                lineLimits = TextFieldLineLimits.SingleLine,
                outputTransformation = draftEmoteGaps,
                interactionSource = fieldPresses,
                placeholder = {
                    Text(
                        stringResource(
                            if (canSend) R.string.chat_input_hint else R.string.chat_input_logged_out,
                        ),
                        color = TwitchHint,
                    )
                },
                trailingIcon = if (canSend) {
                    {
                        IconButton(
                            onClick = { togglePicker() },
                            modifier = Modifier.onGloballyPositioned {
                                pickerButtonLeftPx = it.boundsInWindow().left
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_emote_picker),
                                contentDescription = stringResource(R.string.emote_picker_open),
                                tint = if (pickerOpen) TwitchPurple else TwitchTextSecondary,
                            )
                        }
                    }
                } else {
                    null
                },
                colors = fieldColors,
                container = {
                    TextFieldDefaults.Container(
                        enabled = canSend,
                        isError = false,
                        interactionSource = fieldPresses,
                        colors = fieldColors,
                        shape = RoundedCornerShape(8.dp),
                    )
                },
            )
            CompositionLocalProvider(LocalTextSelectionColors provides fieldColors.textSelectionColors) {
                BasicTextField(
                    state = draft,
                    enabled = canSend,
                    inputTransformation = draftLimit,
                    outputTransformation = draftEmoteGaps,
                    lineLimits = TextFieldLineLimits.SingleLine,
                    interactionSource = fieldPresses,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    onKeyboardAction = { submit() },
                    textStyle = LocalTextStyle.current.merge(TextStyle(color = TwitchText)),
                    cursorBrush = SolidColor(TwitchPurple),
                    onTextLayout = { getResult -> fieldTextLayout = getResult },
                    scrollState = fieldScroll,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(
                            minWidth = TextFieldDefaults.MinWidth,
                            minHeight = TextFieldDefaults.MinHeight,
                        )
                        .then(
                            if (loggedOutOpensLogin || tapOpensRules) {
                                Modifier.clearAndSetSemantics {}
                            } else {
                                Modifier
                                    .focusRequester(fieldFocus)
                                    .onFocusChanged { focus ->
                                        fieldFocused = focus.isFocused
                                        if (focus.isFocused) pickerOpen = false
                                        // Back or a hidden player drops the field: nothing is rising any more.
                                        if (!focus.isFocused) keyboardReturning = false
                                    }
                            },
                        ),
                    decorator = TextFieldDecorator { innerTextField ->
                        materialDecorator.Decoration {
                            Box(
                                modifier = Modifier.chatDraftEmotePlaques(
                                    isEmoteCode = isDraftEmoteCode,
                                    textLayout = { fieldTextLayout?.invoke() },
                                    scrollState = fieldScroll,
                                ),
                            ) {
                                innerTextField()
                            }
                        }
                    },
                )
            }
            if (loggedOutOpensLogin) {
                val loginLabel = stringResource(R.string.chat_input_logged_out)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .semantics { contentDescription = loginLabel }
                        .clickable(role = Role.Button, onClick = onLogIn),
                )
            } else if (tapOpensRules) {
                val fieldLabel = stringResource(R.string.chat_input_hint)
                val pickerLabel = stringResource(R.string.emote_picker_open)
                val pickerInsetPx = (fieldRightPx - pickerButtonLeftPx).coerceAtLeast(0f)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(end = with(density) { pickerInsetPx.toDp() })
                        .semantics {
                            contentDescription = fieldLabel
                            // The cleared field hides the picker button from TalkBack.
                            customActions = listOf(
                                CustomAccessibilityAction(pickerLabel) {
                                    openRules(ChatRulesTrigger.EmotePicker)
                                    true
                                },
                            )
                        }
                        .clickable(role = Role.Button) { openRules(ChatRulesTrigger.MessageField) },
                )
            }
            }
            AnimatedVisibility(
                visible = sideButtons.endButton,
                enter = expandHorizontally(expandFrom = Alignment.End) + slideInHorizontally { it },
                exit = shrinkHorizontally(shrinkTowards = Alignment.End) + slideOutHorizontally { it },
            ) {
            IconButton(
                onClick = {
                    if (sending) return@IconButton
                    if (outgoing != null) {
                        submit()
                        return@IconButton
                    }
                    if (menuOpen) return@IconButton
                    pickerOpen = false
                    sheetKeyboard.open { menuOpen = true }
                },
            ) {
                ChatFieldEndIconContent(chatFieldEndIcon(sending, hasMessage = outgoing != null))
            }
            }
            // Without the end button the row's narrow end padding would put the field at the edge;
            // this makes up the rest of the start padding.
            val endGap by animateDpAsState(if (sideButtons.endButton) 0.dp else CHAT_FIELD_START_PADDING - CHAT_FIELD_END_PADDING, label = "chat field end gap")
            Spacer(Modifier.width(endGap))
        }
        }
        }
        if (panelPx > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { panelPx.toDp() })
                    .background(TwitchBg),
            ) {
                if (pickerOpen) {
                    EmotePickerPanel(
                        catalog = pickerCatalog.copy(
                            sections = sectionsWithFrequentlyUsed(pickerCatalog.sections, emoteUsage),
                        ),
                        onPick = ::insertEmote,
                        onSearch = ::searchEmotes,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = with(density) { navigationPx.toDp() }),
                    )
                }
            }
        }
    }
    }
    if (rulesOpen && chatRules != null) {
        ChatRulesSheet(
            channelLogin = channelLogin,
            channelName = channelOwner.displayName,
            rules = chatRules,
            onConfirm = if (rulesNeedConfirmation) {
                {
                    rulesOpen = false
                    rulesGate.acknowledge(scope, rulesFingerprint)
                    when (chatRulesFollowUp(rulesTrigger)) {
                        ChatRulesFollowUp.EmotePicker -> {
                            keyboardHeight.useCurrentKeyboard()
                            pickerOpen = true
                        }
                        ChatRulesFollowUp.Keyboard -> focusAfterRules = true
                        ChatRulesFollowUp.Nothing -> Unit
                    }
                }
            } else {
                null
            },
            onDismiss = { rulesOpen = false },
        )
    }
    val sheetSettings = animatedControlValue(chatSettings)
    if (menuOpen && sheetSettings != null) {
        ChatSettingsSheet(
            channelLogin = channelLogin,
            browserSheetHeightPx = browserSheetHeightPx,
            readableColors = sheetSettings.readableColors,
            timestamps = sheetSettings.timestamps,
            textSize = sheetSettings.textSize,
            smoothChatScroll = sheetSettings.smoothChatScroll,
            onReadableColorsChange = onReadableColorsChange,
            onTimestampsChange = onTimestampsChange,
            onTextSizeChange = onTextSizeChange,
            onSmoothChatScrollChange = onSmoothChatScrollChange,
            onRefreshEmotes = onRefreshEmotes,
            onRefreshLabels = onRefreshLabels,
            onOpenOwnProfile = onOpenOwnProfile?.let { open ->
                {
                    menuOpen = false
                    open()
                }
            },
            onViewChatRules = if (chatRules.isNullOrEmpty()) {
                null
            } else {
                {
                    menuOpen = false
                    rulesTrigger = ChatRulesTrigger.Settings
                    rulesOpen = true
                }
            },
            onDismiss = {
                menuOpen = false
                sheetKeyboard.onDismiss()
            },
        )
    }
}
