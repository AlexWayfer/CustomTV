package name.alexwayfer.customtv.ui.account

import android.content.Context
import name.alexwayfer.customtv.ui.watch.openTwitchPageInBrowser
import kotlin.math.roundToInt

internal fun twitchAboutUrl(channelLogin: String): String =
    "https://www.twitch.tv/${channelLogin.trim().removePrefix("#").lowercase()}/about"

internal fun aboutBrowserSheetHeightPx(containerHeightPx: Int): Int =
    (containerHeightPx * ABOUT_BROWSER_HEIGHT_FRACTION).roundToInt()

internal fun openTwitchAboutInBrowser(
    context: Context,
    channelLogin: String,
    sheetHeightPx: Int,
) = openTwitchPageInBrowser(context, twitchAboutUrl(channelLogin), sheetHeightPx)

private const val ABOUT_BROWSER_HEIGHT_FRACTION = 0.85f
