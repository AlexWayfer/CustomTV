package name.alexwayfer.customtv.data

enum class LinkPreviewMode {
    None,
    Full,
    Compact,
}

fun storedLinkPreviewMode(value: String?): LinkPreviewMode = when (value) {
    LinkPreviewMode.Full.name -> LinkPreviewMode.Full
    LinkPreviewMode.Compact.name -> LinkPreviewMode.Compact
    else -> LinkPreviewMode.None
}
