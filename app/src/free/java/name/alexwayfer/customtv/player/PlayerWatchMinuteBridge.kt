package name.alexwayfer.customtv.player

import android.content.res.AssetManager
import android.os.Handler
import android.webkit.WebView

internal fun installWatchMinuteBridge(webView: WebView, handler: Handler, isCurrentLivePage: (String) -> Boolean) = Unit

@Suppress("FunctionOnlyReturningConstant")
internal fun watchMinuteScript(assets: AssetManager, name: String): String? = null
