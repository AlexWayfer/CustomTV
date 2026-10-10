package name.alexwayfer.customtv.ui.watch

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.AppSettingsStore
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** The clock in the player's top corner; sets, changes, or turns off the sleep timer. */
@Composable
internal fun BoxScope.PlayerSleepTimerButton(visible: Boolean, fullscreen: Boolean) {
    var dialogOpen by remember { mutableStateOf(false) }
    val running = SleepTimer.endsAtMillis.collectAsState().value != null
    // Over a full screen player it steps aside for the chat settings gear and lines up with it.
    val end by animateDpAsState(targetValue = if (fullscreen) 44.dp else 0.dp, label = "sleep timer button end")
    val top by animateDpAsState(
        targetValue = if (fullscreen) FullscreenTopButtonsTop else 0.dp,
        label = "sleep timer button top",
    )
    val tint by animateColorAsState(
        targetValue = if (running) TwitchPurple else Color.White,
        label = "sleep timer button tint",
    )
    val onState = stringResource(R.string.sleep_timer_on)
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = top, end = end),
        enter = HeaderFadeIn,
        exit = HeaderFadeOut,
    ) {
        IconButton(
            onClick = { dialogOpen = true },
            modifier = Modifier
                .size(44.dp)
                .semantics { if (running) stateDescription = onState },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_sleep_timer),
                contentDescription = stringResource(R.string.sleep_timer),
                tint = tint,
            )
        }
    }
    if (dialogOpen) {
        SleepTimerDialog(onDismiss = { dialogOpen = false })
    }
}

@Composable
private fun SleepTimerDialog(onDismiss: () -> Unit) {
    val endsAtMillis by SleepTimer.endsAtMillis.collectAsState()
    var nowMillis by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(endsAtMillis) {
        while (endsAtMillis != null) {
            nowMillis = SystemClock.elapsedRealtime()
            delay(1.seconds)
        }
    }
    val left = endsAtMillis?.let { sleepTimerLeft(it, nowMillis) }
    val context = LocalContext.current
    // Read once as the dialog opens; the settings are restored when the app starts.
    val start = remember {
        val lastSet = AppSettingsStore.restored.value?.sleepTimerMinutes?.minutes?.takeIf { it.isPositive() }
        sleepTimerPickerStart(left, lastSet)
    }
    var pick by remember { mutableStateOf(start) }
    val duration = sleepTimerDuration(pick)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sleep_timer)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (left != null) {
                    Text(
                        text = stringResource(R.string.sleep_timer_left, formatSleepTimerLeft(left)),
                        modifier = Modifier.padding(bottom = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SleepTimerWheel(
                        count = SLEEP_TIMER_HOURS,
                        step = 1,
                        initial = start.hours,
                        label = stringResource(R.string.sleep_timer_hours),
                        description = stringResource(R.string.sleep_timer_hours_description),
                        onValueChange = { pick = pick.copy(hours = it) },
                    )
                    SleepTimerWheel(
                        count = SLEEP_TIMER_MINUTE_STEPS,
                        step = SLEEP_TIMER_MINUTE_STEP,
                        initial = start.minutes,
                        label = stringResource(R.string.sleep_timer_minutes),
                        description = stringResource(R.string.sleep_timer_minutes_description),
                        onValueChange = { pick = pick.copy(minutes = it) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    duration?.let { SleepTimer.start(context, it) }
                    onDismiss()
                },
                enabled = duration != null,
            ) {
                Text(stringResource(R.string.sleep_timer_start))
            }
        },
        dismissButton = {
            Row {
                if (left != null) {
                    TextButton(
                        onClick = {
                            SleepTimer.cancel()
                            onDismiss()
                        },
                    ) {
                        Text(stringResource(R.string.sleep_timer_off))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
    )
}
