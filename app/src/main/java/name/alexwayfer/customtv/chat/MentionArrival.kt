package name.alexwayfer.customtv.chat

/**
 * The chat shown right after a timeline jump (the first frame, a loaded replay, or a seek) is the new
 * baseline, not an arrival. Only messages that appear after it while playing alert.
 */
internal fun mentionBaselineResets(baselineGeneration: Int?, timelineGeneration: Int): Boolean =
    baselineGeneration != timelineGeneration

/**
 * A mention alerts while the app is on screen, a picture-in-picture window included, or after locking the screen
 * over it, until the screen is unlocked. An app left in the background stays silent, also once the screen locks
 * over another app: nothing on screen would say where the sound came from.
 */
internal fun mentionAlertAudible(appStarted: Boolean, stoppedByLock: Boolean, screenUnlocked: Boolean): Boolean =
    appStarted || stoppedByLock && !screenUnlocked

internal fun newMentionArrived(
    seenIds: Set<String>,
    messages: List<ChatMessage>,
    self: ChatMentionSelf,
    manuallyDismissedIds: Set<String> = emptySet(),
): Boolean {
    return messages.any { message ->
        message.id !in seenIds && message.id !in manuallyDismissedIds && (
            messageMentionsUser(message, self.login, self.displayName) ||
                messageMatchesKeyword(message, self.keywordPhrases, self.login)
            )
    }
}
