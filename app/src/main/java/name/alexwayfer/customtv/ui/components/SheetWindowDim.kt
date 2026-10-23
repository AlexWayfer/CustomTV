package name.alexwayfer.customtv.ui.components

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

/**
 * Takes the system dim off the window of the `ModalBottomSheet` it is called in. The sheet already fades its scrim as
 * it hides, while the window's dim stays until the window closes, after the hide animation has settled, and darkens
 * what is under it for a second or two after the sheet has gone. Call it first in every sheet's content.
 */
@Composable
internal fun NoSheetWindowDim() {
    val view = LocalView.current
    SideEffect {
        (view.parent as? DialogWindowProvider)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }
}
