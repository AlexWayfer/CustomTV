package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.data.ChannelAvatarRepository
import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.data.CollaborationResult
import name.alexwayfer.customtv.data.SharedChatSource
import name.alexwayfer.customtv.data.sharedChatSources
import name.alexwayfer.customtv.data.sharedChatSourcesToLoad
import name.alexwayfer.customtv.ui.components.SharedEmoteImage
import name.alexwayfer.customtv.ui.theme.TwitchSurfaceAlt

/** The channels of a shared chat by room ID; empty outside one. */
internal val LocalSharedChatSources = compositionLocalOf<Map<String, SharedChatSource>> { emptyMap() }

/** What a shared chat message's row shows of the channel it was sent from. */
internal data class SharedChatSourceLook(val avatarUrl: String?, val description: String)

/**
 * Gives the rows the channels their shared messages come from: the collaborators, and by ID a channel the
 * collaboration did not name, loaded once it has come.
 */
@Composable
internal fun ProvideSharedChatSources(
    channelId: String?,
    collaboration: CollaborationResult?,
    collaborationLoading: Boolean,
    messages: () -> List<ChatMessage>,
    content: @Composable () -> Unit,
) {
    val currentMessages by rememberUpdatedState(messages)
    // Changes only when a new source room speaks, not on every message.
    val seen by remember { derivedStateOf { currentMessages().mapNotNullTo(HashSet()) { it.sourceRoomId } } }
    var loaded by remember(channelId) { mutableStateOf(emptyList<ChannelProfile>()) }
    val requested = remember(channelId) { HashSet<String>() }
    val collaborators = (collaboration as? CollaborationResult.Ready)?.channels.orEmpty()
    val sources = remember(collaborators, loaded, seen) {
        sharedChatSources(collaborators, loaded + seen.mapNotNull(ChannelAvatarRepository::cachedById))
    }
    val toLoad = sharedChatSourcesToLoad(seen, sources.keys, requested, collaborationLoading)
    // Outlives the effect, so a new source speaking while a load runs does not drop what it brings.
    val scope = rememberCoroutineScope()
    LaunchedEffect(toLoad) {
        if (toLoad.isEmpty()) return@LaunchedEffect
        requested += toLoad
        scope.launch { loaded = loaded + ChannelAvatarRepository.refreshByIds(toLoad) }
    }
    CompositionLocalProvider(LocalSharedChatSources provides sources) { content() }
}

/** How a row or a card shows the channel of the source [roomId]; null for this channel's own chat. */
@Composable
internal fun sharedChatSourceLook(roomId: String?): SharedChatSourceLook? {
    if (roomId == null) return null
    val source = LocalSharedChatSources.current[roomId]
    return SharedChatSourceLook(
        avatarUrl = source?.avatarUrl,
        description = source?.let { stringResource(R.string.chat_shared_source, it.displayName) }
            ?: stringResource(R.string.chat_shared_source_unknown),
    )
}

/**
 * The source channel's avatar ahead of the badges; in a message it opens the author's card as they do, and in an event
 * line, which has no card, it takes no touch.
 */
internal fun AnnotatedString.Builder.appendSharedChatSource(
    source: SharedChatSourceLook?,
    sizes: ChatTextSizes,
    inlineContent: MutableMap<String, InlineTextContent>,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    if (source == null) return
    val key = "shared_chat_source"
    appendInlineContent(key, source.description)
    inlineContent[key] = InlineTextContent(
        Placeholder(sizes.badgeSlotWidth, sizes.badgeSlotHeight, PlaceholderVerticalAlign.Center),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (onClick != null) {
                        Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            // A circle holds the place until the channel is known, so the row does not reflow when it comes.
            Box(
                modifier = Modifier
                    .size(sizes.badge)
                    .clip(CircleShape)
                    .background(TwitchSurfaceAlt),
            ) {
                // The source repeats on many rows; the shared store loads its avatar once.
                source.avatarUrl?.let { url ->
                    SharedEmoteImage(url = url, contentDescription = null, size = sizes.badge)
                }
            }
        }
    }
}
