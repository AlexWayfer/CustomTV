package name.alexwayfer.customtv.ui.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.ui.components.ChannelAvatar
import name.alexwayfer.customtv.ui.components.rememberLastNonNull
import name.alexwayfer.customtv.ui.watch.formatChatterCreatedDate
import java.time.ZoneId

/** The profile's large avatar, ringed in the background color where it overlaps the banner. */
@Composable
internal fun ProfileHeaderAvatar(login: String, displayName: String, avatarUrl: String?) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.background)
            .padding(4.dp),
    ) {
        ChannelAvatar(
            channel = login,
            profile = ChannelProfile(login = login, displayName = displayName, avatarUrl = avatarUrl),
            size = 96.dp,
        )
    }
}

/** A long name shrinks to one line instead of breaking mid-word. */
@Composable
internal fun ProfileHeaderName(displayName: String) {
    Text(
        text = displayName,
        color = MaterialTheme.colorScheme.onSurface,
        autoSize = TextAutoSize.StepBased(
            minFontSize = MaterialTheme.typography.titleMedium.fontSize,
            maxFontSize = MaterialTheme.typography.headlineSmall.fontSize,
        ),
        maxLines = 1,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

/** The login when it differs from the name, the follower count, and when the channel was last live. */
@Composable
internal fun ProfileHeaderDetails(login: String, displayName: String, followerCount: Int?, lastLiveMillis: Long?) {
    Column {
        if (!displayName.equals(login, ignoreCase = true)) {
            Text(
                text = login,
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        followerCount?.let { count ->
            val shownCount = formatFollowerCount(count, LocalConfiguration.current.locales[0])
            Text(
                text = profileStatText(
                    pluralStringResource(R.plurals.profile_followers, count, shownCount),
                    shownCount,
                ),
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        val shownLastLive = rememberLastNonNull(lastLiveMillis)
        AnimatedVisibility(visible = lastLiveMillis != null) {
            shownLastLive?.let { millis ->
                val date = when (val day = lastLiveDay(millis, System.currentTimeMillis(), ZoneId.systemDefault())) {
                    LastLiveDay.Today -> stringResource(R.string.profile_last_live_today)
                    LastLiveDay.Yesterday -> stringResource(R.string.profile_last_live_yesterday)
                    is LastLiveDay.DaysAgo -> pluralStringResource(
                        R.plurals.profile_last_live_days_ago,
                        day.days,
                        day.days,
                    )
                    LastLiveDay.OnDate -> formatChatterCreatedDate(millis)
                }
                Text(
                    text = profileStatText(stringResource(R.string.profile_last_live, date), date),
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
