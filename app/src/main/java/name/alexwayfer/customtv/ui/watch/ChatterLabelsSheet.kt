package name.alexwayfer.customtv.ui.watch

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import name.alexwayfer.customtv.data.LabeledChatter
import name.alexwayfer.customtv.ui.components.NoSheetWindowDim
import name.alexwayfer.customtv.ui.components.rememberModalSheetState

/** The chatter card's labels page in a sheet of its own, for a profile or a conversation outside the chat. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatterLabelsSheet(userId: String, login: String, displayName: String, onDismiss: () -> Unit) {
    val sheetState = rememberModalSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        NoSheetWindowDim()
        ChatterCardLabelsPage(chatter = LabeledChatter(userId, login, displayName), onBack = null)
    }
}
