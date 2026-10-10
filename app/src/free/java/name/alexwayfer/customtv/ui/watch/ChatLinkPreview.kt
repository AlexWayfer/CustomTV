package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import name.alexwayfer.customtv.data.LinkPreviewMode

/** Link previews are a Premium feature; the free build shows nothing under a chat row. */
@Suppress("unused")
@Composable
internal fun ChatRowLinkPreview(text: String, mode: LinkPreviewMode, alpha: Float) = Unit
