package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.chat.EmotePickerSection

internal data class EmotePickerCatalog(
    val ready: Boolean,
    val sections: List<EmotePickerSection>,
)
