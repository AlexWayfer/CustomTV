package name.alexwayfer.customtv.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.home.FullScreenSearchButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsTopBar(
    page: SettingsPage,
    onBack: () -> Unit,
    searchBar: SearchBarState,
    onSearch: () -> Unit,
    showBack: Boolean,
) {
    TopAppBar(
        title = {
            AnimatedContent(targetState = page, transitionSpec = { settingsPageTransition() }) { current ->
                val title = stringResource(current.titleRes())
                Text(title, modifier = Modifier.settingsSearchAnchor(title))
            }
        },
        navigationIcon = {
            AnimatedVisibility(
                visible = showBack,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally(),
            ) {
                SettingsBackButton(onBack)
            }
        },
        actions = {
            FullScreenSearchButton(searchBar, stringResource(R.string.settings_search), onSearch)
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
private fun SettingsBackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.navigate_back))
    }
}
