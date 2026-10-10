package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable

/** Per-channel notification choices are Premium; the free build sends no stream notifications. */
@Suppress("unused")
@Composable
internal fun ChannelAlertsButton(
    channelId: String?,
    onOpenSettings: () -> Unit,
    onMenuOpenChange: (Boolean) -> Unit,
) = Unit
