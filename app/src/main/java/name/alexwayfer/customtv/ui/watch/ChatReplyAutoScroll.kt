package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import java.util.concurrent.atomic.AtomicBoolean

@Composable
internal fun KeepSelectedMessageVisible(
    selected: Boolean,
    messageId: String,
    rowHeightPx: Int,
    bottomOverlayPx: Int,
    listState: LazyListState,
    autoScrolling: AtomicBoolean,
) {
    // Every row calls this; only the selected one needs the effect.
    if (!selected) return
    LaunchedEffect(messageId, rowHeightPx, bottomOverlayPx) {
        if (rowHeightPx <= 0) return@LaunchedEffect
        withFrameNanos { }
        val layout = listState.layoutInfo
        val item = layout.visibleItemsInfo.find { it.key == messageId } ?: return@LaunchedEffect
        val visibleBottom = layout.viewportEndOffset - bottomOverlayPx
        val overflow = item.offset + item.size - visibleBottom
        if (overflow <= 1) return@LaunchedEffect
        autoScrolling.set(true)
        try {
            listState.scrollBy(overflow.toFloat())
        } finally {
            autoScrolling.set(false)
        }
    }
}
