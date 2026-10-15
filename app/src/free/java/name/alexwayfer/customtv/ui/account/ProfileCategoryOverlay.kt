package name.alexwayfer.customtv.ui.account

import androidx.compose.runtime.Composable
import name.alexwayfer.customtv.data.TwitchCategory

/** A category over a profile shows Explore's streams, which are Premium; the free build's tiles open nothing. */
@Suppress("unused")
@Composable
internal fun ProfileCategoryOverlay(
    category: TwitchCategory?,
    backEnabled: Boolean,
    onClose: () -> Unit,
    onOpenChannel: (String) -> Unit,
    onShowCategories: () -> Unit,
) = Unit
