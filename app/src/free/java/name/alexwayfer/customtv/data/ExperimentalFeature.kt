package name.alexwayfer.customtv.data

import android.app.Application

/** Watch streaks are a Premium feature; the free build starts nothing. */
internal object ExperimentalFeature {
    @Suppress("unused")
    fun start(application: Application) = Unit
}
