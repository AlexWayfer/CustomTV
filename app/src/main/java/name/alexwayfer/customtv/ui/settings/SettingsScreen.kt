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
import name.alexwayfer.customtv.data.ChatterLabelsRepository
import name.alexwayfer.customtv.ui.notifications.SettingsNotificationPrompt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onClose: () -> Unit,
    signedIn: Boolean,
    accountName: String?,
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
        // A subpage is the whole section and opens at its top; only the main list scrolls to one.
        if (page != SettingsPage.Main) onScrolled()
    }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val savedCredentials by ChatterLabelsRepository.credentials.collectAsStateWithLifecycle()
    val credentialsLoaded by ChatterLabelsRepository.credentialsLoaded.collectAsStateWithLifecycle()
    val body = settingsBody(
        animatedControlValue(settings),
        animatedControlValue(
            if (credentialsLoaded) {
                chatterLabelFieldSeed(savedCredentials?.token, savedCredentials?.gistId)
            } else {
                null
            },
        ),
    )
    val leaveGuard = remember { SettingsLeaveGuard() }
    BackHandler(onBack = onClose)
    BackHandler(enabled = page != SettingsPage.Main) { leaveGuard.leave { page = SettingsPage.Main } }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
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
                        IconButton(onClick = { leaveGuard.leave { page = SettingsPage.Main } }) {
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
        },
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
                    SettingsNotificationPrompt()
                    SettingsPageEntry(
                        icon = painterResource(R.drawable.ic_chat),
                        title = stringResource(R.string.settings_page_chat),
                        hint = stringResource(R.string.settings_page_chat_hint),
                        onOpen = { page = SettingsPage.Chat },
                    )
                    SettingsPageEntry(
                        icon = painterResource(R.drawable.ic_alternate_email),
                        title = stringResource(R.string.settings_page_mentions),
                        hint = stringResource(R.string.settings_page_mentions_hint),
                        onOpen = { page = SettingsPage.Mentions },
                    )
                    NotificationSettingsEntry(onOpen = { page = SettingsPage.Notifications })
                    SettingsPageEntry(
                        icon = rememberVectorPainter(Icons.Outlined.PlayArrow),
                        title = stringResource(R.string.settings_section_playback),
                        hint = stringResource(R.string.settings_page_playback_hint),
                        onOpen = { page = SettingsPage.Playback },
                    )
                    ChatterPortraitSettingsEntry(onOpen = { page = SettingsPage.ChatterPortraits })
                    ChatterLabelsSettingsEntry(onOpen = { page = SettingsPage.ChatterLabels })
                    ExperimentalSettingsEntry(accountName, onOpen = { page = SettingsPage.Experimental })
                    Column(modifier = Modifier.settingsScrollAnchor(anchor)) {
                        TelegramSettings()
                    }
                    PremiumSettings()
                    LanguageSettings()
                    ProblemReportSettings()
                    GesturesSettingsEntry(onOpen = { page = SettingsPage.Gestures })
                }
                SettingsPage.Gestures -> GesturesSettingsPage(pageModifier)
                // A subpage shows once every stored value is read, so no control animates from a placeholder.
                // The labels page lays out its own column, with Save floating over it.
                SettingsPage.ChatterLabels -> if (body != null) {
                    ChatterLabelsSettings(body.labelSeed, leaveGuard, pageModifier)
                }
                else -> if (body != null) {
                    SettingsPageColumn(modifier = pageModifier) {
                        when (current) {
                            SettingsPage.Chat -> ChatSettings(body.settings, viewModel)
                            SettingsPage.Mentions -> MentionSettings(body.settings, viewModel)
                            SettingsPage.Notifications -> NotificationSettings(body.settings, signedIn, viewModel)
                            SettingsPage.Playback -> PlaybackSettings(body.settings, viewModel)
                            SettingsPage.ChatterPortraits -> ChatterPortraitSettings(body.settings, viewModel)
                            SettingsPage.Experimental -> ExperimentalSettings(accountName, body.settings, viewModel)
                            SettingsPage.Main, SettingsPage.Gestures, SettingsPage.ChatterLabels -> Unit
                        }
                    }
                }
            }
        }
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
