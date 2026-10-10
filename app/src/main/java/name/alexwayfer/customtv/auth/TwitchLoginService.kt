package name.alexwayfer.customtv.auth

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.appNotificationBuilder
import name.alexwayfer.customtv.diagnostics.AppLog

internal class TwitchLoginService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val type = if (android.os.Build.VERSION.SDK_INT >= 29) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, waitingNotification(), type)
        return START_NOT_STICKY
    }

    private fun waitingNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.twitch_login_channel),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
        return appNotificationBuilder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(getString(R.string.twitch_device_waiting))
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "twitch_login"
        private const val NOTIFICATION_ID = 7101

        fun stayAwake(context: Context, awake: Boolean) {
            val intent = Intent(context, TwitchLoginService::class.java)
            if (!awake) {
                context.stopService(intent)
                return
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (error: Exception) {
                AppLog.w(TAG, "login stay awake failed ${error.javaClass.simpleName}")
            }
        }

        private const val TAG = "TwitchAuth"
    }
}
