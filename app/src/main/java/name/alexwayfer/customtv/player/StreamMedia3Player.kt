package name.alexwayfer.customtv.player

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.data.ChannelAvatarRepository
import name.alexwayfer.customtv.data.ChannelProfile

@OptIn(UnstableApi::class)
internal class StreamMedia3Player(
    looper: Looper,
    private val appName: String,
    private val appContext: Context,
    private val scope: CoroutineScope,
) : SimpleBasePlayer(looper) {
    private val handler = Handler(looper)
    private var target: TwitchPlaybackTarget? = null
    private var displayName: String? = null
    private var categoryName: String? = null
    private var artworkUri: Uri? = null
    private var artworkData: ByteArray? = null
    private var artworkJob: Job? = null
    private var handlingCommand = false
    private val streamCommands = Player.Commands.Builder()
        .addAll(
            COMMAND_PLAY_PAUSE,
            COMMAND_STOP,
            COMMAND_GET_CURRENT_MEDIA_ITEM,
            COMMAND_GET_METADATA,
            COMMAND_GET_TIMELINE,
        )
        .build()
    private val recordingCommands = streamCommands.buildUpon()
        .add(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
        .build()

    override fun getState(): State {
        val target = target
        val bound = target != null && StreamPlayback.isBound()
        val playbackConfirmed = bound && StreamPlayback.isPlaybackConfirmed()
        val playWhenReady = playbackConfirmed && !StreamPlayback.userPaused
        val playbackState = when {
            !playbackConfirmed -> STATE_IDLE
            StreamPlayback.isBuffering() -> STATE_BUFFERING
            else -> STATE_READY
        }
        return State.Builder()
            .setAvailableCommands(if (target is TwitchPlaybackTarget.Video) recordingCommands else streamCommands)
            .setPlayWhenReady(playWhenReady, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(playbackState)
            .apply {
                if (playbackConfirmed) {
                    setPlaylist(listOf(mediaItemData(target)))
                    setCurrentMediaItemIndex(0)
                    if (target is TwitchPlaybackTarget.Video) {
                        setContentPositionMs(PositionSupplier { StreamPlayback.positionMs() })
                    }
                }
            }
            .build()
    }

    private fun mediaItemData(target: TwitchPlaybackTarget): MediaItemData {
        val metadata = when (target) {
            is TwitchPlaybackTarget.Channel -> streamMediaMetadata(
                channel = target.login,
                displayName = streamMediaTitle(target.login, displayName),
                appName = appName,
                categoryName = categoryName,
                artworkUri = artworkUri,
                artworkData = artworkData,
            )
            is TwitchPlaybackTarget.Video -> recordingMediaMetadata(
                title = target.title,
                channel = target.channel,
                displayName = displayName,
                artworkUri = artworkUri,
                artworkData = artworkData,
            )
        }
        val mediaItem = MediaItem.Builder()
            .setMediaId(target.key)
            .setMediaMetadata(metadata)
            .build()
        val builder = MediaItemData.Builder(target.key)
            .setMediaItem(mediaItem)
            .setMediaMetadata(metadata)
        when (target) {
            is TwitchPlaybackTarget.Channel -> builder.setIsDynamic(true)
            is TwitchPlaybackTarget.Video -> {
                val durationMs = recordingDurationMs(target.durationSeconds, StreamPlayback.positionMs())
                builder
                    .setIsSeekable(true)
                    .setDurationUs(durationMs?.let { it * 1_000L } ?: C.TIME_UNSET)
            }
        }
        return builder.build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        handlingCommand = true
        try {
            if (playWhenReady) StreamPlayback.play() else StreamPlayback.pause()
        } finally {
            handlingCommand = false
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        if (positionMs == C.TIME_UNSET) return Futures.immediateVoidFuture()
        handlingCommand = true
        try {
            StreamPlayback.seekTo(positionMs)
        } finally {
            handlingCommand = false
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        handlingCommand = true
        try {
            StreamPlayback.pause()
        } finally {
            handlingCommand = false
        }
        return Futures.immediateVoidFuture()
    }

    fun setTarget(target: TwitchPlaybackTarget) {
        if (this.target == target) return
        this.target = target
        artworkJob?.cancel()
        artworkUri = null
        artworkData = null
        val channel = when (target) {
            is TwitchPlaybackTarget.Channel -> target.login
            is TwitchPlaybackTarget.Video -> target.channel
        }
        applyProfile(ChannelAvatarRepository.cached(channel))
        syncFromPlayback()
        artworkJob = scope.launch {
            when (target) {
                is TwitchPlaybackTarget.Channel -> {
                    refreshStreamArtwork(target, allowAvatarFallback = true)
                    while (isActive && target == this@StreamMedia3Player.target) {
                        delay(StreamMediaArtwork.PREVIEW_MAX_AGE)
                        if (target != this@StreamMedia3Player.target) return@launch
                        refreshStreamArtwork(target, allowAvatarFallback = false)
                    }
                }
                is TwitchPlaybackTarget.Video -> refreshRecordingArtwork(target)
            }
        }
    }

    private suspend fun refreshStreamArtwork(target: TwitchPlaybackTarget.Channel, allowAvatarFallback: Boolean) {
        val channel = target.login
        coroutineScope {
            val fetchedAt = System.currentTimeMillis()
            val jpegDeferred = async(Dispatchers.IO) {
                StreamMediaArtwork.loadJpeg(
                    appContext,
                    StreamMediaArtwork.previewUrl(channel, fetchedAt),
                    ChannelAvatarRepository.cached(channel)?.avatarUrl?.takeIf { allowAvatarFallback },
                )
            }
            val profile = if (allowAvatarFallback) {
                ChannelAvatarRepository.refresh(channel)
                    ?: ChannelAvatarRepository.cached(channel)
            } else {
                ChannelAvatarRepository.cached(channel)
            }
            if (target != this@StreamMedia3Player.target) return@coroutineScope
            applyProfile(profile)
            syncFromPlayback()
            val jpeg = jpegDeferred.await()
            if (target != this@StreamMedia3Player.target) return@coroutineScope
            if (jpeg != null && !jpeg.contentEquals(artworkData)) {
                artworkUri = StreamMediaArtwork.previewUrl(channel, fetchedAt).toUri()
                artworkData = jpeg
                syncFromPlayback()
            }
        }
    }

    private suspend fun refreshRecordingArtwork(target: TwitchPlaybackTarget.Video) {
        coroutineScope {
            val jpegDeferred = async(Dispatchers.IO) {
                StreamMediaArtwork.loadJpeg(
                    appContext,
                    target.thumbnailUrl,
                    ChannelAvatarRepository.cached(target.channel)?.avatarUrl,
                )
            }
            val profile = ChannelAvatarRepository.refresh(target.channel)
                ?: ChannelAvatarRepository.cached(target.channel)
            if (target != this@StreamMedia3Player.target) return@coroutineScope
            applyProfile(profile)
            syncFromPlayback()
            val jpeg = jpegDeferred.await() ?: return@coroutineScope
            if (target != this@StreamMedia3Player.target) return@coroutineScope
            artworkUri = target.thumbnailUrl?.toUri()
            artworkData = jpeg
            syncFromPlayback()
        }
    }

    private fun applyProfile(profile: ChannelProfile?) {
        displayName = profile?.displayName
        categoryName = profile?.categoryName
    }

    fun syncFromPlayback() {
        if (handlingCommand) return
        if (Looper.myLooper() == handler.looper) invalidateState() else handler.post(::invalidateState)
    }
}
