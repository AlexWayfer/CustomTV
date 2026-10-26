package name.alexwayfer.customtv.ui.whispers

import androidx.compose.runtime.Composable
import name.alexwayfer.customtv.ui.components.ChannelPreview

/** Whispers are a Premium feature: the free build has no whispers section and no notifications. */
@Composable
internal fun WhispersBackground(userId: String?, onOpenRequested: () -> Unit) = Unit

@Composable
internal fun WhispersSection(
    onClosed: () -> Unit,
    onOpenChannel: (ChannelPreview) -> Unit,
    onOpenSettings: () -> Unit,
) = Unit
