package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

/** Temporary pins are a Premium feature; the free build keeps nothing for them. */
@Stable
internal class TemporaryHighlightPins

@Suppress("unused")
@Composable
internal fun rememberTemporaryHighlightPins(chat: String): TemporaryHighlightPins = remember { TemporaryHighlightPins() }
