package name.alexwayfer.customtv.player

import android.content.Context
import android.webkit.WebView

internal class PlayerWebView(context: Context) : WebView(context) {
    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(VISIBLE)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun getWindowVisibility(): Int = VISIBLE
}
