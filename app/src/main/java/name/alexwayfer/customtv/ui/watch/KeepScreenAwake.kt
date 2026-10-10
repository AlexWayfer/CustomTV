package name.alexwayfer.customtv.ui.watch

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import name.alexwayfer.customtv.MainActivity

@Composable
internal fun KeepScreenAwake(activity: MainActivity) {
    DisposableEffect(activity) {
        val window = activity.window
        val acquired = screenAwakeAfterChange(screenAwakeHolders, acquire = true)
        screenAwakeHolders = acquired.holders
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.keepScreenOn = true
        onDispose {
            val released = screenAwakeAfterChange(screenAwakeHolders, acquire = false)
            screenAwakeHolders = released.holders
            if (!released.screenOn) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                window.decorView.keepScreenOn = false
            }
        }
    }
}

internal data class ScreenAwake(
    val holders: Int,
    val screenOn: Boolean,
)

internal fun screenAwakeAfterChange(holders: Int, acquire: Boolean): ScreenAwake {
    val next = if (acquire) holders + 1 else (holders - 1).coerceAtLeast(0)
    return ScreenAwake(holders = next, screenOn = next > 0)
}

private var screenAwakeHolders = 0
