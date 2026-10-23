package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.chat.ChatMessage

/** The channel a shared chat message was sent from, as its row shows it. */
internal data class SharedChatSource(val displayName: String, val avatarUrl: String?)

/**
 * The channels of a shared chat by room ID: the collaborators, then the [profiles] loaded by ID for a channel the
 * collaboration did not name, such as a guest who joined after it loaded.
 */
internal fun sharedChatSources(
    collaborators: List<CollaborationChannel>,
    profiles: List<ChannelProfile>,
): Map<String, SharedChatSource> {
    val sources = linkedMapOf<String, SharedChatSource>()
    collaborators.forEach { channel ->
        sources[channel.id] = SharedChatSource(channel.displayName.ifBlank { channel.login }, channel.avatarUrl)
    }
    profiles.forEach { profile ->
        val id = profile.id?.takeIf { it.isNotBlank() } ?: return@forEach
        val name = profile.displayName.ifBlank { profile.login }.takeIf { it.isNotBlank() } ?: return@forEach
        sources.putIfAbsent(id, SharedChatSource(name, profile.avatarUrl))
    }
    return sources
}

/**
 * Whether a row's message, rather than its event line, shows the source channel: only a row without an event line,
 * so a resubscription with a message shows it once.
 */
internal fun sharedChatSourceOnUserMessage(systemText: String?): Boolean = systemText.isNullOrBlank()

/** The room a chatter's card names as their chat: that of the message it was opened from, or of their latest. */
internal fun chatterSharedChatSourceRoomId(openedFrom: ChatMessage?, messages: List<ChatMessage>): String? =
    if (openedFrom != null) {
        openedFrom.sourceRoomId
    } else {
        messages.maxByOrNull { it.timestampMillis }?.sourceRoomId
    }

/**
 * Whether the collaboration that names a shared chat's channels may still come: until the channel is known, and
 * while a channel that streams together waits for it.
 */
internal fun sharedChatCollaborationLoading(
    channelId: String?,
    sharedViewerCount: Int?,
    collaboration: CollaborationResult?,
): Boolean = channelId.isNullOrBlank() || (sharedViewerCount != null && collaboration == null)

/**
 * The source rooms to load by ID: seen in the chat, neither known nor asked for before. None while the
 * collaboration, which names most of them, still loads.
 */
internal fun sharedChatSourcesToLoad(
    seen: Set<String>,
    known: Set<String>,
    requested: Set<String>,
    collaborationLoading: Boolean,
): Set<String> = if (collaborationLoading) emptySet() else seen - known - requested
