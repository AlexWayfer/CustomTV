package name.alexwayfer.customtv.auth

import android.os.Build

internal const val TWITCH_APP_PACKAGE = "tv.twitch.android.app"

private const val LOGIN_SHEET_HEIGHT_PERCENT = 85L

internal fun twitchAppHandlesLogin(handlerPackage: String?): Boolean {
    return handlerPackage == TWITCH_APP_PACKAGE
}

internal fun loginNeedsLinkWarning(linkHandlingAllowed: Boolean?, handlerPackage: String?): Boolean {
    return linkHandlingAllowed ?: twitchAppHandlesLogin(handlerPackage)
}

internal fun loginSheetHeightPx(screenHeightPx: Int): Int {
    if (screenHeightPx <= 0) return 0
    return (screenHeightPx * LOGIN_SHEET_HEIGHT_PERCENT / 100L).toInt()
}

internal fun twitchLinkSettingsAction(sdkInt: Int): String {
    return if (sdkInt >= Build.VERSION_CODES.S) {
        "android.settings.APP_OPEN_BY_DEFAULT_SETTINGS"
    } else {
        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    }
}
