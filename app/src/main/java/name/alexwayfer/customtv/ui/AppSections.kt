package name.alexwayfer.customtv.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import name.alexwayfer.customtv.data.TwitchCategory
import name.alexwayfer.customtv.ui.account.AccountLoginScreen
import name.alexwayfer.customtv.ui.account.AccountScreen
import name.alexwayfer.customtv.ui.account.ChannelAboutScreen
import name.alexwayfer.customtv.ui.explore.ExploreSection
import name.alexwayfer.customtv.ui.home.HomeChannelTarget
import name.alexwayfer.customtv.ui.home.HomeScreen
import name.alexwayfer.customtv.ui.home.homeChannelTarget
import name.alexwayfer.customtv.ui.settings.SettingsScreen
import name.alexwayfer.customtv.ui.settings.SettingsScrollTarget
import name.alexwayfer.customtv.ui.theme.TwitchBg
import name.alexwayfer.customtv.ui.watch.OneShotRequest
import name.alexwayfer.customtv.ui.whispers.WhispersSection

/** Home and the navigation bar or rail; Home stays under the full player while it covers the screen. */
@Composable
internal fun AppHomeLayer(scene: AppScene) {
    val shell = scene.shell
    Row(Modifier.fillMaxSize()) {
        if (scene.layout.railShown) {
            AppNavigation(scene, Modifier.onSizeChanged { shell.navigationRailWidthPx = it.width })
        }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val homeUnderFullPlayer = scene.watching && !shell.player.liveMinimized && shell.fullPlayerCovering
                if (shell.section == AppSection.Home || homeUnderFullPlayer) {
                    // Home leaves composition under another section; this keeps its scroll position for the way back.
                    scene.homeState.SaveableStateProvider(AppSection.Home) { AppHome(scene) }
                }
            }
            if (scene.layout.bottomBarShown) {
                AppNavigation(scene, Modifier.onSizeChanged { shell.navigationBarHeightPx = it.height })
            }
        }
    }
}

@Composable
private fun AppNavigation(scene: AppScene, modifier: Modifier) {
    val shell = scene.shell
    AppNavigationBar(
        rail = scene.layout.navigationRail,
        section = shell.section,
        account = scene.account,
        onHome = {
            if (navigationTapReselects(shell.section, AppSection.Home)) shell.homeListToTop = OneShotRequest(Unit)
            shell.section = AppSection.Home
        },
        onExplore = {
            if (navigationTapReselects(shell.section, AppSection.Explore)) {
                shell.exploreReselected = OneShotRequest(Unit)
            }
            shell.section = AppSection.Explore
        },
        onWhispers = scene::openWhispers,
        onAccount = { shell.section = AppSection.Account },
        onLogIn = scene::logIn,
        onSettings = {
            if (navigationTapReselects(shell.section, AppSection.Settings)) {
                shell.settingsReselected = OneShotRequest(Unit)
            }
            shell.section = AppSection.Settings
        },
        modifier = modifier,
    )
}

@Composable
private fun AppHome(scene: AppScene) {
    val shell = scene.shell
    val accountViewModel = scene.accountViewModel
    val player = shell.player
    HomeScreen(
        viewModel = scene.homeViewModel,
        homeVisible = shell.section == AppSection.Home && (!scene.watching || (player.liveMinimized && !scene.inPip)),
        signedIn = scene.account != null,
        followsReady = scene.accountState.followsReady,
        signedInUserId = scene.account?.userId,
        followAccess = accountViewModel.session::userAccess,
        earlyFollowAccess = accountViewModel.session::freshSavedAccess,
        onFollowPermissionRejected = accountViewModel::rejectForNewPermissions,
        scrollListToTop = shell.homeListToTop,
        padForIme = homeUsesImePadding(shell.keepNavigationBarWhileImeCloses, shell.homeSearchOpen),
        onSearchOpenChange = { shell.homeSearchOpen = it },
        onOpenCategory = scene::openExploreCategory,
        onWatch = { channel -> scene.openChannel(channel, true) },
        onOpenProfile = { channel -> scene.openProfileFrom(AppSection.Home, channel) },
        onLogIn = scene::logIn,
    )
}

/** Opens [channel]'s profile from a section at the top of the app, which Back from the profile returns to. */
private fun AppScene.openProfileFrom(section: AppSection, channel: String) {
    homeViewModel.addRecent(channel)
    shell.profileChannel = channel
    shell.profileOpenedFrom = section
    shell.section = AppSection.ChannelProfile
}

/** Opens Explore on its categories, as a category's tag filtered them from a profile. */
private fun AppScene.showExploreCategories() {
    shell.exploreShowCategories = OneShotRequest(Unit)
    shell.section = AppSection.Explore
}

/** Opens [category] in Explore, over its categories, as the search picked it from Home or from Explore itself. */
private fun AppScene.openExploreCategory(category: TwitchCategory) {
    shell.exploreOpenCategory = OneShotRequest(category)
    shell.section = AppSection.Explore
}

/**
 * The sections over Home: the whispers, the account, a channel's profile, and the settings. A [hidden] section stays
 * composed past the right edge, under picture-in-picture.
 */
