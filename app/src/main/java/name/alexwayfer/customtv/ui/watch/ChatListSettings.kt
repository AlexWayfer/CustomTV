package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.StateFlow
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.data.KeywordPhrase
import name.alexwayfer.customtv.data.LinkPreviewMode

/** What makes a row a mention: the signed-in user's name, or one of their keywords. */
internal data class ChatMentions(
    val highlightMentions: Boolean,
    val selfLogin: String,
    val selfDisplayName: String,
    val keywordPhrases: List<KeywordPhrase>,
) {
    fun matches(message: ChatMessage): Boolean =
        chatRowMentioned(message, highlightMentions, selfLogin, selfDisplayName, keywordPhrases)
}

/** How a chat draws its rows and moves its list, the same for every row of the chat. */
internal data class ChatListSettings(
    val appearance: ChatAppearance,
    val showTimestamps: Boolean,
    val smoothScroll: Boolean,
    val highlightColor: Color,
    val channelColor: Color,
    val highlightRewardCost: Int,
    val channelPointsIconUrl: String?,
    val highlightFirstMessages: Boolean,
    val linkPreviewMode: LinkPreviewMode,
    val mentions: ChatMentions,
    /** Whether highlighted messages that scroll away stay pinned under the notices for [highlightPinSeconds]. */
    val pinHighlights: Boolean,
    val highlightPinSeconds: Int,
)

/** The chat's messages and the list that shows them, held by the screen, and what the input does with them. */
internal data class ChatFeed(
    /** Read where shown, so a new message recomposes the chat and not the screen that holds it. */
    val messages: () -> List<ChatMessage>,
    val recentAuthorMessages: StateFlow<Map<String, List<ChatMessage>>>,
    val listState: LazyListState,
    val highlightPins: TemporaryHighlightPins,
    val stickToBottom: Boolean,
    val onStickToBottomChange: (Boolean) -> Unit,
    /** Grows with each load of a replay or seek in a recording: a jump in the timeline, not new chat. */
    val timelineJumpGeneration: Int,
    val replyingToMessageId: String?,
    val onReply: ((ChatMessage) -> Unit)?,
    val fieldCardOpen: OneShotRequest<ChatterCardRequest>?,
)

/** Where a row sits in its list, so a selected row stays in sight above [bottomOverlayPx]. */
internal data class ChatRowPlace(
    val listState: LazyListState,
    val bottomOverlayPx: Int,
)

/** What a row does when it is held, dismissed, replied to, or swiped. */
internal class ChatRowActions(
    val onSelect: () -> Unit,
    val onDismissSelection: (ChatRowHoldEnd) -> Unit,
    val onReply: ((ChatMessage) -> Unit)?,
    val onReplySwipingChange: (Boolean) -> Unit,
    val onReplySwipeEnd: (ChatSwipeRelease) -> Unit,
)
