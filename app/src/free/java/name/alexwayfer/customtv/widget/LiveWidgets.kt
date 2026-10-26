package name.alexwayfer.customtv.widget

import android.app.Application
import androidx.work.WorkManager

/** The home screen widget of the live channels is a Premium feature; the free build has none. */
internal object LiveWidgets {
    /** Cancels a periodic check a Premium install left scheduled, so it does not run without its worker. */
    fun start(application: Application) {
        WorkManager.getInstance(application).cancelUniqueWork(LIVE_WIDGET_WORK_NAME)
    }
}
