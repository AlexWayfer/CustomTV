package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

/** Temporary pins are a Premium feature; the free build keeps nothing for them. */
@Stable
internal class TemporaryHighlightPins {
    val readManuallyDismissedIds: (suspend () -> Set<String>)? = null
}

@Composable
internal fun rememberLiveTemporaryHighlightPins(chat: String, account: WatchAccount): TemporaryHighlightPins =
    remember(chat, account.userId, account.signedIn) { TemporaryHighlightPins() }
