package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.MAX_CHAT_TEXT_SIZE
import name.alexwayfer.customtv.chat.MIN_CHAT_TEXT_SIZE
import name.alexwayfer.customtv.chat.chatTextSizeFromStored
import name.alexwayfer.customtv.ui.settings.SettingsStepSlider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatSettingsSheet(
    channelLogin: String,
    browserSheetHeightPx: () -> Int,
    readableColors: Boolean,
    timestamps: Boolean,
    textSize: Int,
    smoothChatScroll: Boolean,
    onReadableColorsChange: (Boolean) -> Unit,
    onTimestampsChange: (Boolean) -> Unit,
    onTextSizeChange: (Int) -> Unit,
    onSmoothChatScrollChange: (Boolean) -> Unit,
    onRefreshEmotes: () -> Job,
    onRefreshLabels: () -> Job,
    /** Null when logged out: there is no own profile to show. */
    onOpenOwnProfile: (() -> Unit)?,
    /** Null when the channel has no rules to show. */
    onViewChatRules: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    DismissWhenPlayerContentHidden(onDismiss)
    val context = LocalContext.current
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    // A drag back up through the settings stops at the top; closing the sheet takes a new drag.
    val overscroll = rememberOverscrollEffect()
    val stopAtTop = rememberStopAtScrollTop(scrollState, overscroll)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                // The sheet stops at the player, so the stream stays in sight.
                .heightIn(max = chatSheetContentMaxHeight())
                .nestedScroll(stopAtTop)
                .verticalScroll(scrollState, overscrollEffect = overscroll)
                .padding(bottom = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.chat_menu),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            val browserRows = chatBrowserRows(experimentalLoginOffer(), loggedIn = onOpenOwnProfile != null)
            AnimatedVisibility(visible = browserRows.openInBrowser, enter = expandVertically(), exit = shrinkVertically()) {
                ChatSettingsRow(
                    label = stringResource(R.string.open_chat_in_browser),
                    supportingText = stringResource(R.string.open_chat_in_browser_hint),
                    modifier = Modifier.clickable {
                        openTwitchChatInBrowser(context, channelLogin, browserSheetHeightPx())
                    },
                )
            }
            AnimatedVisibility(visible = browserRows.experimentalLogin, enter = expandVertically(), exit = shrinkVertically()) {
                ExperimentalLoginChatRow(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    },
                )
            }
            if (onOpenOwnProfile != null) {
                ChatSettingsRow(
                    label = stringResource(R.string.chat_own_profile),
                    modifier = Modifier.clickable {
                        scope.launch {
                            sheetState.hide()
                            onOpenOwnProfile()
                        }
                    },
                )
            }
            if (onViewChatRules != null) {
                ChatSettingsRow(
                    label = stringResource(R.string.view_chat_rules),
                    modifier = Modifier.clickable {
                        scope.launch {
                            sheetState.hide()
                            onViewChatRules()
                        }
                    },
                )
            }
            ChatSettingsToggle(
                label = stringResource(R.string.readable_colors),
                checked = readableColors,
                onCheckedChange = onReadableColorsChange,
            )
            ChatSettingsToggle(
                label = stringResource(R.string.timestamps),
                checked = timestamps,
                onCheckedChange = onTimestampsChange,
            )
            SettingsStepSlider(
                title = stringResource(R.string.chat_text_size),
                valueText = { it.toString() },
                value = textSize,
                valueRange = MIN_CHAT_TEXT_SIZE.toFloat()..MAX_CHAT_TEXT_SIZE.toFloat(),
                steps = MAX_CHAT_TEXT_SIZE - MIN_CHAT_TEXT_SIZE - 1,
                resolve = ::chatTextSizeFromStored,
                onChange = onTextSizeChange,
                onPreview = {},
            )
            SmoothChatScrollSettings(
                enabled = smoothChatScroll,
                onChange = onSmoothChatScrollChange,
            )
            ChatSettingsRow(
                label = stringResource(R.string.refresh_emotes_and_labels),
                modifier = Modifier.clickable {
                    if (!refreshing) {
                        refreshing = true
                        scope.launch {
                            try {
                                joinAll(onRefreshEmotes(), onRefreshLabels())
                            } finally {
                                if (isActive) {
                                    sheetState.hide()
                                    onDismiss()
                                }
                            }
                        }
                    }
                },
                trailing = {
                    AnimatedVisibility(visible = refreshing, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                },
            )
        }
    }
}

@Composable
internal fun ChatSettingsToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ChatSettingsRow(
        label = label,
        modifier = Modifier.toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = null,
            )
        },
    )
}

@Composable
internal fun ChatSettingsRow(
    label: String,
    modifier: Modifier,
    supportingText: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
    ) {
        ListItem(
            headlineContent = { Text(label) },
            supportingContent = supportingText?.let { text -> { Text(text) } },
            trailingContent = trailing,
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}
