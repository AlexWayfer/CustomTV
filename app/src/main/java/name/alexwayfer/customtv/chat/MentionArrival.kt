package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.KeywordPhrase

/**
 * The chat shown right after a timeline jump (the first frame, a loaded replay, or a seek) is the new
 * baseline, not an arrival. Only messages that appear after it while playing alert.
 */
internal fun mentionBaselineResets(baselineGeneration: Int?, timelineGeneration: Int): Boolean =
    baselineGeneration != timelineGeneration

internal fun newMentionArrived(
    seenIds: Set<String>,
    messages: List<ChatMessage>,
    selfLogin: String,
    selfDisplayName: String,
    phrases: List<KeywordPhrase> = emptyList(),
): Boolean {
    return messages.any { message ->
        message.id !in seenIds && (
            messageMentionsUser(message, selfLogin, selfDisplayName) ||
                messageMatchesKeyword(message, phrases, selfLogin)
            )
    }
}
