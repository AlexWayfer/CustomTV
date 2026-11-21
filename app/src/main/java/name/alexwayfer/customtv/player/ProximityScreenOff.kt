package name.alexwayfer.customtv.player

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.data.AppSettingsStore
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val PROXIMITY_WAKE_LOCK_TAG = "CustomTV:PlayerProximity"
// Android's display proximity controller caps the sensor's range at five centimeters.
private const val DISPLAY_PROXIMITY_THRESHOLD_CM = 5f
private val ProximityBlankingDelay = 3.seconds

internal fun shouldBlankPlayerScreen(
    settingEnabled: Boolean?,
    inPictureInPicture: Boolean,
    supported: Boolean,
): Boolean = settingEnabled == true && !inPictureInPicture && supported

internal fun proximityCovered(distance: Float, maximumRange: Float): Boolean =
    distance >= 0f && distance < minOf(maximumRange, DISPLAY_PROXIMITY_THRESHOLD_CM)

@OptIn(FlowPreview::class)
internal fun Flow<Boolean>.delayedProximityScreenOff(): Flow<Boolean> =
    distinctUntilChanged().debounce(timeout = { covered -> if (covered) ProximityBlankingDelay else Duration.ZERO })

@Composable
internal fun ProximityScreenOff(inPictureInPicture: Boolean) {
    val context = LocalContext.current
    val power = remember(context) { context.getSystemService(PowerManager::class.java) } ?: return
    val supported = remember(power) { power.supportsProximityScreenOff() }
    val settings by AppSettingsStore.restored.collectAsStateWithLifecycle()
    if (!shouldBlankPlayerScreen(settings?.blankScreenOnProximity, inPictureInPicture, supported)) return
    val sensors = remember(context) { context.getSystemService(SensorManager::class.java) } ?: return
    val proximity = remember(sensors) { sensors.getDefaultSensor(Sensor.TYPE_PROXIMITY) } ?: return
    val scope = rememberCoroutineScope()

    // The proximity wake lock blanks the display without putting the device to sleep.
    LifecycleResumeEffect(power, sensors, proximity) {
        val wakeLock = power.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, PROXIMITY_WAKE_LOCK_TAG)
        val job = scope.launch {
            try {
                sensors.coveredReadings(proximity).delayedProximityScreenOff().collect { covered ->
                    if (covered) {
                        // Keep the screen off until uncovered. Cancellation releases the lock in finally.
                        @Suppress("WakelockTimeout")
                        wakeLock.acquire()
                    } else if (wakeLock.isHeld) {
                        wakeLock.release()
                    }
                }
            } finally {
                if (wakeLock.isHeld) wakeLock.release()
            }
        }
        onPauseOrDispose { job.cancel() }
    }
}

private fun SensorManager.coveredReadings(proximity: Sensor): Flow<Boolean> = callbackFlow {
    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            trySend(proximityCovered(event.values[0], proximity.maximumRange))
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
    }
    if (!registerListener(listener, proximity, SensorManager.SENSOR_DELAY_NORMAL)) {
        close()
        return@callbackFlow
    }
    awaitClose { unregisterListener(listener) }
}

@Composable
internal fun rememberProximityScreenOffSupported(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        context.getSystemService(PowerManager::class.java)?.supportsProximityScreenOff() == true &&
            context.getSystemService(SensorManager::class.java)?.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null
    }
}

private fun PowerManager.supportsProximityScreenOff(): Boolean =
    isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)
