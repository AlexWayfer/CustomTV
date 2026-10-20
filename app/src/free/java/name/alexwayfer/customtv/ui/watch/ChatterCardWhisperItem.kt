package name.alexwayfer.customtv.ui.watch

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import name.alexwayfer.customtv.R

/** Whispers are a Premium feature: the free chatter card shows Whisper locked, and a tap shows the Premium banner. */
@Suppress("unused")
@Composable
internal fun ChatterCardWhisperItem(
    login: String,
    displayName: String,
    userId: String?,
    onOpened: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ChatterCardLockedItem(
        icon = rememberVectorPainter(Icons.Outlined.Email),
        title = stringResource(R.string.chatter_card_whisper),
        source = "the chatter card's whisper",
        modifier = modifier,
    )
}
