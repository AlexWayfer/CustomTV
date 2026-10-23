package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Whispers are a Premium feature: the free build has no unread whispers to glow for. */
@Composable
internal fun WhisperUnreadGlow(
    minimized: Boolean,
    inPictureInPicture: Boolean,
    fade: () -> Float,
    modifier: Modifier = Modifier,
) = Unit
