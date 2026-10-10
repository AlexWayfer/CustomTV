package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.compositionLocalOf
import name.alexwayfer.customtv.chat.LowTrustStatus

/** Suspicious chatters of the open live channel by Twitch user ID, for the rows of the chat pane. */
internal val LocalSuspiciousChatters = compositionLocalOf<Map<String, LowTrustStatus>> { emptyMap() }
