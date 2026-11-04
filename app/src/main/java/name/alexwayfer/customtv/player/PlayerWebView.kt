package name.alexwayfer.customtv.player

import android.content.Context
import android.webkit.WebView

internal class PlayerWebView(context: Context) : WebView(context) {
    init {
        // WebView vibrates on a long press even though the player has no action for it.
        isHapticFeedbackEnabled = false
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(VISIBLE)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun getWindowVisibility(): Int = VISIBLE
}
