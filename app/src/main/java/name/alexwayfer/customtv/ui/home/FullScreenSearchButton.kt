package name.alexwayfer.customtv.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import name.alexwayfer.customtv.ui.theme.TwitchText

/** The shared search entry point, whose measured position is where the full-screen surface opens and closes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FullScreenSearchButton(
    state: SearchBarState,
    description: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onOpen, modifier = modifier.onGloballyPositioned { state.collapsedCoords = it }) {
        Icon(Icons.Filled.Search, contentDescription = description, tint = TwitchText)
    }
}
