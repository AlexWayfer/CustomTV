package name.alexwayfer.customtv.ui.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import name.alexwayfer.customtv.auth.TwitchAccount
import name.alexwayfer.customtv.auth.TwitchProfileLink
import name.alexwayfer.customtv.data.ProfileDetailsRepository
import name.alexwayfer.customtv.ui.components.rememberChannelProfile
import name.alexwayfer.customtv.ui.theme.TwitchSurfaceAlt
import java.text.NumberFormat
import java.util.Locale

private data class ChannelAbout(
    val login: String,
    val displayName: String,
    val avatarUrl: String?,
    val bannerUrl: String?,
    val description: String?,
    val followerCount: Int?,
    val links: List<TwitchProfileLink>,
    val lastLiveMillis: Long?,
    val userId: String? = null,
)

@Composable
internal fun ChannelAboutScreen(
    channel: String,
    backEnabled: Boolean,
    onLeave: () -> Unit,
    onOpenChannel: (String) -> Unit,
    onOpenVideo: (ProfileVideoPlayback) -> Unit,
    firstTabRequest: Int,
    canWhisper: Boolean,
) {
    val profile = rememberChannelProfile(channel)
    val details by produceState(initialValue = ProfileDetailsRepository.cached(channel), channel) {
        // A failed load keeps what the profile already shows.
        ProfileDetailsRepository.load(channel)?.let { value = it }
    }
    ChannelAboutContent(
        about = ChannelAbout(
            login = profile.login,
            displayName = profile.displayName,
            avatarUrl = profile.avatarUrl,
            bannerUrl = details?.bannerUrl,
            description = details?.description,
            followerCount = details?.followerCount,
            links = details?.links.orEmpty(),
            lastLiveMillis = details?.lastLiveMillis,
            userId = details?.userId ?: profile.id,
        ),
        backEnabled = backEnabled,
        onLeave = onLeave,
        onOpenChannel = onOpenChannel,
        onOpenVideo = onOpenVideo,
        firstTabRequest = firstTabRequest,
        canWhisper = canWhisper,
    )
}

@Composable
internal fun AccountAboutScreen(
    account: TwitchAccount,
    onLeave: () -> Unit,
    onLogOut: () -> Unit,
    onOpenChannel: (String) -> Unit,
    onOpenVideo: (ProfileVideoPlayback) -> Unit,
) {
    val details by produceState(initialValue = ProfileDetailsRepository.cached(account.login), account.login) {
        ProfileDetailsRepository.load(account.login)?.let { value = it }
    }
    ChannelAboutContent(
        about = ChannelAbout(
            login = account.login,
            displayName = account.displayName,
            avatarUrl = account.avatarUrl,
            bannerUrl = details?.bannerUrl ?: account.bannerUrl,
            description = details?.description ?: account.description,
            followerCount = details?.followerCount ?: account.followerCount,
            links = details?.links ?: account.links,
            lastLiveMillis = details?.lastLiveMillis,
            userId = details?.userId ?: account.userId,
        ),
        onLeave = onLeave,
        onLogOut = onLogOut,
        onOpenChannel = onOpenChannel,
        onOpenVideo = onOpenVideo,
    )
}

