package name.alexwayfer.customtv.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import name.alexwayfer.customtv.data.TwitchCategory
import name.alexwayfer.customtv.ui.components.ChannelPreview

@Composable
internal fun PremiumVideoPlayer(
    playback: ProfileVideoPlayback?,
    onClose: () -> Unit,
    onOpenChannelProfile: (ChannelPreview) -> Unit,
    onOpenCategory: (TwitchCategory) -> Unit,
    modifier: Modifier = Modifier,
    minimized: Boolean = false,
    onMinimize: () -> Unit = {},
    onExpand: () -> Unit = {},
    onExpanded: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    appNavigationBarHeightPx: Int = 0,
    inPictureInPicture: Boolean = false,
    selfLogin: String = "",
    selfDisplayName: String = "",
    selfUserId: String? = null,
) = Unit
