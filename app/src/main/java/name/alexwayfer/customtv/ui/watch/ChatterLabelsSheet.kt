package name.alexwayfer.customtv.ui.watch

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import name.alexwayfer.customtv.ui.components.NoSheetWindowDim

/** The chatter card's labels page in a sheet of its own, for a profile or a conversation outside the chat. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatterLabelsSheet(userId: String, login: String, displayName: String, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        NoSheetWindowDim()
        ChatterCardLabelsPage(userId = userId, login = login, displayName = displayName, onBack = null)
    }
}
