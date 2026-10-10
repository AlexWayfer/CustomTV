package name.alexwayfer.customtv.player

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import name.alexwayfer.customtv.diagnostics.AppLog

/**
 * Whether the screen is on and unlocked. A locked screen hides a picture-in-picture window without leaving that
 * mode, also while the lock screen lights up, such as when the phone is picked up.
 */
@Composable
internal fun rememberScreenUnlocked(lifecycleState: Lifecycle.State): State<Boolean> {
    val context = LocalContext.current.applicationContext
    val unlocked = remember { mutableStateOf(screenUnlocked(context)) }
    // No broadcast reports the unlock here: USER_PRESENT never reaches this receiver. The activity starting again
    // once the lock screen is gone reads the state then.
    LaunchedEffect(lifecycleState) { update(unlocked, context, "lifecycle $lifecycleState") }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                update(unlocked, context, intent?.action?.substringAfterLast('.').orEmpty())
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return unlocked
}

private fun update(unlocked: MutableState<Boolean>, context: Context, cause: String) {
    val now = screenUnlocked(context)
    val changed = unlocked.value != now
    unlocked.value = now
    // Screen broadcasts are rare, so each one is logged; a lifecycle step only when it changes the answer.
    if (changed || !cause.startsWith("lifecycle")) {
        AppLog.i(PLAYER_LOG_TAG, "Screen ${if (now) "unlocked" else "locked or off"} after $cause")
    }
}

private fun screenUnlocked(context: Context): Boolean {
    val interactive = context.getSystemService(PowerManager::class.java)?.isInteractive != false
    val locked = context.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true
    return interactive && !locked
}
