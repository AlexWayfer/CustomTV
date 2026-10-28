package name.alexwayfer.customtv.ui.watch

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatterBadgeLine
import name.alexwayfer.customtv.chat.ChatterFollow
import name.alexwayfer.customtv.chat.ChatterSubscription
import name.alexwayfer.customtv.chat.calendarAge
import name.alexwayfer.customtv.data.CustomTvBadgeEdition
import name.alexwayfer.customtv.ui.components.rememberLastNonNull
import name.alexwayfer.customtv.ui.settings.SettingsScrollTarget
import name.alexwayfer.customtv.ui.theme.TwitchDivider
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import java.time.ZoneId

/** The card's first page: what is known about the chatter, and what the viewer can do for them. */
@Composable
internal fun ColumnScope.ChatterProfilePage(
    chatter: ChatterCardChatter,
    load: ChatterCardLoad,
    scroll: ChatterCardScroll,
    nav: ChatterCardNavigation,
    renderMessage: @Composable (ChatMessage) -> Unit,
    onDismiss: () -> Unit,
) {
    val profile = load.profile
    Column(
        modifier = Modifier
            .weight(1f, fill = false)
            .nestedScroll(scroll.profile.stopAtTop)
            // The banner shrinks first, before the profile scrolls under it.
            .nestedScroll(scroll.headerCollapse)
            .verticalScroll(state = scroll.profile.state, overscrollEffect = scroll.profile.overscroll)
            .padding(bottom = 24.dp),
    ) {
        ChatterCardFacts(load, chatter.subscription)
        // In the order the chat shows them before the name: the top chatter's bubble, the badges, the labels.
        ChatterCardTopChatterItem(login = chatter.login)
        ChatterCardBadges(chatter.badgeLines, rememberChatterCustomTvEdition(chatter.userId))
        ChatterCardLabels(userId = chatter.userId, onManage = { nav.open(ChatterCardPage.Labels) })
        ChatterCardCountItem(
            title = stringResource(R.string.chatter_card_last_messages_count, chatter.messages.size),
            icon = R.drawable.ic_chatter_recent_messages,
            enabled = chatter.messages.isNotEmpty(),
            onOpen = { nav.open(ChatterCardPage.Messages) },
        )
        ChatterCardCountItem(
            title = chatterMentionsTitle(chatter.mentions.size),
            icon = R.drawable.ic_chatter_recent_mentions,
            enabled = chatter.mentions.isNotEmpty(),
            onOpen = { nav.open(ChatterCardPage.Mentions) },
        )
        ChatterCardPortraitItem(
            channelId = LocalEmotePreviewChannel.current.twitchUserId,
            userId = chatter.userId,
            onOpen = { nav.open(ChatterCardPage.Portrait) },
        )
        ChatterCardAbout(about = profile?.about, links = profile?.links.orEmpty())
        ChatterCardActions(chatter, nav, renderMessage, onDismiss)
    }
}

/** The chatter's recent messages or mentions in this chat, under a title that stays in place while they scroll. */
@Composable
internal fun ColumnScope.ChatterMessagesPage(
    title: String,
    messages: List<ChatMessage>,
    scroll: ChatterPageScroll,
    renderMessage: @Composable (ChatMessage) -> Unit,
    onBack: () -> Unit,
) {
    ChatterRecentMessagesHeader(title = title, onBack = onBack)
    Column(
        modifier = Modifier
            .weight(1f, fill = false)
            // The messages page opens at its end, so a drag down there scrolls back through them.
            .nestedScroll(scroll.stopAtTop)
            .verticalScroll(state = scroll.state, overscrollEffect = scroll.overscroll)
            .padding(bottom = 24.dp),
    ) {
        messages.forEach { message -> renderMessage(message) }
    }
}

/** The portrait page, for a chatter with an ID in a channel the app knows. */
@Composable
internal fun ChatterPortraitPageFor(
    card: ChatterCardRequest,
    chatter: ChatterCardChatter,
    load: ChatterCardLoad,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
) {
    val previewChannel = LocalEmotePreviewChannel.current
    val broadcasterId = previewChannel.twitchUserId
    val userId = chatter.userId
    if (broadcasterId.isNullOrBlank() || userId == null) return
    val openSettings = LocalOpenSettings.current
    ChatterCardPortraitPage(
        subject = ChatterPortraitSubject(
            channelId = broadcasterId,
            channelName = previewChannel.displayName,
            userId = userId,
            login = chatter.login,
            displayName = chatter.name,
            selfLogin = chatter.selfLogin,
            badgeTitles = chatter.badgeLines.map { it.title },
            badgeUrls = chatter.badgeUrls,
            createdAtMillis = load.profile?.createdAtMillis,
            follow = load.follow,
        ),
        onOpenSettings = openSettings?.let { open ->
            {
                onDismiss()
                open(SettingsScrollTarget.ChatterPortraits, PlayerSheetReturn.ChatterPortrait(card))
            }
        },
        onBack = onBack,
    )
}

