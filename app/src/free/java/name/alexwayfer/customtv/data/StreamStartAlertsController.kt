package name.alexwayfer.customtv.data

import android.app.Application
import androidx.work.WorkManager

/** Stream-start and stream-change notifications are a Premium feature; the free build sends none. */
internal object StreamStartAlertsController {
    /** Cancels a periodic check a Premium install left scheduled, so it does not run without its worker. */
    fun start(application: Application) {
        WorkManager.getInstance(application).cancelUniqueWork(STREAM_START_WORK_NAME)
    }

    @Suppress("unused")
    fun onSignedIn(signedIn: Boolean) = Unit

    @Suppress("unused")
    fun onFollowedLive(channels: List<FollowedChannel>, accessToken: String) = Unit

    @Suppress("unused")
    fun onBackgroundHold(hold: Boolean) = Unit
}
