package name.alexwayfer.customtv.links

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import name.alexwayfer.customtv.MainActivity
import name.alexwayfer.customtv.diagnostics.AppLog

/**
 * Takes the twitch.tv links Android hands to the app. A page the app shows goes to [MainActivity], which comes to the
 * front and drops whatever was left above it, such as a Custom Tab, instead of starting a second copy of the app. Any
 * other page opens in the browser, so the link never dead-ends or comes back here.
 */
class TwitchLinkActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent?.dataString
        when {
            url == null -> AppLog.w(TWITCH_LINKS_TAG, "link without an address")
            twitchLinkTarget(url) != null -> {
                AppLog.i(TWITCH_LINKS_TAG, "link opens in the app")
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .putExtra(TWITCH_LINK_URL, url)
                        .addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP,
                        ),
                )
            }
            else -> {
                AppLog.i(TWITCH_LINKS_TAG, "link goes to the browser")
                openInBrowser(this, url)
            }
        }
        finish()
    }
}
