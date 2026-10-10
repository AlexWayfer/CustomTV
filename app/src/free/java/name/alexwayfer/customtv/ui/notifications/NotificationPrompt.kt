package name.alexwayfer.customtv.ui.notifications

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.NotificationPromptStore
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.theme.TwitchText

/** Offers update and crash notifications on Home while they are off, until Not now or a refusal answers it; turning them on later clears that answer. */
@Composable
internal fun HomeNotificationPrompt() {
    val context = LocalContext.current
    val store = remember(context) { NotificationPromptStore(context) }
    val dismissed by store.dismissed.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val enabler = rememberNotificationEnabler(firstAsk = true) { scope.launch { store.markDismissed() } }
    NotificationPromptVisibility(homeNotificationPromptVisible(enabler.enabled, dismissed)) {
        NotificationPromptCard(
            onEnable = enabler.enable,
            modifier = Modifier.padding(top = 16.dp),
            onDismiss = { scope.launch { store.markDismissed() } },
        )
    }
}

/** Keeps the offer in Settings for as long as notifications are off, answered on Home or not. */
@Composable
internal fun SettingsNotificationPrompt() {
    val enabler = rememberNotificationEnabler(firstAsk = false)
    NotificationPromptVisibility(!enabler.enabled) {
        NotificationPromptCard(
            onEnable = enabler.enable,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        )
    }
}

@Composable
private fun NotificationPromptVisibility(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        content()
    }
}

/** Home: the text with both actions centered below it. Settings, with [onDismiss] null: Enable at the end of the text. */
@Composable
private fun NotificationPromptCard(
    onEnable: () -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = if (onDismiss == null) 12.dp else 0.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = Icons.Outlined.Notifications, contentDescription = null)
            Text(
                text = stringResource(R.string.notification_prompt),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp, top = 8.dp, bottom = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (onDismiss == null) EnableButton(onEnable)
        }
        if (onDismiss != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = TwitchText),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    Text(stringResource(R.string.notification_prompt_dismiss))
                }
                EnableButton(onEnable)
            }
        }
    }
}

@Composable
private fun EnableButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(32.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = TwitchPurple,
            contentColor = Color.White,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        Text(stringResource(R.string.notification_prompt_enable))
    }
}

private class NotificationEnabler(val enabled: Boolean, val enable: () -> Unit)

/** Whether notifications are on, read again on every return to the app, and the action that turns them on. */
@Composable
private fun rememberNotificationEnabler(firstAsk: Boolean, onRefused: () -> Unit = {}): NotificationEnabler {
    val context = LocalContext.current
    val activity = LocalActivity.current
    var enabled by remember(context) { mutableStateOf(notificationsEnabled(context)) }
    val store = remember(context) { NotificationPromptStore(context) }
    LaunchedEffect(store, enabled) {
        if (notificationPromptAnswerResets(enabled, store.dismissed.first())) store.clearDismissed()
    }
    LifecycleResumeEffect(context) {
        enabled = notificationsEnabled(context)
        onPauseOrDispose { }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        enabled = notificationsEnabled(context)
        if (granted) return@rememberLauncherForActivityResult
        val refusal = notificationPermissionRefusal(
            firstAsk = firstAsk,
            showRationale = Build.VERSION.SDK_INT >= 33 && activity != null &&
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS),
        )
        when (refusal) {
            NotificationPermissionRefusal.Refused -> onRefused()
            NotificationPermissionRefusal.Blocked -> openNotificationSettings(context)
        }
    }
    return NotificationEnabler(enabled) {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            notificationPromptAction(
                permissionNeeded = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED,
                firstAsk = firstAsk,
                showRationale = Build.VERSION.SDK_INT >= 33 && activity != null &&
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ),
            ) == NotificationPromptAction.RequestPermission
        ) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openNotificationSettings(context)
        }
    }
}

private fun notificationsEnabled(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

private fun openNotificationSettings(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
        )
    } catch (_: ActivityNotFoundException) {
        return
    }
}
