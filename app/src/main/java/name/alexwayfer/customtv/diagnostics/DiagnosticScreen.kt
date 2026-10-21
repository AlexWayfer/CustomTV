package name.alexwayfer.customtv.diagnostics

import android.app.Activity
import android.app.Application
import android.content.res.Configuration
import android.os.Bundle
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

/** The app's window when it crashed: what a layout crash depends on. */
internal data class DiagnosticScreen(
    val landscape: Boolean,
    val widthDp: Int,
    val heightDp: Int,
    val pictureInPicture: Boolean,
)

/** One report line for [screen], or a dash when no activity was on screen, as after a crash in the background. */
internal fun diagnosticScreenLine(screen: DiagnosticScreen?): String =
    if (screen == null) {
        "Screen: -"
    } else {
        val orientation = if (screen.landscape) "landscape" else "portrait"
        val pip = if (screen.pictureInPicture) "yes" else "no"
        "Screen: $orientation ${screen.widthDp}x${screen.heightDp} dp, PiP: $pip"
    }

/** Remembers the activity started last, so a crash can tell what its window looked like. */
internal class DiagnosticScreenTracker : Application.ActivityLifecycleCallbacks {
    @Volatile
    private var activity: WeakReference<Activity>? = null

    fun current(): DiagnosticScreen? {
        val activity = activity?.get() ?: return null
        val density = activity.resources.displayMetrics.density
        val window = activity.window.decorView
        return DiagnosticScreen(
            landscape = activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE,
            widthDp = (window.width / density).roundToInt(),
            heightDp = (window.height / density).roundToInt(),
            pictureInPicture = activity.isInPictureInPictureMode,
        )
    }

    override fun onActivityStarted(activity: Activity) {
        this.activity = WeakReference(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (this.activity?.get() === activity) this.activity = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
