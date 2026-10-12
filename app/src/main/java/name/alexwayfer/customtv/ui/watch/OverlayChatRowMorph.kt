package name.alexwayfer.customtv.ui.watch

/**
 * How compact the full overlaid chat looks as it rises out of the corner or sinks back into it ([progress], from 0
 * to 1, read when drawn), and which of its messages the compact chat shows, oldest first: those take its bubbles,
 * the rest fade.
 */
internal class OverlayChatRowMorph(
    val progress: () -> Float,
    val compactIds: List<String>,
    val fadedId: String?,
)
