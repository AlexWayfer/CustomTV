package name.alexwayfer.customtv.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import name.alexwayfer.customtv.data.RecentChannel
import name.alexwayfer.customtv.ui.account.TwitchUserAccess
import kotlin.time.Duration.Companion.milliseconds

/**
 * Reads the saved profiles of the shown recents, and refreshes the recents while Home is in sight with the app in
 * front: at once, then every [RECENT_AUTO_REFRESH_INTERVAL_MS].
 */
@Composable
internal fun RecentsAutoRefresh(
    viewModel: HomeViewModel,
    homeVisible: Boolean,
    appInForeground: Boolean,
    recents: List<RecentChannel>,
    signedIn: Boolean,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var wasInForeground by remember(lifecycleOwner) { mutableStateOf(false) }
    var firstHomeRefresh by remember { mutableStateOf(true) }

    LaunchedEffect(homeVisible, recents) {
        if (homeVisible) viewModel.rememberCachedProfiles()
    }

    LaunchedEffect(homeVisible, appInForeground, recents, signedIn) {
        val returnedToForeground = appInForeground && !wasInForeground
        wasInForeground = appInForeground
        if (!homeVisible) firstHomeRefresh = false
        if (!homeVisible || !appInForeground || !refreshRecentsInBackground(signedIn, recents.isNotEmpty())) {
            return@LaunchedEffect
        }
        viewModel.refreshRecents(
            showProgress = showAutomaticRecentRefreshProgress(firstHomeRefresh, returnedToForeground),
            automatic = true,
        )
        firstHomeRefresh = false
        while (isActive) {
            delay(RECENT_AUTO_REFRESH_INTERVAL_MS.milliseconds)
            viewModel.refreshRecents(showProgress = false, automatic = true)
        }
    }
}

/**
 * Loads the whole follow list while [active] (signed in, Home in sight, the app in front), with the [earlyAccess]
 * when it is at hand and with the refresh shown while it waits for access otherwise, then its live channels every
 * [FOLLOWED_AUTO_REFRESH_INTERVAL_MS] until the login is refused.
 */
@Composable
internal fun FollowsAutoRefresh(
    follows: HomeFollows,
    active: Boolean,
    userId: String?,
    access: suspend () -> TwitchUserAccess?,
    earlyAccess: () -> TwitchUserAccess?,
) {
    LaunchedEffect(active, userId) {
        if (!active) return@LaunchedEffect
        val early = earlyAccess()
        if (early != null) {
            follows.refresh(early.accessToken, early.userId, liveOnly = false, automatic = true)
        }
        val current = if (early == null) follows.awaitAccess(userId, access) else access()
        if (current == null) {
            if (follows.channels.value.isEmpty()) follows.markUnavailable()
            return@LaunchedEffect
        }
        if (early == null) {
            follows.refresh(current.accessToken, current.userId, liveOnly = false, automatic = true)
        }
        while (isActive) {
            delay(FOLLOWED_AUTO_REFRESH_INTERVAL_MS.milliseconds)
            val status = follows.status.value
            val loginRefused = status == FollowedChannelsStatus.NeedsPermission ||
                status == FollowedChannelsStatus.Rejected
            val next = (if (loginRefused) null else access()) ?: break
            follows.refresh(next.accessToken, next.userId, liveOnly = true)
        }
    }
}
