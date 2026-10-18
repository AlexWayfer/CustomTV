package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import name.alexwayfer.customtv.chat.ChatBadge
import name.alexwayfer.customtv.data.DisplayedChatBadges
import name.alexwayfer.customtv.data.DisplayedChatBadgesRepository

/** The chatter's badges on the card with their images; [badges] is null while the card still loads them. */
internal data class ChatterCardBadges(val badges: List<ChatBadge>?, val imageUrls: Map<String, String>)

/** The [card]'s own badges, or for a card without them, the [loaded] ones once they come. */
internal fun chatterCardBadges(card: ChatterCardRequest, loaded: DisplayedChatBadges?): ChatterCardBadges = when {
    card.badgesKnown -> ChatterCardBadges(card.badges, card.badgeUrls)
    loaded != null -> ChatterCardBadges(loaded.badges, card.badgeUrls + loaded.imageUrls)
    else -> ChatterCardBadges(null, card.badgeUrls)
}

/** Loads the badges of a chatter the card has no message from, once their ID is known. */
@Composable
internal fun rememberChatterCardBadges(card: ChatterCardRequest, userId: String?): ChatterCardBadges {
    val channelId = LocalEmotePreviewChannel.current.twitchUserId
    var loaded by remember(card.login, userId, channelId) { mutableStateOf<DisplayedChatBadges?>(null) }
    LaunchedEffect(card.login, card.badgesKnown, userId, channelId) {
        if (card.badgesKnown || userId.isNullOrBlank() || channelId.isNullOrBlank()) return@LaunchedEffect
        // A failed load leaves the card as for a chatter without badges, so a moderator still gets the actions.
        loaded = DisplayedChatBadgesRepository.load(userId, channelId) ?: DisplayedChatBadges(emptyList(), emptyMap())
    }
    return remember(card, loaded) { chatterCardBadges(card, loaded) }
}
