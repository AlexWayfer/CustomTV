package name.alexwayfer.customtv

import androidx.compose.ui.unit.IntRect

internal const val PIP_VIDEO_RATIO_WIDTH = 16
internal const val PIP_VIDEO_RATIO_HEIGHT = 9

/** The visible video inside its player, without the letterboxing around it. */
internal fun pipVideoBounds(player: IntRect): IntRect? {
    if (player.width < PIP_VIDEO_RATIO_WIDTH || player.height < PIP_VIDEO_RATIO_HEIGHT) return null
    val width = minOf(
        player.width.toLong(),
        player.height.toLong() * PIP_VIDEO_RATIO_WIDTH / PIP_VIDEO_RATIO_HEIGHT,
    ).toInt()
    val height = (width.toLong() * PIP_VIDEO_RATIO_HEIGHT / PIP_VIDEO_RATIO_WIDTH).toInt()
    val left = player.left + (player.width - width) / 2
    val top = player.top + (player.height - height) / 2
    return IntRect(left, top, left + width, top + height)
}

/** Each composed player owns its source, so disposing an old channel cannot clear the new player's bounds. */
internal class PipVideoSource {
    var bounds: IntRect? = null
}

/** Only the current player's changes reach Android, and identical bounds need no new PiP transaction. */
internal class PipVideoSources(private val onBoundsChanged: () -> Unit) {
    private var current: PipVideoSource? = null
    var bounds: IntRect? = null
        private set

    fun bind(source: PipVideoSource) {
        current = source
        update(source)
    }

    fun clear(source: PipVideoSource) {
        if (current !== source) return
        current = null
        report(null)
    }

    fun update(source: PipVideoSource) {
        if (current === source) report(source.bounds)
    }

    private fun report(next: IntRect?) {
        if (bounds == next) return
        bounds = next
        onBoundsChanged()
    }
}
