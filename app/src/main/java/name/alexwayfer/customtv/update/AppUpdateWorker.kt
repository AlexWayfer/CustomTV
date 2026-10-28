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

private const val APP_UPDATE_WORK_NAME = "app-update"

/** The periodic check for a new release; the check itself posts the notification. */
internal class AppUpdateWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        AppUpdates.checkInBackground()
        return Result.success()
    }
}

/** Each edition checks at its own [APP_UPDATE_CHECK_INTERVAL]: Premium's check also tells whether its access holds. */
internal fun scheduleAppUpdateChecks(context: Context) {
    val request = PeriodicWorkRequestBuilder<AppUpdateWorker>(
        APP_UPDATE_CHECK_INTERVAL.inWholeMinutes,
        TimeUnit.MINUTES,
    )
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        APP_UPDATE_WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        request,
    )
}
