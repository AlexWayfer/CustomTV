package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.chat.ChatterFollow
import name.alexwayfer.customtv.chat.FollowForChatMode
import name.alexwayfer.customtv.chat.SubForChatMode
import name.alexwayfer.customtv.ui.settings.SettingsScrollTarget

/** Where the stream screen leads: the player's moves and the app's other sections. */
data class WatchNavigation(
    val onMinimize: () -> Unit,
    val onExpand: () -> Unit,
    val onClose: () -> Unit,
    /** The full player covers the app, so the section under it may change unseen. */
    val onFullPlayerCovering: () -> Unit,
    val onFullPlayerExpanded: () -> Unit,
    val onMinimizeDragDismissesKeyboard: () -> Unit = {},
    val onLiveStatus: (Boolean) -> Unit = {},
    val onOpenChannel: (String) -> Unit = {},
    val onOpenChannelProfile: (String) -> Unit = {},
    val onOpenSettings: (SettingsScrollTarget) -> Unit = {},
    /** The player minimized over a section that opened from it, such as a channel profile or the settings. */
    val onMinimizeOverSection: () -> Unit = {},
    /** Back returns to the channel watched before, which the back stack holds, instead of minimizing. */
    val returnsToPreviousChannel: Boolean = false,
    val onReturnToPreviousChannel: () -> Unit = {},
)

/** The signed-in user, as the stream screen needs them: to send, follow, and pass the chat's modes. */
data class WatchAccount(
    val onLogIn: () -> Unit,
    val signedIn: Boolean = false,
    val userId: String? = null,
    val login: String? = null,
    val displayName: String? = null,
    val raidToken: suspend () -> String? = { null },
    val ownChatterFollow: suspend (broadcasterId: String) -> ChatterFollow? = { null },
    val readOwnFollow: suspend (broadcasterId: String) -> FollowForChatMode = { FollowForChatMode.Unknown },
    val readOwnSubscription: suspend (broadcasterId: String) -> SubForChatMode = { SubForChatMode.Unknown },
    val followedChannelIds: Set<String>? = null,
    val onFollowsChanged: () -> Unit = {},
)

/** How the stream screen shows: in picture-in-picture, in the corner, and above the app's navigation bar. */
data class WatchPlacement(
    val inPictureInPicture: Boolean = false,
    val minimized: Boolean = false,
    val appNavigationBarHeightPx: Int = 0,
)
