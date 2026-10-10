package name.alexwayfer.customtv

import android.content.Context
import androidx.core.app.NotificationCompat

/** Starts every notification of the app, so a debug build marks each one next to the app name. */
internal fun appNotificationBuilder(context: Context, channelId: String): NotificationCompat.Builder =
    NotificationCompat.Builder(context, channelId).apply {
        if (BuildConfig.DEBUG) setSubText(context.getString(R.string.notification_debug_label))
    }
