package name.alexwayfer.customtv.ui.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.components.rememberChannelProfile

/** Home, About, and Chat. The Videos tab and recording playback are Premium. */
@Composable
internal fun ChannelProfileTabs(
    channelLogin: String,
    backEnabled: Boolean,
    onOpenChannel: (String) -> Unit,
    onOpenVideo: (ProfileVideoPlayback) -> Unit,
    aboutContent: @Composable ColumnScope.() -> Unit,
    firstTabRequest: Int,
    minHeight: Dp,
    tabRowPin: () -> Int,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable(channelLogin) { mutableIntStateOf(PROFILE_TAB_FIRST) }
    LaunchedEffect(firstTabRequest) { selectedTab = PROFILE_TAB_FIRST }
    val backTab = profileTabAfterBack(selectedTab)
    BackHandler(enabled = backEnabled && backTab != null) {
        backTab?.let { selectedTab = it }
    }
    val profile = rememberChannelProfile(channelLogin)
    val recordings = rememberChannelRecordings(channelLogin)
    ProfileTabPager(
        tabTitles = listOf(R.string.profile_home, R.string.profile_about),
        selectedTab = selectedTab,
        onSelectTab = { selectedTab = it },
        onOpenChat = { onOpenChannel(channelLogin) },
        minHeight = minHeight,
        tabRowPin = tabRowPin,
        modifier = modifier,
    ) { tab ->
        if (tab == PROFILE_TAB_HOME) {
            ChannelProfileHome(
                profile = profile,
                recordings = recordings.recordings,
                onOpenChannel = onOpenChannel,
                onOpenVideo = onOpenVideo,
            )
        } else {
            aboutContent()
        }
    }
}
