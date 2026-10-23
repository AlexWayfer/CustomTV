package name.alexwayfer.customtv.data

import android.content.Context

/** Stream alerts are a Premium feature: the free build sends none to open. */
internal object StreamAlertsSeen {
    fun opened(context: Context, login: String?) = Unit
}
