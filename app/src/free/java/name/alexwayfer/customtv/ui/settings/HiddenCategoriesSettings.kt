package name.alexwayfer.customtv.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.TwitchCategory

/** Hiding categories is a Premium feature: the free settings have no page for it, so this title is never shown. */
internal val HiddenCategoriesSettingsTitle = R.string.settings

/** The hidden categories row shows locked, and a tap shows the Premium banner. */
@Composable
internal fun HiddenCategoriesSettingsEntry(onOpen: () -> Unit) {
    SettingsLockedEntry(
        icon = painterResource(R.drawable.ic_visibility_off),
        title = stringResource(R.string.settings_hidden_categories),
        source = "the hidden categories settings",
    )
}

@Composable
internal fun HiddenCategoriesSettingsPage(
    onOpenCategory: (TwitchCategory) -> Unit,
    modifier: Modifier = Modifier,
) = Unit
