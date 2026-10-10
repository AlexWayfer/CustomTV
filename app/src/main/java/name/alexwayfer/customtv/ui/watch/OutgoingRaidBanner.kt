package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import name.alexwayfer.customtv.chat.OutgoingRaid
import name.alexwayfer.customtv.chat.raidSecondsRemaining
import name.alexwayfer.customtv.chat.raidTargetToOpen

@Composable
internal fun OutgoingRaidCountdown(
    raid: OutgoingRaid?,
    currentChannel: String,
    onOpen: (String) -> Unit,
    onFinished: () -> Unit,
) {
    val open = rememberUpdatedState(onOpen)
    val finished = rememberUpdatedState(onFinished)
    LaunchedEffect(raid?.id, raid?.goAtMillis, raid?.leaving) {
        val current = raid ?: return@LaunchedEffect
        // Sleep until the raid goes instead of polling; sleep again if the clock moved back.
        while (raidSecondsRemaining(current.goAtMillis, System.currentTimeMillis()) > 0) {
            delay((current.goAtMillis - System.currentTimeMillis()).coerceAtLeast(1L).milliseconds)
        }
        if (current.leaving) {
            raidTargetToOpen(currentChannel, current.targetLogin)?.let { open.value(it) }
            finished.value()
        }
    }
}
