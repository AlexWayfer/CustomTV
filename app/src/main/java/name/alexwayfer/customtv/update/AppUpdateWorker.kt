package name.alexwayfer.customtv.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import name.alexwayfer.customtv.telegram.TelegramSession

private const val APP_UPDATE_WORK_NAME = "app-update"

/** The daily check for a new release; the check itself posts the notification. */
internal class AppUpdateWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        TelegramSession.checkInBackground()
        return Result.success()
    }
}

internal fun scheduleAppUpdateChecks(context: Context) {
    val request = PeriodicWorkRequestBuilder<AppUpdateWorker>(1, TimeUnit.DAYS)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        APP_UPDATE_WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        request,
    )
}
