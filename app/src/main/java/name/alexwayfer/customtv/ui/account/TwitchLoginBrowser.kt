package name.alexwayfer.customtv.ui.account

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Bundle
import android.os.SystemClock
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import name.alexwayfer.customtv.MainActivity
import name.alexwayfer.customtv.auth.TWITCH_APP_PACKAGE
import name.alexwayfer.customtv.auth.TWITCH_AUTH_TAG
import name.alexwayfer.customtv.auth.loginSheetHeightPx
import name.alexwayfer.customtv.auth.twitchLinkSettingsAction
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.ui.watch.BROWSER_SHEET_CORNER_RADIUS_DP
import kotlin.time.Duration.Companion.seconds

internal fun twitchLinkHandlingAllowed(context: Context): Boolean? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val manager = context.getSystemService(android.content.pm.verify.domain.DomainVerificationManager::class.java)
        ?: return null
    return try {
        manager.getDomainVerificationUserState(TWITCH_APP_PACKAGE)?.isLinkHandlingAllowed
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
}

internal fun loginLinkHandlerPackage(packageManager: PackageManager): String? {
    val intent = Intent(Intent.ACTION_VIEW, "https://www.twitch.tv/activate".toUri())
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.resolveActivity(
            intent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }
    return info?.activityInfo?.packageName
}

internal fun openTwitchLinkSettings(context: Context) {
    val intent = Intent(twitchLinkSettingsAction(Build.VERSION.SDK_INT))
        .setData("package:$TWITCH_APP_PACKAGE".toUri())
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        AppLog.w(TWITCH_AUTH_TAG, "twitch link settings unavailable")
    }
}

/** Opens the login page; [onLaunched] hears whether it opened, once the browser was asked to show it. */
internal fun openTwitchLoginPage(
    context: Context,
    url: String,
    screenHeightPx: Int,
    onHidden: () -> Unit,
    onLaunched: (opened: Boolean) -> Unit = {},
) {
    val generation = ++loginBrowserGeneration
    stopWatchingAppResume()
    val browser = CustomTabsClient.getPackageName(context, null)
    if (browser == null) {
        onLaunched(launchTwitchLoginPage(context, url, screenHeightPx, session = null, browserPackage = null))
        return
    }
    val appContext = context.applicationContext
    val launch = LoginBrowserLaunch(generation)
    val connection = object : CustomTabsServiceConnection() {
        override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
            if (!launch.claim(loginBrowserGeneration)) return
            client.warmup(0)
            val opened = launchTwitchLoginPage(
                context,
                url,
                screenHeightPx,
                client.newSession(loginPageCallback(context, onHidden)),
                browser,
            )
            onLaunched(opened)
        }

        override fun onServiceDisconnected(name: ComponentName) = Unit
    }
    unbindLoginBrowser(appContext)
    loginBrowserConnection = connection
    val bound = CustomTabsClient.bindCustomTabsService(appContext, browser, connection)
    if (!bound) {
        loginBrowserConnection = null
        AppLog.w(TWITCH_AUTH_TAG, "login browser bind failed")
        onLaunched(launchTwitchLoginPage(context, url, screenHeightPx, session = null, browserPackage = browser))
        return
    }
    // A browser whose Custom Tabs service crashed may accept the binding and never connect; open a plain tab then.
    Handler(Looper.getMainLooper()).postDelayed(
        {
            if (launch.claim(loginBrowserGeneration)) {
                AppLog.w(TWITCH_AUTH_TAG, "login browser did not connect")
                unbindLoginBrowser(appContext)
                val opened = launchTwitchLoginPage(
                    context,
                    url,
                    screenHeightPx,
                    session = null,
                    browserPackage = browser,
                )
                onLaunched(opened)
            }
        },
        LOGIN_BROWSER_CONNECT_TIMEOUT.inWholeMilliseconds,
    )
}

/** The first of the service connection and its timeout opens the login page, and only for the current attempt. */
internal class LoginBrowserLaunch(private val generation: Int) {
    private var launched = false

    fun claim(currentGeneration: Int): Boolean {
        if (launched || generation != currentGeneration) return false
        launched = true
        return true
    }
}

private fun loginPageCallback(context: Context, onHidden: () -> Unit): CustomTabsCallback {
    val visibility = LoginPageVisibility()
    val lifecycle = (context as? LifecycleOwner)?.lifecycle
    if (lifecycle != null) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && visibility.appResumed()) onHidden()
        }
        lifecycle.addObserver(observer)
        loginAppResumeWatch = lifecycle to observer
    }
    return object : CustomTabsCallback() {
        override fun onNavigationEvent(navigationEvent: Int, extras: Bundle?) {
            val now = SystemClock.elapsedRealtime()
            when (navigationEvent) {
                TAB_SHOWN -> visibility.shown(now)
                NAVIGATION_FINISHED -> visibility.loaded()
                TAB_HIDDEN -> {
                    val appInFront = lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) ?: true
                    if (visibility.hidden(now, appInFront)) onHidden()
                }
                NAVIGATION_STARTED, NAVIGATION_FAILED, NAVIGATION_ABORTED -> Unit
            }
        }
    }
}

internal fun releaseLoginBrowser(context: Context) {
    loginBrowserGeneration += 1
    stopWatchingAppResume()
    unbindLoginBrowser(context.applicationContext)
}

private fun stopWatchingAppResume() {
    val (lifecycle, observer) = loginAppResumeWatch ?: return
    loginAppResumeWatch = null
    lifecycle.removeObserver(observer)
}

/** Returns whether the page opened. */
private fun launchTwitchLoginPage(
    context: Context,
    url: String,
    screenHeightPx: Int,
    session: CustomTabsSession?,
    browserPackage: String?,
): Boolean {
    val builder = if (session == null) CustomTabsIntent.Builder() else CustomTabsIntent.Builder(session)
    builder
        .setShowTitle(false)
        .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
        .setCloseButtonPosition(CustomTabsIntent.CLOSE_BUTTON_POSITION_END)
    val sheetHeight = loginSheetHeightPx(screenHeightPx)
    if (session != null && sheetHeight > 0) {
        builder.setInitialActivityHeightPx(sheetHeight, CustomTabsIntent.ACTIVITY_HEIGHT_ADJUSTABLE)
        builder.setToolbarCornerRadiusDp(BROWSER_SHEET_CORNER_RADIUS_DP)
    }
    val tabs = builder.build()
    if (browserPackage != null) tabs.intent.setPackage(browserPackage)
    return try {
        tabs.launchUrl(context, url.toUri())
        AppLog.i(TWITCH_AUTH_TAG, if (session == null) "login page opened" else "login sheet opened")
        true
    } catch (error: ActivityNotFoundException) {
        AppLog.w(TWITCH_AUTH_TAG, "login page open failed ${error.javaClass.simpleName}")
        false
    }
}

private fun unbindLoginBrowser(appContext: Context) {
    val connection = loginBrowserConnection ?: return
    loginBrowserConnection = null
    try {
        appContext.unbindService(connection)
    } catch (_: IllegalArgumentException) {
        AppLog.w(TWITCH_AUTH_TAG, "login browser already unbound")
    }
}

private var loginBrowserConnection: CustomTabsServiceConnection? = null
private var loginAppResumeWatch: Pair<Lifecycle, LifecycleEventObserver>? = null
private var loginBrowserGeneration = 0

internal fun closeTwitchLoginPage(context: Context) {
    val intent = Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
    AppLog.i(TWITCH_AUTH_TAG, "login page close requested")
}

private val LOGIN_BROWSER_CONNECT_TIMEOUT = 3.seconds
