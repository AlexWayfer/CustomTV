package name.alexwayfer.customtv.data

import android.app.Application

/** Watch streaks are a Premium feature; the free build starts nothing. */
internal object ExperimentalFeature {
    fun start(application: Application) = Unit
}
