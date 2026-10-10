package name.alexwayfer.customtv.player

import android.net.Uri
import androidx.media3.common.MediaMetadata

internal fun streamMediaMetadata(
    channel: String,
    displayName: String,
    appName: String,
    categoryName: String?,
    artworkUri: Uri?,
    artworkData: ByteArray?,
): MediaMetadata = MediaMetadata.Builder()
    .setTitle(streamMediaTitle(channel, displayName))
    .setArtist(streamMediaArtist(appName, categoryName))
    .setArtworkUri(artworkUri)
    .setMediaType(MediaMetadata.MEDIA_TYPE_VIDEO)
    .setIsPlayable(true)
    .apply {
        artworkData?.let {
            setArtworkData(it, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        }
    }
    .build()

/** A recording shows its own title over the channel's name. */
internal fun recordingMediaMetadata(
    title: String,
    channel: String,
    displayName: String?,
    artworkUri: Uri?,
    artworkData: ByteArray?,
): MediaMetadata = MediaMetadata.Builder()
    .setTitle(title)
    .setArtist(streamMediaTitle(channel, displayName))
    .setArtworkUri(artworkUri)
    .setMediaType(MediaMetadata.MEDIA_TYPE_VIDEO)
    .setIsPlayable(true)
    .apply {
        artworkData?.let {
            setArtworkData(it, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        }
    }
    .build()
