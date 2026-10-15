package name.alexwayfer.customtv.ui.notifications

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.NotificationPromptStore

/**
 * Offers update and crash notifications on Home while they are off, until Not now or a refusal answers it; turning
 * them on later clears that answer.
 */
@Composable
internal fun HomeNotificationPrompt() {
    val context = LocalContext.current
    val store = remember(context) { NotificationPromptStore(context) }
    val dismissed by store.dismissed.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val enabler = rememberNotificationEnabler(firstAsk = true) { scope.launch { store.markDismissed() } }
    PromptVisibility(homeNotificationPromptVisible(enabler.enabled, dismissed)) {
        PromptCard(
            text = stringResource(R.string.notification_prompt),
            action = PromptAction(stringResource(R.string.notification_prompt_enable), enabler.enable),
            modifier = Modifier.padding(top = 16.dp),
            dismiss = PromptAction(stringResource(R.string.notification_prompt_dismiss)) {
                scope.launch { store.markDismissed() }
            },
        )
    }
}

/** Keeps the offer in Settings for as long as notifications are off, answered on Home or not. */
@Composable
internal fun SettingsNotificationPrompt() {
    val enabler = rememberNotificationEnabler(firstAsk = false)
    PromptVisibility(!enabler.enabled) {
        PromptCard(
            text = stringResource(R.string.notification_prompt),
            action = PromptAction(stringResource(R.string.notification_prompt_enable), enabler.enable),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        )
    }
}
