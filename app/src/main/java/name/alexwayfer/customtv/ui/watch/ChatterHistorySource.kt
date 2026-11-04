package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import name.alexwayfer.customtv.chat.ChatterHistory

/** A replay can read just the open card from disk, instead of retaining every chatter's history. */
internal val LocalChatterHistorySource = compositionLocalOf<(@Composable (String) -> State<ChatterHistory>)?> { null }

@Composable
internal fun rememberChatterHistory(login: String, history: StateFlow<ChatterHistory>): State<ChatterHistory> {
    val source = LocalChatterHistorySource.current
    return if (source == null) history.collectAsStateWithLifecycle() else source(login)
}
