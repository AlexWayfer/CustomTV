package name.alexwayfer.customtv.player

import android.net.Uri
import androidx.media3.common.MediaMetadata

internal fun streamMediaMetadata(
    channel: String,
    displayName: String,
    appName: String,
    categoryName: String?,
    artwork: MediaArtwork,
): MediaMetadata = MediaMetadata.Builder()
    .setTitle(streamMediaTitle(channel, displayName))
    .setArtist(streamMediaArtist(appName, categoryName))
    .setArtworkUri(artwork.uri)
    .setMediaType(MediaMetadata.MEDIA_TYPE_VIDEO)
    .setIsPlayable(true)
    .apply {
        artwork.data?.let {
            setArtworkData(it, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        }
    }
    .build()

/** The picture the media controls show: where it comes from, and its bytes once loaded. */
internal class MediaArtwork(val uri: Uri?, val data: ByteArray?)

/** A recording shows its own title over the channel's name. */
internal fun recordingMediaMetadata(
    title: String,
    channel: String,
    displayName: String?,
    artwork: MediaArtwork,
): MediaMetadata = MediaMetadata.Builder()
    .setTitle(title)
    .setArtist(streamMediaTitle(channel, displayName))
    .setArtworkUri(artwork.uri)
    .setMediaType(MediaMetadata.MEDIA_TYPE_VIDEO)
    .setIsPlayable(true)
    .apply {
        artwork.data?.let {
            setArtworkData(it, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        }
    }
    .build()
