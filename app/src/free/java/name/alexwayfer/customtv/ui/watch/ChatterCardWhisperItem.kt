package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Whispers are a Premium feature: the free chatter card has no Whisper item. */
@Suppress("unused")
@Composable
internal fun ChatterCardWhisperItem(
    login: String,
    displayName: String,
    userId: String?,
    onOpened: () -> Unit,
    modifier: Modifier = Modifier,
) = Unit
