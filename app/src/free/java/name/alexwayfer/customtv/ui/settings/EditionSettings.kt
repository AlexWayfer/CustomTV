package name.alexwayfer.customtv.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import name.alexwayfer.customtv.BuildConfig
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.telegram.TelegramSession
import name.alexwayfer.customtv.telegram.telegramEditionGroupLink
import name.alexwayfer.customtv.ui.theme.PremiumGold
import kotlin.math.roundToInt

@Composable
internal fun EditionSettings() {
    PremiumFeatureList()
    PremiumButton(
        text = stringResource(R.string.premium_upgrade),
        onClick = premiumUpgradeOpener(),
        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
        colors = goldButtonColors(),
    )
}

/**
 * What Premium adds, as the settings and the Premium banner list it.
 * Add one bullet here when a feature moves into Premium or is added only there.
 */
@Composable
internal fun PremiumFeatureList(
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    lineSpacing: Dp = 4.dp,
    /** How much further in than the heading and the closing line the features sit. */
    featureIndent: Dp = 8.dp,
) {
    Column(modifier) {
        PremiumFeatureLines(style, lineSpacing, featureIndent)
    }
}

@Composable
private fun PremiumFeatureLines(style: TextStyle, lineSpacing: Dp, featureIndent: Dp) {
    PremiumBullet(
        icon = painterResource(R.drawable.ic_history),
        text = stringResource(R.string.premium_feature_past_broadcasts),
        style = style,
        lineSpacing = lineSpacing,
        indent = featureIndent,
    )
    PremiumBullet(
        icon = rememberVectorPainter(Icons.Outlined.Notifications),
        text = stringResource(R.string.premium_feature_notifications),
        style = style,
        lineSpacing = lineSpacing,
        indent = featureIndent,
    )
    PremiumBullet(
        icon = painterResource(R.drawable.ic_moderation_shield_outline),
        text = stringResource(R.string.premium_feature_moderation),
        style = style,
        lineSpacing = lineSpacing,
        indent = featureIndent,
    )
    PremiumBullet(
        icon = painterResource(R.drawable.ic_chat),
        text = stringResource(R.string.premium_feature_chat),
        style = style,
        lineSpacing = lineSpacing,
        indent = featureIndent,
    )
    PremiumBullet(
        icon = painterResource(R.drawable.ic_auto_awesome),
        text = stringResource(R.string.premium_feature_chatter_portraits),
        style = style,
        lineSpacing = lineSpacing,
        indent = featureIndent,
    )
    ExperimentalHeading(style, lineSpacing)
    PremiumBullet(
        icon = rememberVectorPainter(Icons.Outlined.Email),
        text = stringResource(R.string.premium_feature_whispers),
        style = style,
        lineSpacing = lineSpacing,
        indent = featureIndent,
    )
    PremiumBullet(
        icon = painterResource(R.drawable.ic_chat_channel_points),
        text = stringResource(R.string.premium_feature_channel_points),
        style = style,
        lineSpacing = lineSpacing,
        indent = featureIndent,
    )
    PremiumBullet(
        icon = painterResource(R.drawable.ic_poll),
        text = stringResource(R.string.premium_feature_poll_voting),
        style = style,
        lineSpacing = lineSpacing,
        indent = featureIndent,
    )
    PremiumLine(
        text = stringResource(R.string.premium_and_more),
        style = style,
        lineSpacing = lineSpacing,
        modifier = Modifier.padding(top = 8.dp),
    )
}

/** Opens where Upgrade to Premium leads, by the account's access to the Premium group. */
@Composable
internal fun premiumUpgradeOpener(): () -> Unit {
    val context = LocalContext.current
    val ui by TelegramSession.ui.collectAsStateWithLifecycle()
    val premiumTopic = telegramEditionGroupLink(
        premium = true,
        openChatId = BuildConfig.TELEGRAM_OPEN_CHAT_ID,
        openTopicId = BuildConfig.TELEGRAM_OPEN_TOPIC_ID,
        premiumChatId = BuildConfig.TELEGRAM_PREMIUM_CHAT_ID,
        premiumTopicId = BuildConfig.TELEGRAM_PREMIUM_TOPIC_ID,
    )
    return { openLink(context, premiumUpgradeLink(ui.premiumGroup, premiumTopic, premiumSwitchUrl(context))) }
}

/** Starts the group of Premium features that rely on the experimental login. */
@Composable
private fun ExperimentalHeading(style: TextStyle, lineSpacing: Dp) {
    Text(
        text = stringResource(R.string.premium_experimental),
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = lineSpacing * 3, bottom = lineSpacing)
            .semantics { heading() },
        style = style,
    )
}

/**
 * A feature with its icon in place of a bullet; wrapped lines start under the first line, not under the icon. The
 * [indent] sets it further in than the heading and the closing line, so the features read as a list under them.
 */
@Composable
private fun PremiumBullet(icon: Painter, text: String, style: TextStyle, lineSpacing: Dp, indent: Dp) {
    val density = LocalDensity.current
    val iconSize = with(density) { style.lineHeight.toDp() }
    // Half the cap height of Roboto above the baseline: the icon centers on the capitals, not on the line box.
    val capCenterPx = with(density) { (style.fontSize * 0.355f).toPx().roundToInt() }
    Row(
        modifier = Modifier
            .padding(start = 16.dp + indent, end = 16.dp, top = lineSpacing, bottom = lineSpacing)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier
                .size(iconSize)
                .alignBy { it.measuredHeight / 2 + capCenterPx },
            tint = PremiumGold,
        )
        Text(text = text, modifier = Modifier.alignByBaseline(), style = style)
    }
}

@Composable
private fun PremiumLine(text: String, style: TextStyle, lineSpacing: Dp, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(horizontal = 16.dp, vertical = lineSpacing),
        style = style,
    )
}
