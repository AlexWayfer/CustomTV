package name.alexwayfer.customtv.ui.home

import android.content.Context
import coil.imageLoader
import coil.request.ImageRequest
import name.alexwayfer.customtv.data.FollowedChannel
import name.alexwayfer.customtv.player.StreamMediaArtwork

internal data class FollowedSections(
    val live: List<FollowedChannel>,
    val offline: List<FollowedChannel>,
)

/** Live channels and offline ones, each in the list's order. */
internal fun followedSections(channels: List<FollowedChannel>): FollowedSections {
    val (live, offline) = channels.partition { it.isLive }
    return FollowedSections(live, offline)
}

/**
 * [next] with the preview time of each live channel: the one it had, in [next] or [previous], while it is younger
 * than the preview's max age, and [nowMillis] otherwise, so the shown preview stays until a newer one is due.
 */
internal fun followedWithPreviews(
    previous: List<FollowedChannel>,
    next: List<FollowedChannel>,
    nowMillis: Long,
): List<FollowedChannel> {
    val previousById = previous.associateBy { it.id }
    return next.map { channel ->
        val known = channel.previewAtMillis ?: previousById[channel.id]?.takeIf { it.isLive }?.previewAtMillis
        val previewAt = followedPreviewAt(channel.isLive, known, nowMillis)
        if (previewAt == channel.previewAtMillis) channel else channel.copy(previewAtMillis = previewAt)
    }
}

internal fun followedPreviewAt(isLive: Boolean, knownAtMillis: Long?, nowMillis: Long): Long? {
    if (!isLive) return null
    val maxAgeMillis = StreamMediaArtwork.PREVIEW_MAX_AGE.inWholeMilliseconds
    if (knownAtMillis != null && nowMillis - knownAtMillis in 0 until maxAgeMillis) return knownAtMillis
    return nowMillis
}

/** The preview a live channel shows; the time in it makes each new preview a new image cache entry. */
internal fun followedPreviewUrl(channel: FollowedChannel): String =
    StreamMediaArtwork.previewUrl(channel.login, channel.previewAtMillis)

/** Loads the live previews into the image cache, so the list shows them without a wait when scrolled to. */
internal fun prefetchFollowedPreviews(context: Context, channels: List<FollowedChannel>) {
    val loader = context.imageLoader
    channels.filter { it.isLive }.forEach { channel ->
        loader.enqueue(ImageRequest.Builder(context).data(followedPreviewUrl(channel)).build())
    }
}
