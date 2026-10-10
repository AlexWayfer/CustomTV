package name.alexwayfer.customtv.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Suppress("unused")
@Composable
internal fun PremiumVideoPlayer(
    playback: ProfileVideoPlayback?,
    onClose: () -> Unit,
    onOpenChannelProfile: (channel: String) -> Unit,
    modifier: Modifier = Modifier,
    minimized: Boolean = false,
    onMinimize: () -> Unit = {},
    onExpand: () -> Unit = {},
    appNavigationBarHeightPx: Int = 0,
    inPictureInPicture: Boolean = false,
    selfLogin: String = "",
    selfDisplayName: String = "",
) = Unit
