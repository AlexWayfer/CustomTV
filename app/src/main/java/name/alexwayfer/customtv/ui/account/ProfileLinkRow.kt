package name.alexwayfer.customtv.ui.account

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.auth.TwitchProfileLink
import name.alexwayfer.customtv.diagnostics.AppLog

@Composable
internal fun ProfileLink(link: TwitchProfileLink, onClick: () -> Unit) {
    val mark = profileLinkMark(link.name, link.url)
    Row(
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = painterResource(mark.drawable),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(link.title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
    }
}

private val ProfileLinkMark.drawable: Int
    get() = when (this) {
        ProfileLinkMark.Link -> R.drawable.ic_profile_link
        ProfileLinkMark.Twitter -> R.drawable.ic_profile_twitter
        ProfileLinkMark.GitHub -> R.drawable.ic_profile_github
        ProfileLinkMark.Discord -> R.drawable.ic_profile_discord
        ProfileLinkMark.Deezer -> R.drawable.ic_profile_deezer
        ProfileLinkMark.YouTube -> R.drawable.ic_profile_youtube
        ProfileLinkMark.Instagram -> R.drawable.ic_profile_instagram
        ProfileLinkMark.TikTok -> R.drawable.ic_profile_tiktok
        ProfileLinkMark.Facebook -> R.drawable.ic_profile_facebook
        ProfileLinkMark.Steam -> R.drawable.ic_profile_steam
        ProfileLinkMark.Boosty -> R.drawable.ic_profile_boosty
        ProfileLinkMark.Telegram -> R.drawable.ic_profile_telegram
    }

internal fun openProfileLink(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        AppLog.w(TAG, "profile link unavailable")
    }
}

private const val TAG = "TwitchAuth"
