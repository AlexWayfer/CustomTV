package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.chat.ChatMessage

internal enum class ChatReturnScroll {
    Stay,
    JumpToEnd,
}

internal fun restartEntranceAfterManualScroll(userInput: Boolean, canScrollForward: Boolean): Boolean =
    userInput && !canScrollForward

/** Returning to the latest line jumps. A lazy list cannot glide across rows it has not measured. */
internal fun chatReturnScroll(messageCount: Int): ChatReturnScroll = when {
    messageCount <= 0 -> ChatReturnScroll.Stay
    else -> ChatReturnScroll.JumpToEnd
}

internal fun timelineJumpScroll(
    generation: Int,
    handledGeneration: Int,
    messageCount: Int,
): ChatReturnScroll = when {
    generation <= handledGeneration -> ChatReturnScroll.Stay
    else -> chatReturnScroll(messageCount)
}

/**
 * Messages that arrived while the screen was locked are not entrance-animated when the chat was
 * already stuck to the bottom. A reader who had scrolled away keeps their place.
 */
internal fun skipEntranceAfterPause(wasStuckToBottom: Boolean, becameResumed: Boolean): Boolean =
    wasStuckToBottom && becameResumed

/**
 * A downward scroll moves the entrance cursor forward to the furthest visible row.
 * It does not move the cursor backward, so the list is not pulled back up.
 */
internal fun entranceCursorAfterScrollingDown(
    messages: List<ChatMessage>,
    previousTailMessageId: String?,
    lastVisibleMessageId: String?,
): String? {
    if (lastVisibleMessageId == null) return previousTailMessageId
    val visibleIndex = messages.indexOfLast { it.id == lastVisibleMessageId }
    if (visibleIndex < 0) return previousTailMessageId
    val previousIndex = previousTailMessageId?.let { id -> messages.indexOfLast { it.id == id } } ?: -1
    return if (visibleIndex > previousIndex) lastVisibleMessageId else previousTailMessageId
}

/** Skip fully revealed rows, but still animate the remainder of a partially visible row. */
internal fun firstEntranceIndex(firstNewIndex: Int, lastFullyVisibleIndex: Int?): Int =
    if (lastFullyVisibleIndex == null) firstNewIndex else maxOf(firstNewIndex, lastFullyVisibleIndex + 1)

/** The buffer already on screen when the reader returns is the new tail, so it is not entrance-animated. */
internal fun tailAfterReturn(messages: List<ChatMessage>): String? = messages.lastOrNull()?.id

/** How many messages were appended after [previousTailMessageId]. Zero when this is not a tail append. */
internal fun appendedChatMessageCount(
    previousTailMessageId: String?,
    messages: List<ChatMessage>,
): Int {
    if (previousTailMessageId == null) return 0
    val previousIndex = messages.indexOfLast { it.id == previousTailMessageId }
    if (previousIndex < 0) return 0
    return messages.lastIndex - previousIndex
}