@Composable
private fun ChannelAboutContent(
    about: ChannelAbout,
    onLeave: () -> Unit,
    onOpenChannel: (String) -> Unit,
    onOpenVideo: (ProfileVideoPlayback) -> Unit,
    backEnabled: Boolean = true,
    onLogOut: (() -> Unit)? = null,
    firstTabRequest: Int = 0,
    canWhisper: Boolean = false,
) {
    val context = LocalContext.current
    val aboutBrowserHeightPx = aboutBrowserSheetHeightPx(LocalWindowInfo.current.containerSize.height)
    BackHandler(enabled = backEnabled, onBack = onLeave)
    var viewportPx by remember { mutableIntStateOf(0) }
    var contentPx by remember { mutableIntStateOf(0) }
    var tabsPx by remember { mutableIntStateOf(0) }
    var stickyHeaderPx by remember { mutableIntStateOf(0) }
    var detailsPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val tabsMinHeight = with(density) { profileTabsMinHeightPx(viewportPx, contentPx, tabsPx).toDp() }
    val barPx = with(density) { ProfileBarHeight.roundToPx() }
    // The details start where the avatar overlaps the banner, and the tabs end above their bottom padding.
    val headerTopPx = with(density) { (BannerHeight - AvatarOverlap).roundToPx() }
    val tabRowTopPx = headerTopPx + detailsPx - with(density) { DetailsBottomPadding.roundToPx() } - tabsPx
    val scrollState = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .onSizeChanged { viewportPx = it.height },
    ) {
        Column(
            Modifier
                .verticalScroll(scrollState)
                .onSizeChanged { contentPx = it.height },
        ) {
            // The gray fill holds the banner's place, so the profile does not jump when the image arrives.
            AsyncImage(
                model = ImageRequest.Builder(context).data(about.bannerUrl).crossfade(true).build(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(BannerHeight).background(TwitchSurfaceAlt),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier
                    .onSizeChanged { detailsPx = it.height }
                    .padding(horizontal = ProfileSideMargin)
                    .padding(bottom = DetailsBottomPadding)
                    .offset(y = -AvatarOverlap),
            ) {
                // The sticky header lies over this room while it is open.
                Spacer(Modifier.height(with(density) { (stickyHeaderPx - headerTopPx).coerceAtLeast(0).toDp() }))
                about.description?.let { description ->
                    Text(
                        text = description,
                        modifier = Modifier.padding(top = 16.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                if (about.links.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        about.links.forEach { link -> ProfileLink(link) { openProfileLink(context, link.url) } }
                    }
                }
                ChannelProfileTabs(
                    channelLogin = about.login,
                    backEnabled = backEnabled,
                    onOpenChannel = onOpenChannel,
                    onOpenVideo = onOpenVideo,
                    firstTabRequest = firstTabRequest,
                    aboutContent = {
                        ChannelAboutPanels(
                            userId = about.userId,
                            onOpenOnTwitch = { openTwitchAboutInBrowser(context, about.login, aboutBrowserHeightPx) },
                        )
                    },
                    minHeight = tabsMinHeight,
                    tabRowPin = { profileTabRowPinPx(scrollState.value, barPx, tabRowTopPx) },
                    modifier = Modifier.padding(top = 16.dp).onSizeChanged { tabsPx = it.height },
                )
            }
        }
        val typography = MaterialTheme.typography
        ProfileStickyHeader(
            headerTop = BannerHeight - AvatarOverlap,
            barNameScale = typography.titleLarge.fontSize.value / typography.headlineSmall.fontSize.value,
            roomForMenu = onLogOut != null || canWhisper,
            scroll = { scrollState.value },
            avatar = { ProfileHeaderAvatar(about.login, about.displayName, about.avatarUrl) },
            name = { ProfileHeaderName(about.displayName) },
            details = {
                ProfileHeaderDetails(about.login, about.displayName, about.followerCount, about.lastLiveMillis)
            },
            modifier = Modifier.onSizeChanged { stickyHeaderPx = it.height },
        )
        onLogOut?.let { logOut ->
            AccountMenu(
                onLogOut = logOut,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            )
        }
        if (canWhisper) {
            ChannelProfileMenu(
                login = about.login,
                displayName = about.displayName,
                userId = about.userId,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            )
        }
    }
}

private val BannerHeight = 120.dp

/** The room on each side of the profile's text; the tab row reaches past it to the screen's edges. */
internal val ProfileSideMargin = 24.dp

/** How far the avatar row reaches up over the banner. */
private val AvatarOverlap = 48.dp

private val DetailsBottomPadding = 24.dp

internal fun formatFollowerCount(count: Int, locale: Locale): String =
    NumberFormat.getIntegerInstance(locale).format(count)