/** The profile's loading bar, then the account's age, the follow, and the subscription as each becomes known. */
@Composable
private fun ColumnScope.ChatterCardFacts(load: ChatterCardLoad, subscription: ChatterSubscription?) {
    AnimatedVisibility(visible = !load.loaded, enter = fadeIn(), exit = fadeOut()) {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            color = TwitchPurple,
            trackColor = TwitchDivider,
            gapSize = 0.dp,
        )
    }
    val createdAt = load.profile?.createdAtMillis
    val shownCreatedAt = rememberLastNonNull(createdAt)
    AnimatedVisibility(visible = createdAt != null, enter = fadeIn(), exit = fadeOut()) {
        ChatterCardFact(
            icon = ImageVector.vectorResource(R.drawable.ic_chatter_account_created),
            title = stringResource(R.string.chatter_card_account_created),
            supporting = shownCreatedAt?.let { formatChatterCreatedDate(it) },
            modifier = Modifier.padding(top = 12.dp),
        )
    }
    ChatterFollowFact(load.followLoading, load.follow)
    val shownSubscription = rememberLastNonNull(subscription)
    AnimatedVisibility(visible = subscription != null, enter = fadeIn(), exit = fadeOut()) {
        shownSubscription?.let { sub ->
            ChatterCardFact(
                icon = Icons.Filled.Star,
                title = subscriptionLabel(sub),
            )
        }
    }
}

/** Since when the chatter follows the channel, or that they do not; a spinner fades into it. */
@Composable
private fun ChatterFollowFact(followLoading: Boolean, follow: ChatterFollow?) {
    AnimatedContent(
        targetState = followLoading to follow,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        contentKey = { (loading, shown) -> loading to shown?.javaClass },
        label = "chatterCardFollow",
    ) { (loading, shown) ->
        if (loading) {
            ChatterCardLoadingFact(
                icon = Icons.Filled.Favorite,
                description = stringResource(R.string.chatter_card_follow_loading),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        when (shown) {
            is ChatterFollow.Following -> ChatterCardFact(
                icon = Icons.Filled.Favorite,
                title = stringResource(R.string.chatter_card_following_since),
                supporting = stringResource(
                    R.string.chatter_card_followed_date_with_age,
                    formatChatterCreatedDate(shown.atMillis),
                    calendarAgeText(
                        calendarAge(
                            shown.atMillis,
                            System.currentTimeMillis(),
                            ZoneId.systemDefault(),
                        ),
                    ),
                ),
                copyable = true,
                modifier = Modifier.padding(top = 12.dp),
            )
            ChatterFollow.NotFollowing -> ChatterCardFact(
                icon = Icons.Filled.FavoriteBorder,
                title = stringResource(R.string.chatter_card_not_following),
                modifier = Modifier.padding(top = 12.dp),
            )
            null -> Unit
        }
    }
}

/** The chatter's Twitch badges, then their CustomTV one, as the chat shows them. */
@Composable
private fun ChatterCardBadges(badgeLines: List<ChatterBadgeLine>, customTv: CustomTvBadgeEdition?) {
    if (badgeLines.isEmpty() && customTv == null) return
    Text(
        text = stringResource(R.string.chatter_card_badges),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleSmall,
    )
    badgeLines.forEach { badge ->
        ChatterCardIconRow(image = badge.imageUrl, title = badge.title)
    }
    if (customTv != null) {
        ChatterCardIconRow(
            image = customTvBadgeImage(customTv),
            title = stringResource(customTvBadgeTitle(customTv)),
            onClick = customTvBadgeRowTap(customTv),
        )
    }
}

/** How many recent messages or mentions the chatter has here; dimmed, and not a button, without any. */
@Composable
private fun ChatterCardCountItem(title: String, @DrawableRes icon: Int, enabled: Boolean, onOpen: () -> Unit) {
    val itemAlpha by animateFloatAsState(
        if (enabled) 1f else 0.38f,
        label = "chatter card count item",
    )
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = {
            Icon(painterResource(icon), contentDescription = null)
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onOpen)
            .graphicsLayer { alpha = itemAlpha },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

/**
 * Moderation with an ID, which on the user's own card holds only their logs and the delete; on someone else's card
 * also a whisper while logged in, and a report with an ID.
 */
@Composable
private fun ChatterCardActions(
    chatter: ChatterCardChatter,
    nav: ChatterCardNavigation,
    renderMessage: @Composable (ChatMessage) -> Unit,
    onDismiss: () -> Unit,
) {
    val userId = chatter.userId
    if (userId != null) {
        ChatterCardModeration(
            tools = chatter.modTools,
            displayName = chatter.name,
            message = chatter.sourceMessage,
            renderMessage = renderMessage,
            onOpenLogs = nav::openLogs,
            onShoutoutGiven = onDismiss,
        )
    }
    if (chatter.whisper) {
        ChatterCardWhisperItem(
            login = chatter.login,
            displayName = chatter.name,
            userId = userId,
            onOpened = onDismiss,
        )
    }
    if (chatter.report && userId != null) {
        ChatterCardReportItem(login = chatter.login, displayName = chatter.name, userId = userId)
    }
}
