package name.alexwayfer.customtv.ui.watch

/** How tall a chat laid over a full screen video is. */
internal enum class FullscreenOverlayChatHeight {
    /** The latest few messages in the bottom corner. */
    Compact,

    /** The whole chat on its side of the video. */
    Full,

    /** Not laid over the video, or open for typing: the chat takes its full column and keeps its message bar. */
    None,
}
