package name.alexwayfer.customtv.ui.account

import androidx.compose.runtime.Composable
import name.alexwayfer.customtv.data.TwitchCategory
import name.alexwayfer.customtv.ui.components.ChannelPreview

/** A category over a profile shows Explore's streams, which are Premium; the free build's tiles open nothing. */
@Composable
internal fun ProfileCategoryOverlay(
    category: TwitchCategory?,
    backEnabled: Boolean,
    onClose: () -> Unit,
    onOpenChannel: (ChannelPreview) -> Unit,
    onShowCategories: () -> Unit,
) = Unit
