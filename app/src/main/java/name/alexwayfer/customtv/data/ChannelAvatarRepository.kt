package name.alexwayfer.customtv.data

import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import name.alexwayfer.customtv.diagnostics.AppLog
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.TimeSource

/** The log tag of the channel profiles: avatars, streams, and lookups. */
internal const val CHANNEL_AVATAR_TAG = "ChannelAvatar"

object ChannelAvatarRepository {
    private val cache = ConcurrentHashMap<String, ChannelProfile>()
    // Channels loaded in this run: their stream fields are current, unlike a profile restored at start.
    private val loadedIds = ConcurrentHashMap.newKeySet<String>()
    private val _profileUpdates = MutableSharedFlow<ChannelProfile>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val profileUpdates: SharedFlow<ChannelProfile> = _profileUpdates
    private val inFlight = ConcurrentHashMap<String, CompletableDeferred<ChannelProfile?>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val restored = CompletableDeferred<Unit>()
    private var store: ChannelProfileStore? = null

    fun init(context: Context) {
        if (store != null) return
        val profileStore = ChannelProfileStore(context.applicationContext)
        store = profileStore
        scope.launch {
            try {
                profileStore.loadAll().forEach { profile ->
                    cache[profile.login] = profile
                }
            } catch (error: IOException) {
                AppLog.w(CHANNEL_AVATAR_TAG, "profile restore failed: ${error.javaClass.simpleName}")
            } finally {
                restored.complete(Unit)
            }
        }
    }

    suspend fun awaitRestored() {
        restored.await()
    }

    fun cached(login: String): ChannelProfile? = cache[login.lowercase()]

    fun cachedById(id: String): ChannelProfile? = cache.values.firstOrNull { it.id == id }

    /** Whether this run has loaded the channel, so its cached stream is current rather than unknown. */
    fun loadedThisRun(id: String): Boolean = id in loadedIds

    fun applyStreamStatus(event: StreamStatusEvent) {
        val key = event.login.lowercase()
        val current = cache[key] ?: return
        val next = profileAfterStreamStatus(current, event) ?: return
        if (next == current) return
        cache[key] = next
        _profileUpdates.tryEmit(next)
    }

    suspend fun refresh(login: String, force: Boolean = false): ChannelProfile? {
        val key = login.lowercase()
        if (force) {
            return fetchAndCache(key)
        }
        val created = CompletableDeferred<ChannelProfile?>()
        val existing = inFlight.putIfAbsent(key, created)
        if (existing != null) {
            return existing.await()
        }
        return try {
            fetchAndCache(key).also { created.complete(it) }
        } finally {
            // A cancelled or failed load leaves the waiters the cached profile.
            if (!created.isCompleted) created.complete(cache[key])
            inFlight.remove(key, created)
        }
    }

    /**
     * Loads the channels by ID, so each comes back under its current login. A channel the
     * request could not load keeps its cached profile.
     */
    suspend fun refreshByIds(ids: Collection<String>): List<ChannelProfile> {
        val keys = ids.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (keys.isEmpty()) return emptyList()
        return withContext(NonCancellable + Dispatchers.IO) {
            val started = TimeSource.Monotonic.markNow()
            // One query holds only so many channels; the batches load side by side, and a failed one keeps its cache.
            val fetched = gqlAliasBatches(keys).map { batch ->
                async {
                    try {
                        ChannelProfileFetcher.many(batch).map { fetched ->
                            profileWithKeptDetails(fetched, fetched.id?.let(::cachedById) ?: cache[fetched.login])
                        }
                    } catch (failure: IOException) {
                        AppLog.w(CHANNEL_AVATAR_TAG, "profiles fetch failed: ${failure.javaClass.simpleName}")
                        emptyList()
                    }
                }
            }.awaitAll().flatten()
            val fetchedIn = started.elapsedNow()
            fetched.forEach { profile -> persistFetched(profile) }
            AppLog.i(
                CHANNEL_AVATAR_TAG,
                "fetched ${fetched.size} of ${keys.size} profiles in ${fetchedIn.inWholeMilliseconds} ms, " +
                    "saved in ${(started.elapsedNow() - fetchedIn).inWholeMilliseconds} ms",
            )
            keys.mapNotNull(::cachedById)
        }
    }

    suspend fun lookup(login: String): ChannelLookup {
        val key = login.trim().lowercase()
        if (key.isEmpty()) return ChannelLookup.NotFound
        return withContext(Dispatchers.IO) {
            try {
                val fetched = ChannelProfileFetcher.one(key)
                if (fetched == null) {
                    ChannelLookup.NotFound
                } else {
                    persistFetched(fetched)
                    ChannelLookup.Found(fetched)
                }
            } catch (_: IOException) {
                ChannelLookup.Unavailable
            }
        }
    }

    private suspend fun fetchAndCache(key: String): ChannelProfile? {
        return withContext(NonCancellable + Dispatchers.IO) {
            val fetched = try {
                ChannelProfileFetcher.one(key)
            } catch (_: IOException) {
                null
            }
            if (fetched != null) {
                persistFetched(fetched)
                fetched
            } else {
                cache[key]
            }
        }
    }

    private suspend fun persistFetched(fetched: ChannelProfile) {
        val key = fetched.login
        val previous = cache[key]
        cache[key] = fetched
        fetched.id?.let { id -> cache.entries.removeIf { it.key != key && it.value.id == id } }
        fetched.id?.let(loadedIds::add)
        val previousIdentity = previous?.identity()
        val fetchedIdentity = fetched.identity()
        if (fetchedIdentity != previousIdentity) {
            store?.upsert(fetchedIdentity)
        }
    }
}

/** The part of a profile worth saving: who the channel is, without its stream of the moment. */
internal fun ChannelProfile.identity(): ChannelProfile = copy(
    streamTitle = null,
    categoryName = null,
    streamStartedAtMillis = null,
    currentStreamVideoId = null,
    viewerCount = null,
    sharedViewerCount = null,
    collaboratorAvatarUrls = emptyList(),
    isLive = false,
    tags = emptyList(),
    chatRules = null,
)

sealed interface ChannelLookup {
    data class Found(val profile: ChannelProfile) : ChannelLookup
    data object NotFound : ChannelLookup
    data object Unavailable : ChannelLookup
}
