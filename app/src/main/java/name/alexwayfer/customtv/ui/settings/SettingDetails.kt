package name.alexwayfer.customtv.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.diagnostics.AppLog

/**
 * Controls that belong to the switch above them: they unfold under it as it turns on and fold away as it turns off.
 * As the switch unfolds them, the list scrolls smoothly as far as it takes to show them whole; controls already
 * open when the screen opens leave the scroll where it is.
 */
@Composable
internal fun SettingDetails(visible: Boolean, content: @Composable () -> Unit) {
    var revealOnEnter by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (!visible) revealOnEnter = true
        AppLog.i(SETTING_DETAILS_LOG_TAG, if (visible) "unfold, scroll: $revealOnEnter" else "fold")
    }
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        val requester = remember { BringIntoViewRequester() }
        // Decided as the controls come in: the switch turning off sets the flag while they are still folding away,
        // and a scroll then would chase them.
        val scrollIntoView = remember { revealOnEnter }
        Column(Modifier.bringIntoViewRequester(requester)) { content() }
        if (scrollIntoView) {
            LaunchedEffect(Unit) {
                // The list grows a frame at a time as the controls unfold, and a request ends as soon as there is
                // nothing left to scroll, so a new one follows on every frame until the unfold is done.
                var request: Job? = null
                var requests = 0
                while (transition.currentState != EnterExitState.Visible) {
                    if (request?.isActive != true) {
                        requests++
                        request = launch { requester.bringIntoView() }
                    }
                    withFrameNanos { }
                }
                request?.join()
                requester.bringIntoView()
                AppLog.i(SETTING_DETAILS_LOG_TAG, "scrolled into view after ${requests + 1} requests")
            }
        }
    }
}

private const val SETTING_DETAILS_LOG_TAG = "SettingDetails"
