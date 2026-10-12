package name.alexwayfer.customtv.ui.watch

import android.content.Context

internal fun twitchChatUrl(channelLogin: String): String =
    "https://www.twitch.tv/popout/$channelLogin/chat?popout="

internal fun chatBrowserSheetHeightPx(containerHeightPx: Int, playerBottomPx: Int): Int =
    (containerHeightPx - playerBottomPx).coerceAtLeast(0)

internal fun openTwitchChatInBrowser(
    context: Context,
    channelLogin: String,
    sheetHeightPx: Int,
) = openTwitchPageInBrowser(context, twitchChatUrl(channelLogin), sheetHeightPx)
