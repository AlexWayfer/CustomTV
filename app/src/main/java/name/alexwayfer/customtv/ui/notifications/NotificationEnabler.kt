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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.flow.first
import name.alexwayfer.customtv.data.NotificationPromptStore

internal class NotificationEnabler(val enabled: Boolean, val enable: () -> Unit)

/** Whether notifications are on, read again on every return to the app, and the action that turns them on. */
@Composable
internal fun rememberNotificationEnabler(firstAsk: Boolean, onRefused: () -> Unit = {}): NotificationEnabler {
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
            showRationale = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && activity != null &&
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS),
        )
        when (refusal) {
            NotificationPermissionRefusal.Refused -> onRefused()
            NotificationPermissionRefusal.Blocked -> openNotificationSettings(context)
        }
    }
    return NotificationEnabler(enabled) {
        val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPromptAction(
                permissionNeeded = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED,
                firstAsk = firstAsk,
                showRationale = activity != null &&
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ),
            )
        } else {
            null
        }
        if (action == NotificationPromptAction.RequestPermission) {
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
