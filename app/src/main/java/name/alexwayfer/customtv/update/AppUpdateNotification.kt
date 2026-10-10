package name.alexwayfer.customtv.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import name.alexwayfer.customtv.MainActivity
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.appNotificationBuilder

private const val CHANNEL_ID = "app_updates"
private const val APP_UPDATE_NOTIFICATION_ID = 7108

/** One notice for the newest release; a later check replaces it, and an installed release takes it away. */
internal fun showAppUpdateNotification(context: Context, version: String) {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    if (manager.getNotificationChannel(CHANNEL_ID) == null) {
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.app_update_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }
    val open = PendingIntent.getActivity(
        context,
        APP_UPDATE_NOTIFICATION_ID,
        Intent(context, MainActivity::class.java)
            .putExtra(APP_UPDATE_OPEN, true)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    manager.notify(
        APP_UPDATE_NOTIFICATION_ID,
        appNotificationBuilder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.app_update_available, version))
            .setContentText(context.getString(R.string.app_update_notification_text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build(),
    )
}

internal fun cancelAppUpdateNotification(context: Context) {
    context.getSystemService(NotificationManager::class.java)?.cancel(APP_UPDATE_NOTIFICATION_ID)
}
