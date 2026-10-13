package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import name.alexwayfer.customtv.chat.ChatBadge
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatterBadgeLine
import name.alexwayfer.customtv.chat.ChatterFollow
import name.alexwayfer.customtv.chat.ChatterFollowSource
import name.alexwayfer.customtv.chat.ChatterProfile
import name.alexwayfer.customtv.chat.ChatterSubscription
import name.alexwayfer.customtv.chat.chatterBadgeLines
import name.alexwayfer.customtv.chat.chatterCardShowsOwnFollow
import name.alexwayfer.customtv.chat.chatterFollowSource
import name.alexwayfer.customtv.chat.moderatesChannel
import name.alexwayfer.customtv.chat.moderatorChatterFollow
import name.alexwayfer.customtv.data.ChatBadgeRepository
import name.alexwayfer.customtv.data.ProfileDetailsRepository
import name.alexwayfer.customtv.data.toChatterProfile

/** Which page of the chatter card shows, and the tab the moderation logs open on. */
internal class ChatterCardNavigation {
    var page by mutableStateOf(ChatterCardPage.Profile)
        private set
    var modLogsTab by mutableIntStateOf(0)
        private set

    fun open(page: ChatterCardPage) {
        this.page = page
    }

    fun openLogs(tab: Int) {
        modLogsTab = tab
        page = ChatterCardPage.ModLogs
    }

    fun back() {
        page = ChatterCardPage.Profile
    }
}

/** What the card loads about the chatter: their profile, kept from the last load, and whether they follow. */
internal class ChatterCardLoad(cachedProfile: ChatterProfile?) {
    var profile by mutableStateOf(cachedProfile)
    var loaded by mutableStateOf(cachedProfile != null)
    var follow by mutableStateOf<ChatterFollow?>(null)
    var followLoading by mutableStateOf(false)
}

/**
 * Loads the chatter's profile and follow. A card opened again shows the profile kept before at once, and still
 * loads it in the background; the follow shows when the viewer may see it: on their own card or as a moderator.
 */
@Composable
internal fun rememberChatterCardLoad(login: String, userId: String?): ChatterCardLoad {
    val load = remember(login) { ChatterCardLoad(ProfileDetailsRepository.cached(login)?.toChatterProfile()) }
    LaunchedEffect(login) {
        ProfileDetailsRepository.load(login)?.let { load.profile = it.toChatterProfile() }
        load.loaded = true
    }
    val cache = LocalChatterCardCache.current
    val ownFollow = LocalOwnChannelFollow.current
    val broadcasterId = LocalEmotePreviewChannel.current.twitchUserId
    // A card opened by `/user` for someone who has not written here gets the ID from the loaded profile.
    val knownUserId = userId ?: load.profile?.userId
    LaunchedEffect(login, knownUserId, ownFollow?.userId, ownFollow?.login, broadcasterId) {
        load.follow = null
        load.followLoading = false
        if (broadcasterId.isNullOrBlank()) return@LaunchedEffect
        val ownCard = ownFollow != null &&
            chatterCardShowsOwnFollow(knownUserId, login, ownFollow.userId, ownFollow.login)
        // The own card knows the chatter's ID from the account before the profile loads it.
        val chatterUserId = knownUserId ?: ownFollow?.userId?.takeIf { ownCard }
        val source = chatterFollowSource(ownCard, chatterUserId, broadcasterId, moderatesChannel(broadcasterId))
        if (source == ChatterFollowSource.None) return@LaunchedEffect
        load.follow = cache?.cachedFollow(broadcasterId, login)
        load.followLoading = load.follow == null
        val loadedFollow = when (source) {
            ChatterFollowSource.Own -> ownFollow?.follow(broadcasterId)
            else -> knownUserId?.let { moderatorChatterFollow(broadcasterId, it) }
        }
        load.follow = cache?.rememberFollow(broadcasterId, login, loadedFollow) ?: loadedFollow
        load.followLoading = false
    }
    return load
}

/** The chatter the card is about, as far as it is known, and what the card does for them. */
internal data class ChatterCardChatter(
    val login: String,
    val name: String,
    val userId: String?,
    val selfLogin: String,
    val badgeUrls: Map<String, String>,
    val badgeLines: List<ChatterBadgeLine>,
    val subscription: ChatterSubscription?,
    /** Their recent messages in this chat. */
    val messages: List<ChatMessage>,
    /** The recent messages in this chat that mention them. */
    val mentions: List<ChatMessage>,
    /** The message the card was opened from, as the chat shows it now. */
    val sourceMessage: ChatMessage?,
    /** Someone else's card, which offers whispers and, with an ID, moderation and reports. */
    val othersCard: Boolean,
    val modTools: ChatterModTools,
)

/** The chatter's badges with the titles Twitch gives their images. */
@Composable
internal fun rememberChatterBadgeLines(
    badges: List<ChatBadge>,
    badgeUrls: Map<String, String>,
): List<ChatterBadgeLine> =
    remember(badges, badgeUrls) {
        val titles = buildMap {
            badgeUrls.forEach { (key, url) ->
                ChatBadgeRepository.titleForImage(url)?.let { put(key, it) }
            }
        }
        chatterBadgeLines(badges, badgeUrls, titles)
    }

/** A card page's scroll with its own edge stretch, which takes the drag past the top that the sheet must not get. */
internal class ChatterPageScroll(
    val state: ScrollState,
    val overscroll: OverscrollEffect?,
    val stopAtTop: NestedScrollConnection,
)

/** The scroll of each page, and the banner that folds as the profile scrolls. */
internal class ChatterCardScroll(
    val profile: ChatterPageScroll,
    val messages: ChatterPageScroll,
    val mentions: ChatterPageScroll,
    val headerCollapse: ChatterHeaderCollapse,
)

/** A drag back up through any page stops at its top; closing the sheet takes a new drag. */
@Composable
internal fun rememberChatterCardScroll(): ChatterCardScroll {
    val profileState = rememberScrollState()
    val headerCollapse = rememberChatterHeaderCollapse(profileState)
    val profileOverscroll = rememberOverscrollEffect()
    val stopProfileAtTop = rememberStopAtScrollTop(profileState, profileOverscroll) {
        profileState.value == 0 && headerCollapse.collapsedPx == 0f
    }
    return ChatterCardScroll(
        profile = ChatterPageScroll(profileState, profileOverscroll, stopProfileAtTop),
        messages = rememberChatterPageScroll(),
        mentions = rememberChatterPageScroll(),
        headerCollapse = headerCollapse,
    )
}

@Composable
private fun rememberChatterPageScroll(): ChatterPageScroll {
    val state = rememberScrollState()
    val overscroll = rememberOverscrollEffect()
    return ChatterPageScroll(state, overscroll, rememberStopAtScrollTop(state, overscroll))
}

/** The messages and the mentions pages open on the newest message and stay there as it comes, unless scrolled up. */
@Composable
internal fun FollowChatterMessagesEnd(page: ChatterCardPage, cardScroll: ChatterCardScroll) {
    LaunchedEffect(page) {
        val scroll = when (page) {
            ChatterCardPage.Messages -> cardScroll.messages.state
            ChatterCardPage.Mentions -> cardScroll.mentions.state
            else -> return@LaunchedEffect
        }
        var following = true
        var previousMax = -1
        snapshotFlow { scroll.value to scroll.maxValue }.collect { (value, max) ->
            // The range is unknown until the page is measured.
            if (max == Int.MAX_VALUE) return@collect
            following = followChatterMessagesEnd(following, previousMax, value, max)
            previousMax = max
            if (following && value < max) scroll.scrollTo(max)
        }
    }
}
