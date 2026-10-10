package name.alexwayfer.customtv.ui.watch

import android.content.Context
import android.os.SystemClock
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.AppSettingsStore
import name.alexwayfer.customtv.diagnostics.AppLog
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * Closes the open player after the time the user set, so the screen can go dark. One timer serves the stream and
 * the recording alike, and keeps running while the user switches between them.
 */
internal object SleepTimer {
    private val _endsAtMillis = MutableStateFlow<Long?>(null)

    /** When the timer fires, on the [SystemClock.elapsedRealtime] clock; null while it is off. */
    val endsAtMillis: StateFlow<Long?> = _endsAtMillis

    // Outlives the dialog that starts the timer, so the time set is saved after it closes.
    private val saveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Starts the timer and saves [duration] for the picker to open on next time. */
    fun start(context: Context, duration: Duration) {
        _endsAtMillis.value = SystemClock.elapsedRealtime() + duration.inWholeMilliseconds
        AppLog.i(SLEEP_TIMER_LOG_TAG, "start")
        val store = AppSettingsStore(context)
        saveScope.launch {
            try {
                store.setSleepTimerMinutes(duration.inWholeMinutes.toInt())
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                AppLog.w(SLEEP_TIMER_LOG_TAG, "save failed: ${failure.javaClass.simpleName}")
            }
        }
    }

    fun cancel() {
        if (_endsAtMillis.value == null) return
        _endsAtMillis.value = null
        AppLog.i(SLEEP_TIMER_LOG_TAG, "cancel")
    }

    internal fun fired() {
        _endsAtMillis.value = null
        AppLog.i(SLEEP_TIMER_LOG_TAG, "fire")
    }
}

/**
 * Calls [onFire] once the sleep timer runs out; the app closes its players there. A minute before, a snackbar warns
 * and offers to cancel; after, another one says why the player closed. Picture-in-picture shows neither.
 */
@Composable
internal fun SleepTimerEffect(snackbarHostState: SnackbarHostState, inPictureInPicture: Boolean, onFire: () -> Unit) {
    // Not tied to the lifecycle: the timer fires while the app is in the background or in picture-in-picture too.
    val endsAtMillis by SleepTimer.endsAtMillis.collectAsState()
    // Closing the app, such as by swiping it out of the recent apps, turns the timer off: the process can outlive the
    // screen, and an old timer would close a player opened later. A rotation keeps it.
    val activity = LocalActivity.current
    DisposableEffect(activity) {
        onDispose {
            if (activity != null && activity.isFinishing && !activity.isChangingConfigurations) SleepTimer.cancel()
        }
    }
    val currentOnFire by rememberUpdatedState(onFire)
    val currentInPictureInPicture by rememberUpdatedState(inPictureInPicture)
    // The end of the timer the warning is about; a new or cancelled timer takes the warning away.
    var warningFor by remember { mutableStateOf<Long?>(null) }
    var closedNotice by remember { mutableStateOf<OneShotRequest<Unit>?>(null) }
    LaunchedEffect(endsAtMillis) {
        warningFor = null
        val endsAt = endsAtMillis ?: return@LaunchedEffect
        delay(sleepTimerWarningDelay(sleepTimerLeft(endsAt, SystemClock.elapsedRealtime())))
        if (!currentInPictureInPicture) warningFor = endsAt
        delay(sleepTimerLeft(endsAt, SystemClock.elapsedRealtime()))
        warningFor = null
        val inPictureInPicture = currentInPictureInPicture
        SleepTimer.fired()
        currentOnFire()
        if (!inPictureInPicture) closedNotice = OneShotRequest(Unit)
    }
    val warning = stringResource(R.string.sleep_timer_warning)
    val cancel = stringResource(R.string.cancel)
    LaunchedEffect(warningFor) {
        if (warningFor == null) return@LaunchedEffect
        // Stays until the timer fires or changes; leaving this effect then takes the snackbar away.
        val result = snackbarHostState.showSnackbar(
            message = warning,
            actionLabel = cancel,
            duration = SnackbarDuration.Indefinite,
        )
        if (result == SnackbarResult.ActionPerformed) SleepTimer.cancel()
    }
    val closed = stringResource(R.string.sleep_timer_closed)
    LaunchedEffect(closedNotice) {
        closedNotice?.take() ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(closed)
    }
}

/** How long a timer with [left] to run waits before it warns; at once when less than the warning time is left. */
internal fun sleepTimerWarningDelay(left: Duration): Duration =
    (left - SLEEP_TIMER_WARNING).coerceAtLeast(Duration.ZERO)

private val SLEEP_TIMER_WARNING = 1.minutes

internal const val SLEEP_TIMER_HOURS = 24
internal const val SLEEP_TIMER_MINUTE_STEP = 5
internal const val SLEEP_TIMER_MINUTE_STEPS = 60 / SLEEP_TIMER_MINUTE_STEP

/** The hours and minutes the picker shows. */
internal data class SleepTimerPick(val hours: Int, val minutes: Int)

/** What the picker sets; null for zero, which starts nothing. */
internal fun sleepTimerDuration(pick: SleepTimerPick): Duration? =
    (pick.hours.hours + pick.minutes.minutes).takeIf { it.isPositive() }

internal fun sleepTimerLeft(endsAtMillis: Long, nowMillis: Long): Duration =
    (endsAtMillis - nowMillis).coerceAtLeast(0L).milliseconds

/** The picker opens on the time left, rounded up to the minute wheel's step, or else on the time set last. */
internal fun sleepTimerPickerStart(left: Duration?, lastSet: Duration?): SleepTimerPick {
    val start = left ?: lastSet ?: Duration.ZERO
    val stepSeconds = SLEEP_TIMER_MINUTE_STEP * 60L
    val steps = ((start.inWholeSeconds + stepSeconds - 1) / stepSeconds)
        .coerceIn(0L, SLEEP_TIMER_HOURS * SLEEP_TIMER_MINUTE_STEPS - 1L)
    val totalMinutes = steps * SLEEP_TIMER_MINUTE_STEP
    return SleepTimerPick(hours = (totalMinutes / 60).toInt(), minutes = (totalMinutes % 60).toInt())
}

internal fun formatSleepTimerLeft(left: Duration): String =
    left.toComponents { hours, minutes, seconds, _ -> "%d:%02d:%02d".format(hours, minutes, seconds) }

private const val SLEEP_TIMER_LOG_TAG = "SleepTimer"
