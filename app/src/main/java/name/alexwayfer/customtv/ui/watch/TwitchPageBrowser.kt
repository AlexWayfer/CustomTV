package name.alexwayfer.customtv.ui.watch

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.core.net.toUri
import name.alexwayfer.customtv.diagnostics.AppLog

/** The top corners of a browser opened as a sheet over the app. */
internal const val BROWSER_SHEET_CORNER_RADIUS_DP = 16

/**
 * Opens a page about the channel, such as a twitch.tv page or its MemeAlerts page, in a Custom Tab, signed in
 * with the browser's own session on that site. A
 * [sheetHeightPx] above zero opens it as a sheet of that height; zero opens it full screen.
 * [onVisibleChanged] hears the tab show and hide while the browser keeps a session with the app;
 * a browser without one reports nothing.
 */
internal fun openTwitchPageInBrowser(
    context: Context,
    url: String,
    sheetHeightPx: Int,
    onVisibleChanged: (Boolean) -> Unit = {},
) {
    val generation = ++pageBrowserGeneration
    val browserPackage = CustomTabsClient.getPackageName(context, null)
    if (browserPackage == null) {
        launchTwitchPage(context, url, sheetHeightPx, session = null, browserPackage = null)
        return
    }
    val appContext = context.applicationContext
    val connection = object : CustomTabsServiceConnection() {
        override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
            if (generation != pageBrowserGeneration) return
            client.warmup(0)
            launchTwitchPage(
                context = context,
                url = url,
                sheetHeightPx = sheetHeightPx,
                session = client.newSession(pageTabCallback(appContext, generation, onVisibleChanged)),
                browserPackage = browserPackage,
            )
        }

        override fun onServiceDisconnected(name: ComponentName) = Unit
    }
    unbindPageBrowser(appContext)
    pageBrowserConnection = connection
    if (!CustomTabsClient.bindCustomTabsService(appContext, browserPackage, connection)) {
        pageBrowserConnection = null
        AppLog.w(TAG, "page ${pagePath(url)} browser bind failed")
        launchTwitchPage(context, url, sheetHeightPx, session = null, browserPackage = browserPackage)
    }
}

private fun pageTabCallback(
    appContext: Context,
    generation: Int,
    onVisibleChanged: (Boolean) -> Unit,
): CustomTabsCallback = object : CustomTabsCallback() {
    override fun onNavigationEvent(navigationEvent: Int, extras: Bundle?) {
        if (navigationEvent == TAB_SHOWN) onVisibleChanged(true)
        if (navigationEvent == TAB_HIDDEN) onVisibleChanged(false)
        if (navigationEvent == TAB_HIDDEN && generation == pageBrowserGeneration) {
            unbindPageBrowser(appContext)
        }
    }
}

private fun launchTwitchPage(
    context: Context,
    url: String,
    sheetHeightPx: Int,
    session: CustomTabsSession?,
    browserPackage: String?,
) {
    val builder = if (session == null) CustomTabsIntent.Builder() else CustomTabsIntent.Builder(session)
    builder
        .setShowTitle(false)
        .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
        .setCloseButtonPosition(CustomTabsIntent.CLOSE_BUTTON_POSITION_END)
    if (sheetHeightPx > 0) {
        builder.setInitialActivityHeightPx(sheetHeightPx, CustomTabsIntent.ACTIVITY_HEIGHT_ADJUSTABLE)
        builder.setToolbarCornerRadiusDp(BROWSER_SHEET_CORNER_RADIUS_DP)
    }
    try {
        val tabs = builder.build()
        if (browserPackage != null) tabs.intent.setPackage(browserPackage)
        tabs.launchUrl(context, url.toUri())
        AppLog.i(TAG, "page ${pagePath(url)} opened in browser")
    } catch (error: ActivityNotFoundException) {
        AppLog.w(TAG, "page ${pagePath(url)} browser open failed ${error.javaClass.simpleName}")
    }
}

/** The page's path for the log: it names the page and the channel, without the query. */
private fun pagePath(url: String): String = url.toUri().path.orEmpty()

private fun unbindPageBrowser(appContext: Context) {
    val connection = pageBrowserConnection ?: return
    pageBrowserConnection = null
    try {
        appContext.unbindService(connection)
    } catch (_: IllegalArgumentException) {
        AppLog.w(TAG, "page browser already unbound")
    }
}

private const val TAG = "TwitchPageBrowser"
private var pageBrowserConnection: CustomTabsServiceConnection? = null
private var pageBrowserGeneration = 0
