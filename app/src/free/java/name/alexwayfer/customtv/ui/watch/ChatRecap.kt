package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable

@Composable
internal fun ChatRecapRow(source: ChatRecapSource, onOpen: () -> Unit) = Unit

@Composable
internal fun ChatRecapSheet(source: ChatRecapSource, readableColors: Boolean, onDismiss: () -> Unit) = Unit

@Composable
internal fun ChatRecapPresenceEffect(channelId: String?, shown: Boolean) = Unit
