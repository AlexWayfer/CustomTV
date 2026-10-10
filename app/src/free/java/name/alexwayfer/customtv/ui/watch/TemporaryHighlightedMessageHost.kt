package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.data.KeywordPhrase

@Suppress("unused")
@Composable
internal fun TemporaryHighlightedMessageHost(
    messages: List<ChatMessage>,
    pins: TemporaryHighlightPins,
    listState: LazyListState,
    enabled: Boolean,
    pinSeconds: Int,
    stickToBottom: Boolean,
    timelineJumpGeneration: Int,
    highlightMentions: Boolean,
    selfLogin: String,
    selfDisplayName: String,
    keywordPhrases: List<KeywordPhrase>,
    appearance: ChatAppearance,
    replyingToMessageId: String?,
    onReply: ((ChatMessage) -> Unit)?,
) = Unit
