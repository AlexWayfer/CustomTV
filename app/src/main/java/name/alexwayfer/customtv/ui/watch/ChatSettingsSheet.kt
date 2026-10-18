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
import androidx.compose.foundation.layout.ColumnScope
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
import kotlinx.coroutines.CoroutineScope
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
    /** Null over a recording's or a clip's replayed chat: the site and the experimental login are for the live one. */
    browserSheetHeightPx: (() -> Int)?,
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
    /** The chat recap's row, given a way to close the sheet before its action; none over a replayed chat. */
    recapRow: @Composable (hideThen: (() -> Unit) -> Unit) -> Unit = {},
) {
    DismissWhenPlayerContentHidden(onDismiss)
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hideThen: (() -> Unit) -> Unit = { action ->
        scope.launch {
            sheetState.hide()
            action()
        }
    }
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
            ChatMenuTitle()
            ChatBrowserRows(
                channelLogin = channelLogin,
                browserSheetHeightPx = browserSheetHeightPx,
                loggedIn = onOpenOwnProfile != null,
                onExperimentalLogin = { hideThen(onDismiss) },
            )
            onOpenOwnProfile?.let { open ->
                ChatSettingsRow(stringResource(R.string.chat_own_profile), Modifier.clickable { hideThen(open) })
            }
            onViewChatRules?.let { view ->
                ChatSettingsRow(stringResource(R.string.view_chat_rules), Modifier.clickable { hideThen(view) })
            }
            recapRow(hideThen)
            ChatSettingsToggle(stringResource(R.string.readable_colors), readableColors, onReadableColorsChange)
            ChatSettingsToggle(stringResource(R.string.timestamps), timestamps, onTimestampsChange)
            ChatTextSizeSlider(textSize, onTextSizeChange)
            SmoothChatScrollSettings(
                enabled = smoothChatScroll,
                onChange = onSmoothChatScrollChange,
            )
            RefreshEmotesRow(
                scope = scope,
                refresh = { listOf(onRefreshEmotes(), onRefreshLabels()) },
                onRefreshed = {
                    sheetState.hide()
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun ChatMenuTitle() {
    Text(
        text = stringResource(R.string.chat_menu),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
        style = MaterialTheme.typography.titleLarge,
    )
}

@Composable
private fun ChatTextSizeSlider(textSize: Int, onTextSizeChange: (Int) -> Unit) {
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
}

/** Open chat in browser, or the offer of the experimental login that replaces it. */
@Composable
private fun ColumnScope.ChatBrowserRows(
    channelLogin: String,
    browserSheetHeightPx: (() -> Int)?,
    loggedIn: Boolean,
    onExperimentalLogin: () -> Unit,
) {
    val context = LocalContext.current
    val browserRows = chatBrowserRows(
        experimentalLoginOffer(),
        loggedIn = loggedIn,
        liveChat = browserSheetHeightPx != null,
    )
    AnimatedVisibility(
        visible = browserRows.openInBrowser,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        ChatSettingsRow(
            label = stringResource(R.string.open_chat_in_browser),
            supportingText = stringResource(R.string.open_chat_in_browser_hint),
            modifier = Modifier.clickable {
                browserSheetHeightPx?.let { openTwitchChatInBrowser(context, channelLogin, it()) }
            },
        )
    }
    AnimatedVisibility(
        visible = browserRows.experimentalLogin,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        ExperimentalLoginChatRow(onClick = onExperimentalLogin)
    }
}

/** Reloads the emotes and chatter labels, then closes the sheet; a second tap while it runs does nothing. */
@Composable
private fun RefreshEmotesRow(scope: CoroutineScope, refresh: () -> List<Job>, onRefreshed: suspend () -> Unit) {
    var refreshing by remember { mutableStateOf(false) }
    ChatSettingsRow(
        label = stringResource(R.string.refresh_emotes_and_labels),
        modifier = Modifier.clickable {
            if (!refreshing) {
                refreshing = true
                scope.launch {
                    try {
                        refresh().joinAll()
                    } finally {
                        if (isActive) onRefreshed()
                    }
                }
            }
        },
        trailing = {
            AnimatedVisibility(
                visible = refreshing,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        },
    )
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
