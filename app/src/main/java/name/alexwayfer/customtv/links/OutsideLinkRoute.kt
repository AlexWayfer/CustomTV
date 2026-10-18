package name.alexwayfer.customtv.links

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.browser.customtabs.CustomTabsClient
import androidx.core.net.toUri
import name.alexwayfer.customtv.diagnostics.AppLog

/** Where a link the user follows out of a screen goes. */
internal enum class OutsideLinkRoute {
    /** A twitch.tv page the app shows: it opens here, without a trip through Android. */
    App,

    /** Android hands the link to this app, which does not show that page: the browser opens it. */
    Browser,

    /** Wherever Android sends it, such as the browser or the Twitch app. */
    System,
}

/**
 * A link goes to the app only when Android would hand it to the app anyway, that is, once the user let the app open
 * twitch.tv links; otherwise it goes where it always went.
 */
internal fun outsideLinkRoute(
    handlerPackage: String?,
    ownPackage: String,
    target: TwitchLinkTarget?,
): OutsideLinkRoute =
    when {
        handlerPackage != ownPackage -> OutsideLinkRoute.System
        target != null -> OutsideLinkRoute.App
        else -> OutsideLinkRoute.Browser
    }

/** The package of the app that opens [url] by default; Android's chooser when none is set. */
internal fun linkHandlerPackage(packageManager: PackageManager, url: String): String? {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
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

/**
 * Opens [url] wherever Android sends it, except this app: a twitch.tv link the app holds goes to the browser. Returns
 * whether something opened it.
 */
internal fun openOutsideApp(context: Context, url: String): Boolean {
    if (linkHandlerPackage(context.packageManager, url) == context.packageName) return openInBrowser(context, url)
    return startLink(context, Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/**
 * Opens [url] in the default browser, never in this app, so a twitch.tv link the app holds does not come back to it.
 * Without a default browser Android asks which app opens it, this one left out. Returns whether something opened it.
 */
internal fun openInBrowser(context: Context, url: String): Boolean {
    val view = Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val browser = CustomTabsClient.getPackageName(context, null)
    val intent = if (browser != null) {
        view.setPackage(browser)
    } else {
        Intent.createChooser(view, null)
            .putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, arrayOf(ComponentName(context, TwitchLinkActivity::class.java)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return startLink(context, intent)
}

private fun startLink(context: Context, intent: Intent): Boolean =
    try {
        context.startActivity(intent)
        true
    } catch (error: ActivityNotFoundException) {
        AppLog.w(TWITCH_LINKS_TAG, "link open failed ${error.javaClass.simpleName}")
        false
    }

internal const val TWITCH_LINKS_TAG = "TwitchLinks"
