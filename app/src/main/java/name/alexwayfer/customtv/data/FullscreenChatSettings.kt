package name.alexwayfer.customtv.data

/** Where the chat goes while the player fills the screen. */
enum class FullscreenChatMode(val stored: String) {
    /** Over the video, on a see-through background. */
    Overlay("overlay"),

    /** Beside the video, which shrinks to the rest of the screen. */
    Column("column"),

    /** Not shown. */
    Hidden("hidden"),
}

/** Which side of the screen the full screen chat takes. */
enum class FullscreenChatSide(val stored: String) {
    Left("left"),
    Right("right"),
}

internal fun fullscreenChatModeFromStored(value: String?): FullscreenChatMode =
    FullscreenChatMode.entries.firstOrNull { it.stored == value } ?: FullscreenChatMode.Hidden

internal fun fullscreenChatSideFromStored(value: String?): FullscreenChatSide =
    FullscreenChatSide.entries.firstOrNull { it.stored == value } ?: FullscreenChatSide.Right
