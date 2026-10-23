package name.alexwayfer.customtv.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.AppSettings
import name.alexwayfer.customtv.data.ChatterLabelsRepository
import name.alexwayfer.customtv.data.TwitchCategory
import name.alexwayfer.customtv.ui.notifications.SettingsNotificationPrompt
import name.alexwayfer.customtv.ui.watch.OneShotRequest

@Composable
internal fun SettingsScreen(
    onClose: () -> Unit,
    onOpenCategory: (TwitchCategory) -> Unit,
    signedIn: Boolean,
    accountName: String?,
    reselected: OneShotRequest<Unit>?,
    viewModel: SettingsViewModel = viewModel(),
    scrollTo: SettingsScrollTarget? = null,
    onScrolled: () -> Unit = {},
) {
    // The main list keeps its place while a subpage is open; a subpage opens at its top.
    val mainScroll = rememberScrollState()
    var page by rememberSaveable { mutableStateOf(scrollTo?.let(::settingsPageFor) ?: SettingsPage.Main) }
    LaunchedEffect(scrollTo) {
        if (scrollTo == null) return@LaunchedEffect
        page = settingsPageFor(scrollTo)
        // A page that holds the target's block reports once it has scrolled there; a whole subpage opens at its top.
        if (!settingsScrollsToBlock(scrollTo)) onScrolled()
    }
    val body = storedSettingsBody(viewModel)
    val leaveGuard = remember { SettingsLeaveGuard() }
    val backToMain = { leaveGuard.leave { page = SettingsPage.Main } }
    LaunchedEffect(reselected) {
        if (reselected?.take() == null) return@LaunchedEffect
        when (settingsReselect(page)) {
            SettingsReselect.BackToMain -> backToMain()
            SettingsReselect.ScrollToTop -> mainScroll.animateScrollTo(0)
        }
    }
    BackHandler(onBack = onClose)
    BackHandler(enabled = page != SettingsPage.Main, onBack = backToMain)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SettingsTopBar(page, onBack = backToMain) },
    ) { padding ->
        AnimatedContent(targetState = page, transitionSpec = { settingsPageTransition() }) { current ->
            // The keyboard's inset already holds the navigation bar the Scaffold padded for; without consuming it
            // the bar's height showed twice, as a black strip over the keyboard.
            val pageModifier = Modifier.padding(padding).consumeWindowInsets(padding)
            when (current) {
                SettingsPage.Main -> SettingsPageColumn(
                    scrollTo = scrollTo?.takeIf { settingsPageFor(it) == SettingsPage.Main },
                    onScrolled = onScrolled,
                    modifier = pageModifier,
                    scroll = mainScroll,
                ) { anchor ->
                    SettingsMainEntries(anchor, accountName, onOpen = { page = it })
                }
                SettingsPage.HiddenCategories -> HiddenCategoriesSettingsPage(onOpenCategory, pageModifier)
                SettingsPage.Gestures -> GesturesSettingsPage(pageModifier)
                SettingsPage.Licenses -> LicensesSettingsPage(pageModifier)
                // A subpage shows once every stored value is read, so no control animates from a placeholder.
                // The labels page lays out its own column, with Save floating over it.
                SettingsPage.ChatterLabels -> if (body != null) {
                    ChatterLabelsSettings(body.labelSeed, leaveGuard, pageModifier)
                }
                else -> if (body != null) {
                    SettingsPageColumn(
                        scrollTo = scrollTo?.takeIf { settingsPageFor(it) == current && settingsScrollsToBlock(it) },
                        onScrolled = onScrolled,
                        modifier = pageModifier,
                    ) { anchor ->
                        SettingsSubpage(current, body.settings, signedIn, accountName, viewModel, anchor)
                    }
                }
            }
        }
    }
}

