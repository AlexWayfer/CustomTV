package name.alexwayfer.customtv.ui.watch

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

internal fun twitchChatUrl(channelLogin: String): String =
    "https://www.twitch.tv/popout/$channelLogin/chat?popout="

internal fun chatBrowserSheetHeightPx(containerHeightPx: Int, playerBottomPx: Int): Int =
    (containerHeightPx - playerBottomPx).coerceAtLeast(0)

internal fun openTwitchChatInBrowser(
    context: Context,
    channelLogin: String,
    sheetHeightPx: Int,
) = openTwitchPageInBrowser(context, twitchChatUrl(channelLogin), sheetHeightPx)

/**
 * Opens a twitch.tv page in a Custom Tab, signed in with the browser's own Twitch session. A
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
    val generation = ++chatBrowserGeneration
    val browserPackage = CustomTabsClient.getPackageName(context, null)
    if (browserPackage == null) {
        launchTwitchPage(context, url, sheetHeightPx, session = null, browserPackage = null)
        return
    }
    val appContext = context.applicationContext
    val connection = object : CustomTabsServiceConnection() {
        override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
            if (generation != chatBrowserGeneration) return
            client.warmup(0)
            launchTwitchPage(
                context = context,
                url = url,
                sheetHeightPx = sheetHeightPx,
                session = client.newSession(chatTabCallback(appContext, generation, onVisibleChanged)),
                browserPackage = browserPackage,
            )
        }

        override fun onServiceDisconnected(name: ComponentName) = Unit
    }
    unbindChatBrowser(appContext)
    chatBrowserConnection = connection
    if (!CustomTabsClient.bindCustomTabsService(appContext, browserPackage, connection)) {
        chatBrowserConnection = null
        AppLog.w(TAG, "page browser bind failed")
        launchTwitchPage(context, url, sheetHeightPx, session = null, browserPackage = browserPackage)
    }
}

private fun chatTabCallback(
    appContext: Context,
    generation: Int,
    onVisibleChanged: (Boolean) -> Unit,
): CustomTabsCallback = object : CustomTabsCallback() {
    override fun onNavigationEvent(navigationEvent: Int, extras: Bundle?) {
        if (navigationEvent == TAB_SHOWN) onVisibleChanged(true)
        if (navigationEvent == TAB_HIDDEN) onVisibleChanged(false)
        if (navigationEvent == TAB_HIDDEN && generation == chatBrowserGeneration) {
            unbindChatBrowser(appContext)
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
        .setShowTitle(false)
        .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
        .setCloseButtonPosition(CustomTabsIntent.CLOSE_BUTTON_POSITION_END)
    if (sheetHeightPx > 0) {
        builder.setInitialActivityHeightPx(sheetHeightPx, CustomTabsIntent.ACTIVITY_HEIGHT_ADJUSTABLE)
        builder.setToolbarCornerRadiusDp(16)
    }
    try {
        val tabs = builder.build()
        if (browserPackage != null) tabs.intent.setPackage(browserPackage)
        tabs.launchUrl(context, url.toUri())
        AppLog.i(TAG, "page opened in browser")
    } catch (error: Exception) {
        AppLog.w(TAG, "page browser open failed ${error.javaClass.simpleName}")
    }
}

private fun unbindChatBrowser(appContext: Context) {
    val connection = chatBrowserConnection ?: return
    chatBrowserConnection = null
    try {
        appContext.unbindService(connection)
    } catch (_: IllegalArgumentException) {
        AppLog.w(TAG, "page browser already unbound")
    }
}

private const val TAG = "TwitchChatBrowser"
private var chatBrowserConnection: CustomTabsServiceConnection? = null
private var chatBrowserGeneration = 0
