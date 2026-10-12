package name.alexwayfer.customtv.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import name.alexwayfer.customtv.BuildConfig
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.account.LogOutConfirmDialog
import name.alexwayfer.customtv.telegram.TelegramGroupState
import name.alexwayfer.customtv.telegram.TelegramSession
import name.alexwayfer.customtv.telegram.telegramEditionGroupLink
import name.alexwayfer.customtv.telegram.telegramNoUpdateText
import name.alexwayfer.customtv.ui.asString
import name.alexwayfer.customtv.ui.rememberHeldWhileLoading
import name.alexwayfer.customtv.ui.theme.TwitchBg
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary
import name.alexwayfer.customtv.update.AppUpdateDownload

@Composable
internal fun TelegramSettings() {
    SettingsSection(stringResource(R.string.settings_section_updates))
    val ui by TelegramSession.ui.collectAsStateWithLifecycle()
    AnimatedVisibility(
        visible = telegramUpdatesHintVisible(ui.step),
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        TelegramHint(stringResource(R.string.telegram_updates))
    }
    if (BuildConfig.TELEGRAM_API_HASH.isBlank()) {
        TelegramHint(stringResource(R.string.telegram_missing_api))
        return
    }
    var confirmLogOut by remember { mutableStateOf(false) }
    TelegramLogin { ui ->
        ui.accountLabel?.let { label ->
            val loggedIn = stringResource(R.string.telegram_account, label)
            val nameStart = loggedIn.indexOf(label)
            Text(
                text = buildAnnotatedString {
                    append(loggedIn)
                    if (nameStart >= 0) {
                        addStyle(
                            SpanStyle(fontWeight = FontWeight.Bold),
                            nameStart,
                            nameStart + label.length,
                        )
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        // Without a background, so the account action does not draw the eye from the updates below.
        TextButton(
            onClick = { confirmLogOut = true },
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            Text(stringResource(R.string.log_out))
        }
        TelegramGroup(if (BuildConfig.PREMIUM) ui.premiumGroup else ui.openGroup, ui.download)
    }
    if (confirmLogOut) {
        LogOutConfirmDialog(
            onConfirm = {
                confirmLogOut = false
                TelegramSession.logOut()
            },
            onDismiss = { confirmLogOut = false },
        )
    }
}

@Composable
private fun TelegramGroup(state: TelegramGroupState, download: AppUpdateDownload) {
    // A quick check keeps its progress for the minimum, so the status and the button do not blink.
    val shown = rememberHeldWhileLoading(state, loading = state is TelegramGroupState.Checking)
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        val currentVersion = if (BuildConfig.PREMIUM) {
            R.string.telegram_current_version_premium
        } else {
            R.string.telegram_current_version_free
        }
        GroupStatus(stringResource(currentVersion, BuildConfig.VERSION_NAME))
        TelegramUpdateStatus(shown, download)
        val context = LocalContext.current
        val groupLink = telegramEditionGroupLink(
            premium = BuildConfig.PREMIUM,
            openChatId = BuildConfig.TELEGRAM_OPEN_CHAT_ID,
            openTopicId = BuildConfig.TELEGRAM_OPEN_TOPIC_ID,
            premiumChatId = BuildConfig.TELEGRAM_PREMIUM_CHAT_ID,
            premiumTopicId = BuildConfig.TELEGRAM_PREMIUM_TOPIC_ID,
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SmallButton(
                text = stringResource(R.string.telegram_check_updates),
                onClick = TelegramSession.updates::checkAgain,
                enabled = shown !is TelegramGroupState.Checking,
            )
            if (groupLink != null) {
                SmallButton(
                    text = stringResource(R.string.telegram_open_group),
                    onClick = { openLink(context, groupLink) },
                    colors = secondaryButtonColors(),
                )
            }
        }
    }
}

/** The last check for a new version: its progress, why it failed, or the update it found. */
@Composable
private fun TelegramUpdateStatus(shown: TelegramGroupState, download: AppUpdateDownload) {
    // The check fades into its result, and back into progress on the next check.
    AnimatedContent(
        targetState = shown,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        contentKey = { it.javaClass },
        label = "telegramGroupStatus",
    ) { status ->
        when (status) {
            TelegramGroupState.Checking -> ProgressStatus(
                text = stringResource(R.string.telegram_group_checking),
                modifier = Modifier.padding(top = 4.dp),
            )
            TelegramGroupState.Unavailable -> GroupStatus(stringResource(R.string.telegram_group_unavailable))
            TelegramGroupState.NotConfigured -> GroupStatus(stringResource(R.string.telegram_group_not_configured))
            TelegramGroupState.TopicNotConfigured ->
                GroupStatus(stringResource(R.string.telegram_topic_not_configured))
            is TelegramGroupState.Failed -> GroupStatus(status.message.asString())
            is TelegramGroupState.Open -> {
                val update = status.update
                if (update == null) {
                    GroupStatus(telegramNoUpdateText(status.latest).asString())
                } else {
                    AppUpdateBlock(update, download)
                }
            }
        }
    }
}

@Composable
internal fun secondaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = TwitchTextSecondary,
    contentColor = TwitchBg,
)

@Composable
internal fun SmallButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
internal fun GroupStatus(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 4.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

/** A status that lasts a while, such as a check or a log out: a spinner and one announcement. */
@Composable
internal fun ProgressStatus(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
internal fun TelegramHint(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}
