package name.alexwayfer.customtv.ui.home

import android.os.Build
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.AppLinksPromptStore
import name.alexwayfer.customtv.links.appLinksNeedSetup
import name.alexwayfer.customtv.links.openAppLinkSettings
import name.alexwayfer.customtv.links.rememberAppLinksEnabled
import name.alexwayfer.customtv.ui.notifications.PromptAction
import name.alexwayfer.customtv.ui.notifications.PromptCard
import name.alexwayfer.customtv.ui.notifications.PromptVisibility

/**
 * Home offers twitch.tv links one card at a time: only once the cards above it are answered or gone, while the links
 * still need setting up, and until Not now puts it away for good.
 */
internal fun homeAppLinksPromptVisible(
    needSetup: Boolean,
    enabled: Boolean,
    dismissed: Boolean?,
    otherPromptPending: Boolean,
): Boolean = needSetup && !enabled && dismissed == false && !otherPromptPending

/** A card above this one is on screen, or may still appear once its stored answer is read. */
internal fun homePromptPending(dismissed: Boolean?, visible: Boolean): Boolean = dismissed == null || visible

/** The card on Home that offers to open twitch.tv links in the app; [otherPromptPending] holds it back. */
@Composable
internal fun HomeAppLinksPrompt(otherPromptPending: Boolean) {
    val context = LocalContext.current
    val store = remember(context) { AppLinksPromptStore(context) }
    val dismissed by store.dismissed.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val visible = homeAppLinksPromptVisible(
        needSetup = appLinksNeedSetup(Build.VERSION.SDK_INT),
        enabled = rememberAppLinksEnabled(),
        dismissed = dismissed,
        otherPromptPending = otherPromptPending,
    )
    PromptVisibility(visible) {
        PromptCard(
            text = stringResource(R.string.app_links_prompt),
            action = PromptAction(stringResource(R.string.app_links_prompt_set_up)) { openAppLinkSettings(context) },
            modifier = Modifier.padding(top = 16.dp),
            dismiss = PromptAction(stringResource(R.string.notification_prompt_dismiss)) {
                scope.launch { store.markDismissed() }
            },
            icon = painterResource(R.drawable.ic_profile_link),
        )
    }
}
