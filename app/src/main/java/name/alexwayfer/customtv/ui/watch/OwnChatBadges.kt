package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import name.alexwayfer.customtv.chat.ChatBadge
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.latestOwnChatMessage
import name.alexwayfer.customtv.chat.ownChatBadgesNow
import name.alexwayfer.customtv.data.DisplayedChatBadges
import name.alexwayfer.customtv.data.OwnChatBadgesRepository

/** The signed-in user's badges in this channel, for their own chatter card. */
internal class OwnChatBadges(
    val badges: List<ChatBadge>,
    val imageUrls: Map<String, String>,
)

@Composable
internal fun rememberOwnChatBadges(
    userId: String?,
    channelId: String?,
    selfLogin: String?,
    messages: () -> List<ChatMessage>,
    badgeUrls: Map<String, String>,
): OwnChatBadges? {
    var loaded by remember(userId, channelId) { mutableStateOf<DisplayedChatBadges?>(null) }
    var loadedAtMillis by remember(userId, channelId) { mutableLongStateOf(Long.MAX_VALUE) }
    LaunchedEffect(userId, channelId) {
        if (userId.isNullOrBlank() || channelId.isNullOrBlank()) return@LaunchedEffect
        loadedAtMillis = System.currentTimeMillis()
        loaded = OwnChatBadgesRepository.load(userId, channelId)
    }
    val currentMessages by rememberUpdatedState(messages)
    // Only a new own message changes this, so other chat messages do not recompose the caller.
    val latestOwn by remember(selfLogin) {
        derivedStateOf { latestOwnChatMessage(currentMessages(), selfLogin.orEmpty()) }
    }
    if (userId.isNullOrBlank()) return null
    val badges = ownChatBadgesNow(loaded?.badges, loadedAtMillis, latestOwn)
        ?: return null
    val loadedImageUrls = loaded?.imageUrls
    // The same instance while nothing changed: a new one on every chat message would make the callbacks that
    // capture it new too, and recompose the chat input with each message.
    return remember(badges, badgeUrls, loadedImageUrls) {
        OwnChatBadges(badges, badgeUrls + loadedImageUrls.orEmpty())
    }
}

/** The signed-in user's own chatter card, with their badges in this channel once they are known. */
internal fun ownChatterCardRequest(badges: OwnChatBadges?, login: String, displayName: String, userId: String?) = ChatterCardRequest(
    login = login,
    displayName = displayName.ifBlank { login },
    userId = userId,
    badges = badges?.badges.orEmpty(),
    badgeUrls = badges?.imageUrls.orEmpty(),
)
