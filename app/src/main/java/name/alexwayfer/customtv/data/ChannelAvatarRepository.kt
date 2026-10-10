package name.alexwayfer.customtv.data

import android.content.Context
import kotlinx.coroutines.CancellationException
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
import okhttp3.CacheControl
import org.json.JSONObject
import name.alexwayfer.customtv.chat.ChatReward
import name.alexwayfer.customtv.diagnostics.Diagnostics
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.time.TimeSource

object ChannelAvatarRepository {
    private val http = sharedHttpClient.newBuilder()
        .cache(null)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val cache = ConcurrentHashMap<String, ChannelProfile>()
    // Channels loaded in this run: their stream fields are current, unlike a profile restored at start.
    private val loadedIds = ConcurrentHashMap.newKeySet<String>()
    private val _profileUpdates = MutableSharedFlow<ChannelProfile>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val profileUpdates: SharedFlow<ChannelProfile> = _profileUpdates
    private val customRewards = ConcurrentHashMap<String, Map<String, ChatReward>>()
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
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                AppLog.w(TAG, "profile restore failed: ${error.javaClass.simpleName}")
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

    fun customReward(login: String, id: String): ChatReward? {
        return customRewards[login.lowercase()]?.get(id)
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
        } catch (error: CancellationException) {
            if (!created.isCompleted) {
                created.complete(cache[key])
            }
            throw error
        } catch (error: Throwable) {
            created.completeExceptionally(error)
            throw error
        } finally {
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
                    runCatching { fetchMany(batch) }
                        .onFailure { failure -> AppLog.w(TAG, "profiles fetch failed: ${failure.javaClass.simpleName}") }
                        .getOrNull().orEmpty()
                }
            }.awaitAll().flatten()
            val fetchedIn = started.elapsedNow()
            fetched.forEach { profile -> persistFetched(profile) }
            AppLog.i(
                TAG,
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
                val fetched = fetchOne(key)
                if (fetched == null) {
                    ChannelLookup.NotFound
                } else {
                    persistFetched(fetched)
                    ChannelLookup.Found(fetched)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                ChannelLookup.Unavailable
            }
        }
    }

    private suspend fun fetchAndCache(key: String): ChannelProfile? {
        return withContext(NonCancellable + Dispatchers.IO) {
            val fetched = runCatching { fetchOne(key) }.getOrNull()
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
        val previousIdentity = previous?.copy(
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
        val fetchedIdentity = fetched.copy(
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
        if (fetchedIdentity != previousIdentity) {
            store?.upsert(fetchedIdentity)
        }
    }

    private fun fetchOne(login: String): ChannelProfile? {
        val payload = JSONObject()
            .put(
                "query",
                $$"query($login:String!){user(login:$login){id login displayName profileImageURL(width:150) primaryColorHex stream{title createdAt archiveVideo{id} viewersCount collaborationViewersCount game{name} freeformTags{name}} broadcastSettings{title game{name}} channel{collaboration{collaborators{status user{login profileImageURL(width:150)}}} communityPointsSettings{image{url} defaultImage{url} automaticRewards{type cost defaultCost backgroundColor} customRewards{id title prompt cost backgroundColor image{url} defaultImage{url}}}} chatSettings{rules}}}",
            )
            .put("variables", JSONObject().put("login", login))
        val body = executeGql(payload)
        val parsed = ChannelProfileParser.parse(body, login) ?: return null
        customRewards[parsed.profile.login] = parsed.customRewards
        return parsed.profile
    }

    private fun fetchMany(ids: List<String>): List<ChannelProfile> {
        val variables = JSONObject()
        val declarations = ids.indices.joinToString(",") { index -> $$"$id$$index:ID!" }
        val users = ids.mapIndexed { index, id ->
            variables.put("id$index", id)
            $$"user$$index:user(id:$id$$index){$$HOME_PROFILE_FIELDS}"
        }
        val payload = JSONObject()
            .put("query", "query($declarations){${users.joinToString("")}}")
            .put("variables", variables)
        val data = JSONObject(executeGql(payload)).optJSONObject("data") ?: return emptyList()
        return ids.indices.mapNotNull { index ->
            val user = data.optJSONObject("user$index") ?: return@mapNotNull null
            val login = user.optString("login").takeIf { it.isNotBlank() && it != "null" }
                ?: return@mapNotNull null
            mergeRecentProfile(ChannelProfileParser.parseUser(user, login).profile)
        }
    }

    private fun mergeRecentProfile(fetched: ChannelProfile): ChannelProfile {
        val previous = fetched.id?.let(::cachedById) ?: cache[fetched.login] ?: return fetched
        return fetched.copy(
            highlightColorHex = previous.highlightColorHex ?: fetched.highlightColorHex,
            primaryColorHex = fetched.primaryColorHex ?: previous.primaryColorHex,
            highlightRewardCost = previous.highlightRewardCost ?: fetched.highlightRewardCost,
            channelPointsIconUrl = previous.channelPointsIconUrl,
            tags = fetched.tags.ifEmpty { previous.tags },
            chatRules = fetched.chatRules ?: previous.chatRules,
        )
    }

    private fun executeGql(payload: JSONObject): String {
        val request = twitchGqlRequest(payload.toString())
            .header("Client-Request-Id", UUID.randomUUID().toString())
            .header("Cache-Control", "no-cache")
            .header("Pragma", "no-cache")
            .cacheControl(CacheControl.FORCE_NETWORK)
            .build()
        http.newCall(request).execute().use { response ->
            val body = response.body.string()
            Diagnostics.reportGql(TAG, "channel profile", response.code, body)
            if (!response.isSuccessful) {
                throw IOException("Twitch GQL ${response.code}")
            }
            return body
        }
    }

    private const val TAG = "ChannelAvatar"
    private const val HOME_PROFILE_FIELDS =
        "id login displayName profileImageURL(width:150) primaryColorHex stream{title createdAt archiveVideo{id} viewersCount collaborationViewersCount game{name}} broadcastSettings{title game{name}} channel{collaboration{collaborators{status user{login profileImageURL(width:150)}}}}"
}

sealed class ChannelLookup {
    data class Found(val profile: ChannelProfile) : ChannelLookup()
    data object NotFound : ChannelLookup()
    data object Unavailable : ChannelLookup()
}
