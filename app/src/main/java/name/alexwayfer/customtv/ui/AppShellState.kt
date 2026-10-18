package name.alexwayfer.customtv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import name.alexwayfer.customtv.ui.settings.SettingsScrollTarget
import name.alexwayfer.customtv.ui.watch.OneShotRequest

@Composable
internal fun rememberAppShellState(): AppShellState {
    val player = rememberSaveable(stateSaver = PlayerSession.Saver) { mutableStateOf(PlayerSession()) }
    val section = rememberSaveable { mutableStateOf(AppSection.Home) }
    val profileChannel = rememberSaveable { mutableStateOf<String?>(null) }
    val profileOpenedFrom = rememberSaveable { mutableStateOf<AppSection?>(null) }
    val awaitingLogin = rememberSaveable { mutableStateOf(false) }
    val returnToStreamAfterLogin = rememberSaveable { mutableStateOf(false) }
    return remember {
        AppShellState(player, section, profileChannel, profileOpenedFrom, awaitingLogin, returnToStreamAfterLogin)
    }
}

/**
 * The app's navigation and player state: the open section, the players, the login under way, and the room the
 * navigation bar takes. The values a restart keeps come in saved.
 */
@Stable
internal class AppShellState(
    savedPlayer: MutableState<PlayerSession>,
    savedSection: MutableState<AppSection>,
    savedProfileChannel: MutableState<String?>,
    savedProfileOpenedFrom: MutableState<AppSection?>,
    savedAwaitingLogin: MutableState<Boolean>,
    savedReturnToStreamAfterLogin: MutableState<Boolean>,
) {
    var player by savedPlayer
    var section by savedSection
    var profileChannel by savedProfileChannel
    var profileOpenedFrom by savedProfileOpenedFrom
    var awaitingLogin by savedAwaitingLogin
    var returnToStreamAfterLogin by savedReturnToStreamAfterLogin

    var channelLive by mutableStateOf(false)
    var wasInPip by mutableStateOf(false)
    var watchedThisSession by mutableStateOf(false)
    var pipResumeChecked by mutableStateOf(false)

    /** Until the start decides whether it reopens the channel left in picture-in-picture. */
    var startChannelPending by mutableStateOf(true)
    var settingsScrollTarget by mutableStateOf<SettingsScrollTarget?>(null)
    var profileFirstTabRequest by mutableIntStateOf(0)
    var homeListToTop by mutableStateOf<OneShotRequest<Unit>?>(null)
    var exploreReselected by mutableStateOf<OneShotRequest<Unit>?>(null)

    /** Explore is to show its categories, such as after a category's tag in a profile filtered them. */
    var exploreShowCategories by mutableStateOf<OneShotRequest<Unit>?>(null)

    /** Explore is to open this category over its categories, such as one picked in the search or the player's. */
    var exploreOpenCategory by mutableStateOf<OneShotRequest<ExploreCategoryOpen>?>(null)

    /** Explore is to close its open category, which Back left under the player expanding over it. */
    var exploreCloseCategory by mutableStateOf<OneShotRequest<Unit>?>(null)

    /** The section the player covered as its category opened in Explore; Back from that category returns to it. */
    var categoryPlayerSection by mutableStateOf(AppSection.Home)

    /** Back from the player's category expands the player, and the category closes once the player covers it. */
    var exploreCategoryClosesUnderPlayer by mutableStateOf(false)
    var settingsReselected by mutableStateOf<OneShotRequest<Unit>?>(null)
    var openPlayerFromHome by mutableStateOf(true)
    var sectionBeforeWhispers by mutableStateOf(AppSection.Home)
    var expandPlayerAfterWhispers by mutableStateOf(false)
    var keepNavigationBarWhileImeCloses by mutableStateOf(false)
    var homeSearchOpen by mutableStateOf(false)
    var fullPlayerCovering by mutableStateOf(false)
    var profileClosesUnderPlayer by mutableStateOf(false)
    var navigationBarHeightPx by mutableIntStateOf(0)
    var navigationRailWidthPx by mutableIntStateOf(0)
    var loginPageReopen by mutableStateOf<OneShotRequest<Unit>?>(null)
}