/** The settings and the Gist fields once both are read, or null until then. */
@Composable
private fun storedSettingsBody(viewModel: SettingsViewModel): SettingsBody? {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val savedCredentials by ChatterLabelsRepository.credentials.collectAsStateWithLifecycle()
    val credentialsLoaded by ChatterLabelsRepository.credentialsLoaded.collectAsStateWithLifecycle()
    return settingsBody(
        animatedControlValue(settings),
        animatedControlValue(
            if (credentialsLoaded) {
                chatterLabelFieldSeed(savedCredentials?.token, savedCredentials?.gistId)
            } else {
                null
            },
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(page: SettingsPage, onBack: () -> Unit) {
    TopAppBar(
        title = {
            AnimatedContent(targetState = page, transitionSpec = { settingsPageTransition() }) { current ->
                Text(stringResource(current.titleRes()))
            }
        },
        navigationIcon = {
            // Grows from no width, so the title slides aside instead of jumping.
            AnimatedVisibility(
                visible = page != SettingsPage.Main,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally(),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.navigate_back),
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

/** The list of subpages and the sections that sit on the main page itself. */
@Composable
private fun SettingsMainEntries(anchor: SettingsScrollAnchor, accountName: String?, onOpen: (SettingsPage) -> Unit) {
    SettingsNotificationPrompt()
    SettingsPageEntry(
        icon = painterResource(R.drawable.ic_chat),
        title = stringResource(R.string.settings_page_chat),
        hint = stringResource(R.string.settings_page_chat_hint),
        onOpen = { onOpen(SettingsPage.Chat) },
    )
    SettingsPageEntry(
        icon = painterResource(R.drawable.ic_alternate_email),
        title = stringResource(R.string.settings_page_mentions),
        hint = stringResource(R.string.settings_page_mentions_hint),
        onOpen = { onOpen(SettingsPage.Mentions) },
    )
    NotificationSettingsEntry(onOpen = { onOpen(SettingsPage.Notifications) })
    SettingsPageEntry(
        icon = rememberVectorPainter(Icons.Outlined.PlayArrow),
        title = stringResource(R.string.settings_section_playback),
        hint = stringResource(R.string.settings_page_playback_hint),
        onOpen = { onOpen(SettingsPage.Playback) },
    )
    HiddenCategoriesSettingsEntry(onOpen = { onOpen(SettingsPage.HiddenCategories) })
    ChatterPortraitSettingsEntry(onOpen = { onOpen(SettingsPage.ChatterPortraits) })
    ChatterLabelsSettingsEntry(onOpen = { onOpen(SettingsPage.ChatterLabels) })
    ExperimentalSettingsEntry(accountName, onOpen = { onOpen(SettingsPage.Experimental) })
    Column(modifier = Modifier.settingsScrollAnchor(anchor)) {
        UpdateSettings()
    }
    PremiumSettings()
    LanguageSettings()
    AppLinksSettings()
    SettingsSection(stringResource(R.string.settings_section_help))
    GesturesSettingsEntry(onOpen = { onOpen(SettingsPage.Gestures) })
    LicensesSettingsEntry(onOpen = { onOpen(SettingsPage.Licenses) })
    ProblemReportSettings()
}

/** A subpage laid out in the common column; the main, gestures, licenses, and labels pages lay out their own. */
@Composable
private fun SettingsSubpage(
    page: SettingsPage,
    settings: AppSettings,
    signedIn: Boolean,
    accountName: String?,
    viewModel: SettingsViewModel,
    anchor: SettingsScrollAnchor,
) {
    when (page) {
        SettingsPage.Chat -> ChatSettings(settings, viewModel)
        SettingsPage.Mentions -> MentionSettings(settings, viewModel)
        SettingsPage.Notifications -> NotificationSettings(settings, signedIn, viewModel, anchor)
        SettingsPage.Playback -> PlaybackSettings(settings, viewModel)
        SettingsPage.ChatterPortraits -> ChatterPortraitSettings(settings, viewModel)
        SettingsPage.Experimental -> ExperimentalSettings(accountName, settings, viewModel)
        SettingsPage.Main, SettingsPage.Gestures, SettingsPage.Licenses, SettingsPage.ChatterLabels,
        SettingsPage.HiddenCategories -> Unit
    }
}

@Composable
internal fun SettingsSection(title: String, first: Boolean = false) {
    Text(
        text = title,
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            top = if (first) 8.dp else 20.dp,
            bottom = 4.dp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.titleSmall,
    )
}

@Composable
internal fun SettingsToggle(
    label: String,
    hint: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
    ) {
        ListItem(
            headlineContent = { Text(label) },
            supportingContent = hint?.let { { Text(it) } },
            trailingContent = {
                Switch(
                    checked = checked,
                    onCheckedChange = null,
                    enabled = enabled,
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}
