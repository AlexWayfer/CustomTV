package name.alexwayfer.customtv.data

/** The online model that writes AI chatter portraits with the viewer's own key. */
enum class PortraitModelKind {
    GroqQwen,
    GroqGptOss,
    GeminiFlash,
    GeminiFlashLite,
}

/** A model no longer offered, such as one that ran on the device, falls back to the default. */
fun storedPortraitModelKind(value: String?): PortraitModelKind =
    PortraitModelKind.entries.find { it.name == value } ?: PortraitModelKind.GroqQwen
