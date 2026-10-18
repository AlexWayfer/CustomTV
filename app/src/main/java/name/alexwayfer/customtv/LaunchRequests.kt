package name.alexwayfer.customtv

import android.content.Context
import android.content.Intent
import name.alexwayfer.customtv.data.STREAM_ALERTS_LOGIN
import name.alexwayfer.customtv.data.STREAM_START_CHANNEL
import name.alexwayfer.customtv.data.StreamAlertsLoginRequest
import name.alexwayfer.customtv.data.StreamStartRequest
import name.alexwayfer.customtv.data.WhisperOpenRequest
import name.alexwayfer.customtv.links.TWITCH_LINK_URL
import name.alexwayfer.customtv.links.TwitchLinkRequest
import name.alexwayfer.customtv.update.APP_UPDATE_OPEN
import name.alexwayfer.customtv.update.AppUpdateOpenRequest

/** Hands what a notification or a twitch.tv link asked for, when it opened the app, to the screens that act on it. */
internal fun offerLaunchRequests(context: Context, intent: Intent) {
    StreamStartRequest.offer(intent.getStringExtra(STREAM_START_CHANNEL))
    offerStreamAlertsLogin(intent)
    offerAppUpdateOpen(intent)
    WhisperOpenRequest.offer(context, intent)
    offerTwitchLink(intent)
}

/** The extra is dropped once read, so recreating the activity does not open the link again. */
private fun offerTwitchLink(intent: Intent) {
    TwitchLinkRequest.offer(intent.getStringExtra(TWITCH_LINK_URL))
    intent.removeExtra(TWITCH_LINK_URL)
}

/** The extra is dropped once read, so recreating the activity does not start the login again. */
private fun offerStreamAlertsLogin(intent: Intent) {
    StreamAlertsLoginRequest.offer(intent.getBooleanExtra(STREAM_ALERTS_LOGIN, false))
    intent.removeExtra(STREAM_ALERTS_LOGIN)
}

/** The extra is dropped once read, so recreating the activity does not open the settings again. */
private fun offerAppUpdateOpen(intent: Intent) {
    AppUpdateOpenRequest.offer(intent.getBooleanExtra(APP_UPDATE_OPEN, false))
    intent.removeExtra(APP_UPDATE_OPEN)
}
