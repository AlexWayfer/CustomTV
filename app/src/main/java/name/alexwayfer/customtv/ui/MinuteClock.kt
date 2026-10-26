package name.alexwayfer.customtv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.minutes

/** The wall clock, ticking each minute, so "5 minutes ago" keeps counting while it is on screen. */
@Composable
internal fun rememberMinuteClock(): Long {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1.minutes)
        }
    }
    return nowMillis
}
