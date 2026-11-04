package name.alexwayfer.customtv

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.util.Rational

internal fun Activity.hasPipFeature(): Boolean {
    return packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
}

/** The 16:9 window; on Android 12 and later it opens on its own when [autoEnter] and grows from the screen. */
internal fun MainActivity.pipParams(autoEnter: Boolean): PictureInPictureParams {
    val builder = PictureInPictureParams.Builder()
        .setAspectRatio(Rational(PIP_VIDEO_RATIO_WIDTH, PIP_VIDEO_RATIO_HEIGHT))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        builder.setAutoEnterEnabled(autoEnter)
        builder.setSeamlessResizeEnabled(true)
        // An empty rect also clears a previous player's hint in Android's merged PiP parameters.
        val bounds = pipVideoSources.bounds
        builder.setSourceRectHint(bounds?.let { Rect(it.left, it.top, it.right, it.bottom) } ?: Rect())
    }
    return builder.build()
}

internal fun MainActivity.applyPipParams(autoEnter: Boolean) {
    if (!hasPipFeature()) return
    try {
        setPictureInPictureParams(pipParams(autoEnter))
    } catch (_: IllegalStateException) {
        // The system has already dropped the activity, such as when its task is removed during destroy.
    }
}
