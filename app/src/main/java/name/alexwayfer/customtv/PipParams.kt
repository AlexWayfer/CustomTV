package name.alexwayfer.customtv

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.util.Rational
import android.view.View

internal fun Activity.hasPipFeature(): Boolean {
    return packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
}

/** The 16:9 window; on Android 12 and later it opens on its own when [autoEnter] and grows from the screen. */
internal fun Activity.pipParams(autoEnter: Boolean): PictureInPictureParams {
    val builder = PictureInPictureParams.Builder()
        .setAspectRatio(Rational(16, 9))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        builder.setAutoEnterEnabled(autoEnter)
        builder.setSeamlessResizeEnabled(true)
        sourceRectHint()?.let { builder.setSourceRectHint(it) }
    }
    return builder.build()
}

internal fun Activity.applyPipParams(autoEnter: Boolean) {
    if (!hasPipFeature()) return
    try {
        setPictureInPictureParams(pipParams(autoEnter))
    } catch (_: IllegalStateException) {
        // The system has already dropped the activity, such as when its task is removed during destroy.
    }
}

private fun Activity.sourceRectHint(): Rect? {
    val content = findViewById<View>(android.R.id.content) ?: return null
    return Rect().takeIf { content.getGlobalVisibleRect(it) }
}
