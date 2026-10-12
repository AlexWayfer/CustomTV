package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** AI chatter portraits are a Premium feature: the free chatter card has no item for them. */
@Suppress("unused")
@Composable
internal fun ChatterCardPortraitItem(
    channelId: String?,
    userId: String?,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) = Unit

@Suppress("unused")
@Composable
internal fun ChatterCardPortraitPage(
    subject: ChatterPortraitSubject,
    onOpenSettings: (() -> Unit)?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) = Unit
