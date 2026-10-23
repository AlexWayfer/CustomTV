package name.alexwayfer.customtv.ui.components

import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Whether a screen reader such as TalkBack explores the screen by touch, updated as the user turns
 * it on or off while the composable is shown.
 */
@Composable
internal fun rememberScreenReaderOn(): Boolean {
    val context = LocalContext.current
    val manager: AccessibilityManager? = remember(context) {
        context.getSystemService(AccessibilityManager::class.java)
    }
    var on by remember(manager) { mutableStateOf(manager?.isTouchExplorationEnabled == true) }
    DisposableEffect(manager) {
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { enabled -> on = enabled }
        manager?.addTouchExplorationStateChangeListener(listener)
        // It may have changed between the first read and the listener.
        on = manager?.isTouchExplorationEnabled == true
        onDispose { manager?.removeTouchExplorationStateChangeListener(listener) }
    }
    return on
}
