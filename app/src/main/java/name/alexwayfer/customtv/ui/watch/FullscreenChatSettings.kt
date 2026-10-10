package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChatSettings
import name.alexwayfer.customtv.data.FullscreenChatMode
import name.alexwayfer.customtv.data.FullscreenChatSide
import name.alexwayfer.customtv.data.GestureHint
import name.alexwayfer.customtv.ui.LocalGestureHints

/** The gear in the full screen player's top corner; opens the full screen chat settings. */
@Composable
internal fun BoxScope.PlayerFullscreenChatButton(visible: Boolean) {
    var sheetOpen by remember { mutableStateOf(false) }
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = FullscreenTopButtonsTop),
        enter = HeaderFadeIn,
        exit = HeaderFadeOut,
    ) {
        IconButton(
            onClick = { sheetOpen = true },
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.fullscreen_chat_settings),
                tint = Color.White,
            )
        }
    }
    if (sheetOpen) {
        FullscreenChatSettingsSheet(onDismiss = { sheetOpen = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullscreenChatSettingsSheet(onDismiss: () -> Unit) {
    val viewModel: ChatSettingsViewModel = viewModel()
    val stored by viewModel.settings.collectAsStateWithLifecycle()
    val settings = stored ?: ChatSettings()
    val gestureHints = LocalGestureHints.current
    val scrollState = rememberScrollState()
    // A drag back up through the settings stops at the top; closing the sheet takes a new drag.
    val overscroll = rememberOverscrollEffect()
    val stopAtTop = rememberStopAtScrollTop(scrollState, overscroll)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .nestedScroll(stopAtTop)
                .verticalScroll(scrollState, overscrollEffect = overscroll)
                .padding(bottom = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.fullscreen_chat_settings),
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    .semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
            )
            OptionGroup(title = stringResource(R.string.fullscreen_chat_mode)) {
                FullscreenChatMode.entries.forEach { mode ->
                    OptionRow(
                        label = stringResource(mode.label),
                        selected = settings.fullscreenChatMode == mode,
                        onSelect = {
                            if (mode == FullscreenChatMode.Hidden && settings.fullscreenChatMode != mode) {
                                gestureHints?.slowPathUsed(GestureHint.FullscreenChatHide)
                            }
                            viewModel.setFullscreenChatMode(mode)
                        },
                    )
                }
            }
            OptionGroup(title = stringResource(R.string.fullscreen_chat_position)) {
                FullscreenChatSide.entries.forEach { side ->
                    OptionRow(
                        label = stringResource(side.label),
                        selected = settings.fullscreenChatSide == side,
                        onSelect = {
                            if (settings.fullscreenChatSide != side) {
                                gestureHints?.slowPathUsed(GestureHint.FullscreenChatSide)
                            }
                            viewModel.setFullscreenChatSide(side)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionGroup(title: String, options: @Composable () -> Unit) {
    Text(
        text = title,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
            .semantics { heading() },
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.titleSmall,
    )
    Column(modifier = Modifier.selectableGroup()) {
        options()
    }
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = { RadioButton(selected = selected, onClick = null) },
        modifier = Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

internal val FullscreenChatMode.label: Int
    get() = when (this) {
        FullscreenChatMode.Overlay -> R.string.fullscreen_chat_overlay
        FullscreenChatMode.Column -> R.string.fullscreen_chat_column
        FullscreenChatMode.Hidden -> R.string.fullscreen_chat_hidden
    }

private val FullscreenChatSide.label: Int
    get() = when (this) {
        FullscreenChatSide.Left -> R.string.fullscreen_chat_left
        FullscreenChatSide.Right -> R.string.fullscreen_chat_right
    }
