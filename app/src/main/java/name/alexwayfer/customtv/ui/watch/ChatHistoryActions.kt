package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.State

/** What the history button, its plaque, and the Up and Down keys do with the user's own messages. */
internal class ChatHistoryActions(
    private val composer: ChatComposerState,
    private val editor: ChatDraftEditor,
    private val input: State<ChatInput>,
) {
    /** The history button opens the plaque at the newest message, or closes it. */
    fun toggle() {
        composer.historyShownId = null
        composer.historyOpen = !composer.historyOpen
    }

    fun step(older: Boolean) {
        val history = input.value.compose.sentHistory
        composer.historyShownId = chatHistoryStep(history, composer.historyShownId, older)?.id
    }

    /** A tap on the plaque puts its message in the field in the draft's place, and the plaque goes. */
    fun pick(message: ChatSentMessage) {
        editor.replace(message.text, message.text.length)
        composer.historyRecall = ChatHistoryRecall()
        composer.historyOpen = false
    }

    /** Up ([older]) or Down on a hardware keyboard; false leaves the key to the field. */
    fun recall(older: Boolean): Boolean {
        val compose = input.value.compose
        if (!compose.historyEnabled) return false
        val result = chatHistoryKey(compose.sentHistory, composer.historyRecall, compose.draft.text.toString(), older)
            ?: return false
        composer.historyRecall = result.recall
        editor.replace(result.text, result.text.length)
        return true
    }
}
