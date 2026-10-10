package name.alexwayfer.customtv.ui.account

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
import kotlin.math.roundToInt

internal fun twitchAboutUrl(channelLogin: String): String =
    "https://www.twitch.tv/${channelLogin.trim().removePrefix("#").lowercase()}/about"

internal fun aboutBrowserSheetHeightPx(containerHeightPx: Int): Int =
    (containerHeightPx * ABOUT_BROWSER_HEIGHT_FRACTION).roundToInt()

internal fun openTwitchAboutInBrowser(
    context: Context,
    channelLogin: String,
    sheetHeightPx: Int,
) {
    val generation = ++aboutBrowserGeneration
    val browserPackage = CustomTabsClient.getPackageName(context, null)
    if (browserPackage == null) {
        launchTwitchAbout(context, channelLogin, sheetHeightPx, session = null, browserPackage = null)
        return
    }
    val appContext = context.applicationContext
    val connection = object : CustomTabsServiceConnection() {
        override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
            if (generation != aboutBrowserGeneration) return
            client.warmup(0)
            launchTwitchAbout(
                context = context,
                channelLogin = channelLogin,
                sheetHeightPx = sheetHeightPx,
                session = client.newSession(aboutTabCallback(appContext, generation)),
                browserPackage = browserPackage,
            )
        }

        override fun onServiceDisconnected(name: ComponentName) = Unit
    }
    unbindAboutBrowser(appContext)
    aboutBrowserConnection = connection
    if (!CustomTabsClient.bindCustomTabsService(appContext, browserPackage, connection)) {
        aboutBrowserConnection = null
        AppLog.w(TAG, "about browser bind failed")
        launchTwitchAbout(context, channelLogin, sheetHeightPx, session = null, browserPackage = browserPackage)
    }
}

private fun aboutTabCallback(appContext: Context, generation: Int): CustomTabsCallback = object : CustomTabsCallback() {
    override fun onNavigationEvent(navigationEvent: Int, extras: Bundle?) {
        if (navigationEvent == TAB_HIDDEN && generation == aboutBrowserGeneration) {
            unbindAboutBrowser(appContext)
        }
    }
}

private fun launchTwitchAbout(
    context: Context,
    channelLogin: String,
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
        val tab = builder.build()
        if (browserPackage != null) tab.intent.setPackage(browserPackage)
        tab.launchUrl(context, twitchAboutUrl(channelLogin).toUri())
        AppLog.i(TAG, "about opened in browser")
    } catch (error: Exception) {
        AppLog.w(TAG, "about browser open failed ${error.javaClass.simpleName}")
    }
}

private fun unbindAboutBrowser(appContext: Context) {
    val connection = aboutBrowserConnection ?: return
    aboutBrowserConnection = null
    try {
        appContext.unbindService(connection)
    } catch (_: IllegalArgumentException) {
        AppLog.w(TAG, "about browser already unbound")
    }
}

private const val TAG = "TwitchAboutBrowser"
private const val ABOUT_BROWSER_HEIGHT_FRACTION = 0.85f
private var aboutBrowserConnection: CustomTabsServiceConnection? = null
private var aboutBrowserGeneration = 0
