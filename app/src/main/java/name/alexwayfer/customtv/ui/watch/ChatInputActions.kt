package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.State
import androidx.compose.ui.text.TextRange
import kotlinx.coroutines.CoroutineScope
import name.alexwayfer.customtv.chat.ChatNick
import name.alexwayfer.customtv.chat.PickerEmote
import name.alexwayfer.customtv.chat.chatSendWarned
import name.alexwayfer.customtv.chat.textWithCompletedEmote
import name.alexwayfer.customtv.chat.textWithCompletedNick
import name.alexwayfer.customtv.chat.textWithEmoteSearchColon
import name.alexwayfer.customtv.chat.textWithInsertedEmote
import name.alexwayfer.customtv.chat.textWithInsertedMention
import name.alexwayfer.customtv.data.CHAT_MESSAGE_MAX_LENGTH
import name.alexwayfer.customtv.data.ChatSendResult
import name.alexwayfer.customtv.data.chatMessageToSend
import name.alexwayfer.customtv.data.chatSendError

/**
 * Completions and emotes put into the draft. The lists keep rows built earlier, so each edit reads the draft and the
 * settings at the tap, and text and cursor always come from one state.
 */
internal class ChatDraftEditor(private val input: State<ChatInput>) {
    private val draft get() = input.value.compose.draft

    fun replace(text: String, cursor: Int) {
        draft.edit {
            replace(0, length, text)
            selection = TextRange(cursor)
        }
    }

    fun pickEmote(emote: PickerEmote) {
        val inserted = textWithCompletedEmote(
            draft.text.toString(),
            draft.selection.end,
            emote.name,
            CHAT_MESSAGE_MAX_LENGTH,
            withoutColon = input.value.compose.emotes.completionWithoutColon,
        ) ?: return
        replace(inserted.text, inserted.cursor)
    }

    fun pickCommand(command: ChatCommand) {
        val completed = textWithCompletedCommand(
            draft.text.toString(),
            draft.selection.end,
            command,
            CHAT_MESSAGE_MAX_LENGTH,
        ) ?: return
        replace(completed.text, completed.cursor)
    }

    fun pickNick(nick: ChatNick) {
        val completed = textWithCompletedNick(
            draft.text.toString(),
            draft.selection.end,
            nick.displayName,
            CHAT_MESSAGE_MAX_LENGTH,
        ) ?: return
        replace(completed.text, completed.cursor)
    }

    fun insertEmote(name: String) {
        val inserted = textWithInsertedEmote(
            draft.text.toString(),
            draft.selection.start,
            draft.selection.end,
            name,
            CHAT_MESSAGE_MAX_LENGTH,
        ) ?: return
        replace(inserted.text, inserted.cursor)
    }

    /** Puts `@name` at the cursor, as Mention in a message's menu asks. */
    fun insertMention(name: String) {
        textWithInsertedMention(
            draft.text.toString(),
            draft.selection.start,
            draft.selection.end,
            name,
            CHAT_MESSAGE_MAX_LENGTH,
        )?.let { replace(it.text, it.cursor) }
    }

    /** Puts a `:` at the cursor, so the field's completion takes over the picker's search. */
    fun addEmoteSearchColon() {
        textWithEmoteSearchColon(
            draft.text.toString(),
            draft.selection.start,
            draft.selection.end,
            CHAT_MESSAGE_MAX_LENGTH,
        )?.let { replace(it.text, it.cursor) }
    }
}

