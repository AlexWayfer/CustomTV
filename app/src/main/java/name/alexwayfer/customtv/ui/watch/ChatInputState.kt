package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.chat.emotePanelHeightPx
import name.alexwayfer.customtv.chat.emotePickerHeightPx
import name.alexwayfer.customtv.data.CHAT_MESSAGE_MAX_LENGTH
import name.alexwayfer.customtv.data.ChatSendError
import name.alexwayfer.customtv.data.ChatSendRepository

/** What the bar keeps for the open channel: the picker, the rules sheet, and the message being sent. */
@Stable
internal class ChatComposerState {
    var pickerOpen by mutableStateOf(false)

    /** The keyboard rises in the picker's place, which keeps its room until the keyboard has it. */
    var keyboardReturning by mutableStateOf(false)
    var rulesOpen by mutableStateOf(false)
    var rulesTrigger by mutableStateOf(ChatRulesTrigger.MessageField)
    var focusAfterRules by mutableStateOf(false)
    var recapOpen by mutableStateOf(false)

    /** The plaque with the user's own messages is open over the field, at [historyShownId] or at the newest one. */
    var historyOpen by mutableStateOf(false)
    var historyShownId by mutableStateOf<String?>(null)

    /** What Up and Down on a hardware keyboard have put in the field, and the draft to bring back. */
    var historyRecall = ChatHistoryRecall()
    var sending by mutableStateOf(false)
        private set

    /** The send has run for [CHAT_SEND_CANCEL_DELAY], so a tap on its progress cancels it. */
    var sendCancelable by mutableStateOf(false)
        private set
    var sendError by mutableStateOf<ChatSendError?>(null)
    private var sendJob: Job? = null

    /** Runs [send] in [scope] as the send under way, which the end button can cancel once it takes a while. */
    fun startSend(scope: CoroutineScope, send: suspend () -> Unit) {
        sending = true
        sendCancelable = false
        val job = scope.launch(start = CoroutineStart.LAZY) {
            val unlock = launch {
                delay(CHAT_SEND_CANCEL_DELAY)
                sendCancelable = true
            }
            try {
                send()
            } finally {
                unlock.cancel()
                // A cancelled send may finish after the next one has started; that one keeps its progress.
                if (sendJob === coroutineContext.job) endSend()
            }
        }
        sendJob = job
        job.start()
    }

    /** Stops the send under way; the draft stays in the field. */
    fun cancelSend() {
        sendJob?.cancel()
        endSend()
    }

    private fun endSend() {
        sending = false
        sendCancelable = false
        sendJob = null
    }
}

@Composable
internal fun rememberChatFieldState(): ChatFieldState {
    val scroll = rememberScrollState()
    return remember { ChatFieldState(scroll) }
}

/** The message field, kept across channels: its focus, its text, and the chat menu behind its end button. */
@Stable
internal class ChatFieldState(val scroll: ScrollState) {
    var menuOpen by mutableStateOf(false)

    /** The menu opens at the chat modes page, as a tap on their plaque asks, instead of at its rows. */
    var menuAtModes by mutableStateOf(false)
    var focused by mutableStateOf(false)
    val focus = FocusRequester()
    val presses = MutableInteractionSource()
    val limit = ChatDraftLimit(CHAT_MESSAGE_MAX_LENGTH)
    var textLayout by mutableStateOf<(() -> TextLayoutResult?)?>(null)

    // Window x of the field's right edge and of the emote picker button, so the cover that opens
    // the rules leaves the button to open the rules and then the picker.
    var rightPx by mutableFloatStateOf(0f)
    var pickerButtonLeftPx by mutableFloatStateOf(0f)

    fun onFocusChanged(focused: Boolean, composer: ChatComposerState) {
        this.focused = focused
        if (focused) composer.pickerOpen = false
        // Back or a hidden player drops the field: nothing is rising any more.
        if (!focused) composer.keyboardReturning = false
    }
}

/** How the keyboard and the emote picker share the room under the field. */
internal data class ChatComposerPanel(
    /** The room the picker or the rising keyboard takes under the field; 0 otherwise. */
    val panelPx: Int,
    val navigationPx: Int,
    val imeOpen: Boolean,
    /** The keyboard or the picker is up, or the keyboard is on its way back. */
    val composerOpen: Boolean,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun chatComposerPanel(
    composer: ChatComposerState,
    keyboardHeight: RememberedKeyboardHeight,
): ChatComposerPanel {
    val density = LocalDensity.current
    val imePx = WindowInsets.ime.getBottom(density)
    val imeTargetPx = WindowInsets.imeAnimationTarget.getBottom(density)
    val navigationPx = WindowInsets.navigationBars.getBottom(density)
    val rememberedImePx = keyboardHeight.heightPx()
    if (composer.keyboardReturning && keyboardReturnFinished(imePx, imeTargetPx)) composer.keyboardReturning = false
    val panelPx = emotePanelHeightPx(
        navigationBarPx = navigationPx,
        imeBottomPx = imePx,
        pickerOpen = composer.pickerOpen,
        keyboardReturning = composer.keyboardReturning,
        pickerHeightPx = emotePickerHeightPx(rememberedImePx, fallbackPx = with(density) { 280.dp.roundToPx() }),
    )
    return ChatComposerPanel(
        panelPx = panelPx,
        navigationPx = navigationPx,
        imeOpen = imePx > 0,
        composerOpen = chatComposerOpen(imeTargetPx, composer.pickerOpen, composer.keyboardReturning),
    )
}

/** Everything the bar's parts and actions read, as of the latest composition. */
internal data class ChatInput(
    val channel: ChatInputChannel,
    val user: ChatInputUser,
    val compose: ChatCompose,
    val menu: ChatMenu,
    val bar: ChatInputBarLook,
    val rulesGate: ChatRulesGate,
    val rulesFingerprint: String?,
    /** The channel's rules wait to be confirmed before the first message. */
    val rulesNeedConfirmation: Boolean,
    val warningGate: ChatWarningGate,
    /** A warning to acknowledge takes the field's place. */
    val warned: Boolean,
    val imeOpen: Boolean,
    val keyboard: SoftwareKeyboardController?,
    val focusManager: FocusManager,
    val sheetKeyboard: SheetKeyboard,
    val imeDismissFocus: ImeDismissFocus,
    val keyboardHeight: RememberedKeyboardHeight,
    val repository: ChatSendRepository,
)
