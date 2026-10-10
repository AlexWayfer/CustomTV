package name.alexwayfer.customtv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.diagnostics.Diagnostics
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

internal data class TwitchEmoteDetails(
    val type: String,
    val subscriptionTier: String?,
    val ownerDisplayName: String?,
)

internal object TwitchEmoteDetailsRepository {
    private val http = sharedHttpClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val cache = ConcurrentHashMap<String, TwitchEmoteDetails>()
    @Volatile
    private var globalsLoaded = false

    fun peek(id: String): TwitchEmoteDetails? = cache[id]

    suspend fun details(id: String): TwitchEmoteDetails? {
        cache[id]?.let { return it }
        val loaded = withContext(Dispatchers.IO) { fetch(id) } ?: return null
        if (loaded.globalSet) globalsLoaded = true
        cache.putAll(loaded.emotes)
        return cache[id]
    }

    private fun fetch(id: String): TwitchEmotePage? {
        val includeGlobals = !globalsLoaded
        val body = JSONObject()
            .put("query", emoteDetailsQuery(includeGlobals))
            .put("variables", JSONObject().put("id", id))
            .toString()
        val request = twitchGqlRequest(body).build()
        return http.newCall(request).execute().use { response ->
            val text = response.body.string()
            Diagnostics.reportGql("TwitchEmoteDetails", "emote details", response.code, text)
            if (!response.isSuccessful) return null
            val json = JSONObject(text)
            TwitchEmotePage(
                emotes = parse(json),
                globalSet = includeGlobals && json.optJSONObject("data")?.optJSONObject("emoteSet") != null,
            )
        }
    }

    internal fun parse(json: JSONObject): Map<String, TwitchEmoteDetails> {
        val result = LinkedHashMap<String, TwitchEmoteDetails>()
        val data = json.optJSONObject("data") ?: return result
        val emote = data.optJSONObject("emote")
        val ownerName = emote?.optJSONObject("owner").text("displayName")
        val owner = emote?.optJSONObject("owner")
        val products = owner?.optJSONArray("subscriptionProducts")
        if (products != null) {
            for (productIndex in 0 until products.length()) {
                putEmoteList(
                    result,
                    products.optJSONObject(productIndex)?.optJSONArray("emotes"),
                    ownerName,
                )
            }
        }
        val localSets = owner?.optJSONObject("channel")?.optJSONArray("localEmoteSets")
        if (localSets != null) {
            for (setIndex in 0 until localSets.length()) {
                putEmoteList(
                    result,
                    localSets.optJSONObject(setIndex)?.optJSONArray("emotes"),
                    ownerName,
                )
            }
        }
        if (emote != null) putEmote(result, emote, ownerName, replace = true)
        val globals = data.optJSONObject("emoteSet")?.optJSONArray("emotes")
        if (globals != null) {
            for (index in 0 until globals.length()) {
                val item = globals.optJSONObject(index) ?: continue
                putEmote(result, item, ownerDisplayName = null, replace = false)
            }
        }
        return result
    }

    private fun putEmoteList(
        into: MutableMap<String, TwitchEmoteDetails>,
        emotes: JSONArray?,
        ownerDisplayName: String?,
    ) {
        if (emotes == null) return
        for (index in 0 until emotes.length()) {
            val item = emotes.optJSONObject(index) ?: continue
            putEmote(into, item, ownerDisplayName, replace = false)
        }
    }

    private fun putEmote(
        into: MutableMap<String, TwitchEmoteDetails>,
        item: JSONObject,
        ownerDisplayName: String?,
        replace: Boolean,
    ) {
        val id = item.text("id") ?: return
        val type = item.text("type") ?: return
        val details = TwitchEmoteDetails(
            type = type,
            subscriptionTier = item.text("subscriptionTier"),
            ownerDisplayName = ownerDisplayName,
        )
        if (replace) into[id] = details else into.putIfAbsent(id, details)
    }

    internal fun emoteDetailsQuery(includeGlobals: Boolean): String {
        val globals = if (includeGlobals) """emoteSet(id:"0"){emotes{id type}}""" else ""
        return $$"query($id:ID!){emote(id:$id){id type subscriptionTier owner{displayName " +
            "subscriptionProducts{emotes{id type subscriptionTier}} " +
            "channel{localEmoteSets{emotes{id type subscriptionTier}}}}}$globals}"
    }

    private data class TwitchEmotePage(
        val emotes: Map<String, TwitchEmoteDetails>,
        val globalSet: Boolean,
    )
}

private fun JSONObject?.text(name: String): String? {
    val value = this?.optString(name) ?: return null
    return value.takeIf { it.isNotBlank() && it != "null" }
}
