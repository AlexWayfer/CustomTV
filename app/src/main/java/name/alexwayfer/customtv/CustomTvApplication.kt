package name.alexwayfer.customtv

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.decode.SvgDecoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.chat.TwitchEmoteInterceptor
import name.alexwayfer.customtv.data.AppSettingsStore
import name.alexwayfer.customtv.data.AssetFiles
import name.alexwayfer.customtv.data.ChatSettingsStore
import name.alexwayfer.customtv.data.ChannelAvatarRepository
import name.alexwayfer.customtv.data.ProfileDetailsRepository
import name.alexwayfer.customtv.data.ExperimentalFeature
import name.alexwayfer.customtv.data.FollowedChannelsAtStart
import name.alexwayfer.customtv.data.HiddenCategoriesStore
import name.alexwayfer.customtv.data.ChatterLabelsRepository
import name.alexwayfer.customtv.data.WhispersFeature
import name.alexwayfer.customtv.data.RecentChannelsStore
import name.alexwayfer.customtv.data.StreamStartAlertsController
import name.alexwayfer.customtv.data.TwitchLanguage
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import name.alexwayfer.customtv.update.AppUpdates
import name.alexwayfer.customtv.widget.LiveWidgets

/** The share of the app's memory that decoded images may hold. */
private const val IMAGE_MEMORY_CACHE_SHARE = 0.15
private const val IMAGE_DISK_CACHE_BYTES = 64L * 1024 * 1024

class CustomTvApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        AppLog.start(this)
        Diagnostics.start(this)
        TwitchLanguage.init(this)
        ChannelAvatarRepository.init(this)
        ProfileDetailsRepository.start(this)
        AssetFiles.init(this)
        ChatterLabelsRepository.init(this)
        AppSettingsStore.start(this)
        ChatSettingsStore.start(this)
        HiddenCategoriesStore.start(this)
        StreamStartAlertsController.start(this)
        LiveWidgets.start(this)
        WhispersFeature.start(this)
        ExperimentalFeature.start(this)
        RecentChannelsStore.start(this)
        FollowedChannelsAtStart.start(this)
        AppUpdates.start(this)
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .respectCacheHeaders(false)
                .crossfade(false)
                .allowHardware(false)
                .components {
                    add(ImageDecoderDecoder.Factory())
                    add(SvgDecoder.Factory())
                    add(TwitchEmoteInterceptor())
                }
                .memoryCache {
                    MemoryCache.Builder(this)
                        .maxSizePercent(IMAGE_MEMORY_CACHE_SHARE)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("coil_avatars"))
                        .maxSizeBytes(IMAGE_DISK_CACHE_BYTES)
                        .build()
                }
                .build(),
        )
        warmRecentAvatars()
    }

    private fun warmRecentAvatars() {
        applicationScope.launch {
            val recents = RecentChannelsStore(this@CustomTvApplication).snapshot()
            val loader = Coil.imageLoader(this@CustomTvApplication)
            recents.mapNotNull { channel ->
                ChannelAvatarRepository.cachedById(channel.id)?.avatarUrl
            }.distinct().forEach { url ->
                loader.enqueue(
                    ImageRequest.Builder(this@CustomTvApplication)
                        .data(url)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                )
            }
        }
    }
}