/** What the bar does: it opens the picker, the rules, and the menu, and sends the draft. */
internal class ChatInputActions(
    private val composer: ChatComposerState,
    private val field: ChatFieldState,
    val editor: ChatDraftEditor,
    private val scope: CoroutineScope,
    private val input: State<ChatInput>,
) {
    fun focusField() {
        field.focus.requestFocus()
        input.value.keyboard?.show()
    }

    fun openRules(trigger: ChatRulesTrigger) {
        composer.rulesTrigger = trigger
        composer.pickerOpen = false
        input.value.sheetKeyboard.open { composer.rulesOpen = true }
    }

    fun togglePicker() {
        val now = input.value
        if (!now.user.canSend) return
        if (!composer.pickerOpen && now.rulesNeedConfirmation) {
            openRules(ChatRulesTrigger.EmotePicker)
            return
        }
        if (composer.pickerOpen) {
            composer.pickerOpen = false
            composer.keyboardReturning = true
            focusField()
            return
        }
        if (now.imeOpen) now.imeDismissFocus.keepFocus = true
        now.keyboard?.hide()
        // Heights of a keyboard the user has since switched away from would size the picker wrong.
        now.keyboardHeight.useCurrentKeyboard()
        composer.pickerOpen = true
    }

    /** The picker hands its search to the `:` completion, which shows matches above the keyboard. */
    fun searchEmotes() {
        editor.addEmoteSearchColon()
        composer.pickerOpen = false
        composer.keyboardReturning = true
        focusField()
    }

    /** Reply or Share asked for the field: the keyboard comes up, or the rules first. */
    fun askForField() {
        val now = input.value
        when (chatReplyComposer(now.user.canSend, now.warned, now.rulesNeedConfirmation)) {
            ChatReplyComposer.Keyboard -> focusField()
            ChatReplyComposer.Rules -> openRules(ChatRulesTrigger.MessageField)
            ChatReplyComposer.Nothing -> Unit
        }
    }

    fun confirmRules() {
        val now = input.value
        composer.rulesOpen = false
        now.rulesGate.acknowledge(scope, now.rulesFingerprint)
        when (chatRulesFollowUp(composer.rulesTrigger)) {
            ChatRulesFollowUp.EmotePicker -> {
                now.keyboardHeight.useCurrentKeyboard()
                composer.pickerOpen = true
            }
            ChatRulesFollowUp.Keyboard -> composer.focusAfterRules = true
            ChatRulesFollowUp.Nothing -> Unit
        }
    }

    /** The end button sends the draft, cancels a slow send, or opens the chat menu when there is nothing to send. */
    fun tapEndButton(hasMessage: Boolean) {
        when (chatFieldEndTap(composer.sending, composer.sendCancelable, hasMessage)) {
            ChatFieldEndTap.CancelSend -> {
                composer.cancelSend()
                if (chatCancelFocusesField(field.focused, composer.pickerOpen)) focusField()
            }
            ChatFieldEndTap.Nothing -> Unit
            ChatFieldEndTap.Send -> submit()
            ChatFieldEndTap.Menu -> {
                if (field.menuOpen) return
                composer.pickerOpen = false
                input.value.sheetKeyboard.open { field.menuOpen = true }
            }
        }
    }

    fun submit() {
        // `/user` and `/prediction` post nothing, so neither the rules nor slow mode hold them.
        if (runFieldCommand()) return
        val now = input.value
        val sharing = now.compose.shareOffer?.takeIf { it.composing }
        val message = chatTextToSubmit(now.compose.draft.text.toString(), emptyAllowed = sharing?.emptyAllowed == true)
        val held = now.compose.slowSendBlocked || !now.user.canSend || composer.sending
        if (message == null || held) return
        // Rules that arrived after the field was already focused still come before the first message.
        if (now.rulesNeedConfirmation) {
            openRules(ChatRulesTrigger.Send)
            return
        }
        composer.sendError = null
        if (chatSendHidesKeyboard(now.compose.keepKeyboardAfterSend)) {
            now.focusManager.clearFocus(force = true)
            now.keyboard?.hide()
        }
        // The send works with the chat it was tapped in, even if the screen moves on, such as with a raid.
        composer.startSend(scope) {
            if (sharing != null) share(sharing, message, now.compose) else send(message, now)
        }
    }

    /**
     * Opens the card `/user` names, or the prediction for `/prediction` with the keyboard going down first; true when
     * the text was one of them, valid or not. A user without `/prediction` sends it to chat as typed.
     */
    private fun runFieldCommand(): Boolean {
        val now = input.value
        val compose = now.compose
        val text = compose.draft.text.toString()
        val openPrediction = compose.onOpenPrediction
        if (openPrediction != null && isPredictionCommand(text)) {
            compose.draft.clearText()
            composer.pickerOpen = false
            now.sheetKeyboard.open { openPrediction() }
            return true
        }
        return when (val command = chatUserCommand(text, compose.sessionChatters())) {
            is ChatUserCommand.Open -> {
                compose.draft.clearText()
                compose.onOpenChatterCard(command.login)
                true
            }
            ChatUserCommand.Invalid -> true
            null -> false
        }
    }

    private suspend fun share(sharing: ChatShareOffer, message: String, compose: ChatCompose) {
        val error = sharing.share(message)
        if (error == null) {
            compose.draft.clearText()
            compose.onSent()
        } else {
            composer.sendError = error
        }
    }

    /** Sends [message] as of [sent], the bar when Send was tapped: its channel, user, reply, and draft. */
    private suspend fun send(message: String, sent: ChatInput) {
        val compose = sent.compose
        val token = sent.user.accessToken()?.takeIf { it.isNotBlank() }
        val channelId = sent.channel.broadcasterId?.takeIf { it.isNotBlank() }
        val userId = sent.user.userId?.takeIf { it.isNotBlank() }
        val result = if (token == null || userId == null) {
            ChatSendResult.Rejected(401, "")
        } else if (channelId == null) {
            ChatSendResult.Unavailable
        } else {
            sent.repository.send(token, channelId, userId, message, compose.replyParentMessageId)
        }
        val draftMatches = chatMessageToSend(compose.draft.text.toString()) == message
        if (chatSendCompleted(result, draftMatches)) {
            compose.shareOffer?.onPlainSent?.invoke()
            compose.draft.clearText()
            compose.onSent()
        } else if (chatSendWarned(result)) {
            // The plaque says it, and the draft stays for after the warning is acknowledged.
            sent.warningGate.sendRefused()
        } else {
            composer.sendError = chatSendError(result)
        }
    }
}
