package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChannelPanelsLoad
import name.alexwayfer.customtv.data.ChannelPanelsRepository
import name.alexwayfer.customtv.data.donationPanels
import name.alexwayfer.customtv.ui.PremiumBannerRequest
import name.alexwayfer.customtv.ui.PremiumLockIcon

/**
 * Send a donation as Premium shows it, with a lock, once one of the channel's About panels links to a donation page:
 * a tap shows the Premium banner. The panels come from the same cache as the About tab, filled when the stream opens.
 */
@Suppress("unused")
@Composable
internal fun ChannelPanelRows(
    channelId: String?,
    sheetHeightPx: () -> Int,
    openSubpage: (ChatMenuSubpage) -> Unit,
) {
    val panels by produceState(channelId?.let(ChannelPanelsRepository::cached), channelId) {
        if (value == null && channelId != null) {
            (ChannelPanelsRepository.load(channelId) as? ChannelPanelsLoad.Loaded)?.let { value = it.panels }
        }
    }
    AnimatedVisibility(
        visible = panels?.let(::donationPanels).orEmpty().isNotEmpty(),
        // From the top, so the row unfolds downward instead of sliding in as if scrolled.
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
    ) {
        ChatSettingsRow(
            label = stringResource(R.string.send_donation),
            icon = painterResource(R.drawable.ic_volunteer_activism),
            modifier = Modifier.clickable(role = Role.Button) { PremiumBannerRequest.send("the chat menu's donation") },
            trailing = { PremiumLockIcon() },
        )
    }
}
