package name.alexwayfer.customtv.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.BuildConfig
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.asString
import name.alexwayfer.customtv.ui.rememberHeldWhileLoading
import name.alexwayfer.customtv.ui.theme.TwitchBg
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary
import name.alexwayfer.customtv.update.AppRelease
import name.alexwayfer.customtv.update.AppUpdateDownload
import name.alexwayfer.customtv.update.AppUpdateStatus
import name.alexwayfer.customtv.update.appNoUpdateText

/** The public topic of the free releases: the free build's group, and Premium's way back to free. */
internal const val FREE_RELEASES_TELEGRAM_URL = "https://t.me/customtv_app/3"

/**
 * This build's version, the last check for a newer one with its download and install, Check updates, and the
 * edition's [links] on the line below.
 */
@Composable
internal fun AppUpdateSection(
    status: AppUpdateStatus,
    download: AppUpdateDownload,
    onCheck: () -> Unit,
    onDownload: (AppRelease) -> Unit,
    links: @Composable () -> Unit,
) {
    // A quick check keeps its progress for the minimum, so the status and the button do not blink.
    val shown = rememberHeldWhileLoading(status, loading = status is AppUpdateStatus.Checking)
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        GroupStatus(stringResource(R.string.app_update_current_version, BuildConfig.VERSION_NAME))
        AppUpdateStatusLine(shown, download, onDownload)
        SmallButton(
            text = stringResource(R.string.app_update_check),
            onClick = onCheck,
            modifier = Modifier.padding(top = 8.dp),
            enabled = shown !is AppUpdateStatus.Checking,
        )
        // The links to the releases sit on their own line under the check.
        FlowRow(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            links()
        }
    }
}

/** The last check for a new version: its progress, why it failed, or the update it found. */
@Composable
private fun AppUpdateStatusLine(
    shown: AppUpdateStatus,
    download: AppUpdateDownload,
    onDownload: (AppRelease) -> Unit,
) {
    // The check fades into its result, and back into progress on the next check.
    AnimatedContent(
        targetState = shown,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        contentKey = { it.javaClass },
        label = "appUpdateStatus",
    ) { status ->
        when (status) {
            AppUpdateStatus.Checking -> ProgressStatus(
                text = stringResource(R.string.app_update_checking),
                modifier = Modifier.padding(top = 4.dp),
            )
            is AppUpdateStatus.Failed -> GroupStatus(status.message.asString())
            is AppUpdateStatus.Read -> {
                val update = status.update
                if (update == null) {
                    GroupStatus(appNoUpdateText(status.latest).asString())
                } else {
                    AppUpdateBlock(update, download, onDownload = { onDownload(update) })
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
