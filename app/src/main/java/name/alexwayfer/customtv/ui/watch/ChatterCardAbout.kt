package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.auth.TwitchProfileLink
import name.alexwayfer.customtv.ui.account.ProfileLink
import name.alexwayfer.customtv.ui.account.openProfileLink

/** The chatter's About text and social links, each under its own title when Twitch has them. */
@Composable
internal fun ChatterCardAbout(about: String?, links: List<TwitchProfileLink>) {
    if (about != null) {
        ChatterCardSectionTitle(stringResource(R.string.profile_about), top = 8.dp)
        Text(
            text = about,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
    if (links.isNotEmpty()) {
        val context = LocalContext.current
        ChatterCardSectionTitle(
            stringResource(R.string.chatter_card_socials),
            top = if (about != null) 16.dp else 8.dp,
        )
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            links.forEach { link -> ProfileLink(link) { openProfileLink(context, link.url) } }
        }
    }
    // Room before what follows, such as Moderation or Report.
    if (about != null || links.isNotEmpty()) Spacer(Modifier.height(8.dp))
}

@Composable
private fun ChatterCardSectionTitle(text: String, top: Dp) {
    Text(
        text = text,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = top, bottom = 4.dp)
            .semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
    )
}
