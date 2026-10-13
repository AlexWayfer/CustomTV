package name.alexwayfer.customtv.ui.watch

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import name.alexwayfer.customtv.chat.mentionAlertAudible
import name.alexwayfer.customtv.player.screenUnlocked

/**
 * Asks whether a mention may alert right now, by [mentionAlertAudible]. It follows the activity's own lifecycle, not
 * the screen's, so a mini player over another section still counts as on screen. A stop with the screen dark or
 * locked is the lock coming over the app; a stop with the screen unlocked is the user leaving it.
 */
@Composable
internal fun rememberMentionAlertAudible(): () -> Boolean {
    val context = LocalContext.current
    val lifecycle = (LocalActivity.current as? LifecycleOwner)?.lifecycle
    val stoppedByLock = remember { mutableStateOf(false) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> stoppedByLock.value = !screenUnlocked(context)
                Lifecycle.Event.ON_START -> stoppedByLock.value = false
                else -> Unit
            }
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }
    return remember(context, lifecycle) {
        {
            mentionAlertAudible(
                appStarted = lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) != false,
                stoppedByLock = stoppedByLock.value,
                screenUnlocked = screenUnlocked(context),
            )
        }
    }
}