@Composable
internal fun AppSections(scene: AppScene, hidden: Boolean) {
    val shell = scene.shell
    val layout = scene.layout
    val density = LocalDensity.current
    AnimatedContent(
        targetState = shell.section,
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = if (layout.railShown) with(density) { shell.navigationRailWidthPx.toDp() } else 0.dp,
                bottom = if (layout.bottomBarShown) with(density) { shell.navigationBarHeightPx.toDp() } else 0.dp,
            )
            // The app's bar covers the system's, so a screen's own Scaffold must not keep clear of it again.
            .then(if (layout.bottomBarShown) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier)
            .then(if (shell.section == AppSection.Home) Modifier.clearAndSetSemantics {} else Modifier)
            .offscreenWhen(hidden, layout.screenWidthPx),
        contentAlignment = Alignment.TopStart,
        transitionSpec = {
            if (targetState == AppSection.ChannelProfile) {
                ContentTransform(EnterTransition.None, ExitTransition.None)
            } else {
                ContentTransform(
                    fadeIn(tween(SECTION_FADE_MS)),
                    fadeOut(tween(SECTION_FADE_MS)),
                    sizeTransform = null,
                )
            }
        },
        label = "app section",
    ) { page ->
        if (page != AppSection.Home) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(TwitchBg),
            ) {
                AppSectionPage(scene, page)
            }
        }
    }
}

@Composable
private fun AppSectionPage(scene: AppScene, page: AppSection) {
    val shell = scene.shell
    if (page == AppSection.Whispers) {
        WhispersSection(
            onClosed = {
                shell.section = shell.sectionBeforeWhispers
                if (shell.expandPlayerAfterWhispers) shell.player = shell.player.returnedFromWhispers(scene.watching)
                shell.expandPlayerAfterWhispers = false
            },
            onOpenChannel = { channel ->
                shell.profileChannel = channel
                shell.profileOpenedFrom = AppSection.Whispers
                shell.section = AppSection.ChannelProfile
            },
            // As the navigation's Settings does, at the whispers' block.
            onOpenSettings = {
                shell.settingsScrollTarget = SettingsScrollTarget.Whispers
                shell.section = AppSection.Settings
            },
        )
    }
    if (page == AppSection.Explore) {
        // Kept like Home's, so the list is where it was after another section.
        scene.homeState.SaveableStateProvider(AppSection.Explore) { ExploreSectionPage(scene) }
    }
    if (page == AppSection.Account) AccountSectionPage(scene)
    if (page == AppSection.ChannelProfile) {
        shell.profileChannel?.let { channel -> ProfileSectionPage(scene, channel) }
    }
    if (page == AppSection.Settings) {
        SettingsScreen(
            onClose = { shell.section = AppSection.Home },
            signedIn = scene.account != null,
            accountName = scene.account?.displayName,
            reselected = shell.settingsReselected,
            scrollTo = shell.settingsScrollTarget,
            onScrolled = { shell.settingsScrollTarget = null },
        )
    }
}

@Composable
private fun ExploreSectionPage(scene: AppScene) {
    val shell = scene.shell
    ExploreSection(
        homeViewModel = scene.homeViewModel,
        signedIn = scene.account != null,
        // A full stream over Explore minimizes on Back first, as over a profile.
        backEnabled = profileHandlesBack(scene.watching, shell.player.liveMinimized),
        reselected = shell.exploreReselected,
        showCategories = shell.exploreShowCategories,
        openCategory = shell.exploreOpenCategory,
        onBack = { shell.section = AppSection.Home },
        onOpen = { channel, live ->
            when (homeChannelTarget(live)) {
                HomeChannelTarget.Stream -> scene.openChannel(channel, true)
                HomeChannelTarget.Profile -> scene.openProfileFrom(AppSection.Explore, channel)
            }
        },
        onOpenCategory = scene::openExploreCategory,
        onSearchOpenChange = { shell.homeSearchOpen = it },
    )
}

@Composable
private fun AccountSectionPage(scene: AppScene) {
    val shell = scene.shell
    val accountViewModel = scene.accountViewModel
    val signedIn = scene.account
    val accountPage = accountSectionPage(scene.accountState.loggingIn, signedIn = signedIn != null)
    if (accountPage == AccountSectionPage.LoginLoader) {
        val reopenShown by accountViewModel.deviceLogin.reopen.shown.collectAsStateWithLifecycle()
        AccountLoginScreen(
            reopenShown = reopenShown && scene.accountState.loginPrompt != null,
            onReopen = { shell.loginPageReopen = OneShotRequest(Unit) },
            onCancel = accountViewModel.deviceLogin::cancel,
        )
    }
    if (accountPage == AccountSectionPage.Profile && signedIn != null) {
        AccountScreen(
            account = signedIn,
            onLeave = { shell.section = AppSection.Home },
            onLogOut = {
                accountViewModel.logOut()
                shell.section = AppSection.Home
            },
            onOpenChannel = { channel -> scene.openChannel(channel, false) },
            onOpenVideo = scene::openProfileVideo,
            onShowCategories = scene::showExploreCategories,
        )
    }
}

@Composable
private fun ProfileSectionPage(scene: AppScene, channel: String) {
    val shell = scene.shell
    val account = scene.account
    ChannelAboutScreen(
        channel = channel,
        // Whispers go to another person's channel, from a logged-in account.
        canWhisper = account != null && !channel.equals(account.login, ignoreCase = true),
        backEnabled = profileHandlesBack(scene.watching, shell.player.liveMinimized),
        onLeave = {
            when (val back = profileBack(shell.profileOpenedFrom, scene.watching, shell.player.liveMinimized)) {
                ProfileBack.ExpandPlayer -> {
                    shell.profileClosesUnderPlayer = true
                    shell.player = shell.player.withLiveMinimized(false)
                }
                is ProfileBack.Show -> shell.section = back.section
            }
            shell.profileOpenedFrom = null
        },
        onOpenChannel = { target -> scene.openChannel(target, false) },
        onOpenVideo = scene::openProfileVideo,
        onShowCategories = scene::showExploreCategories,
        firstTabRequest = shell.profileFirstTabRequest,
    )
}

private const val SECTION_FADE_MS = 200
