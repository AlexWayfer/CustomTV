package name.alexwayfer.customtv.data

import android.content.Context
import name.alexwayfer.customtv.diagnostics.AppLog
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** What a recent channel was streaming at the last load: shown at start until a fresh load replaces it. */
internal data class RecentStream(
    val isLive: Boolean,
    val streamTitle: String? = null,
    val categoryName: String? = null,
    val streamStartedAtMillis: Long? = null,
    val viewerCount: Int? = null,
    val sharedViewerCount: Int? = null,
    val collaborationCount: Int? = null,
    val collaboratorAvatarUrls: List<String> = emptyList(),
)

internal fun ChannelProfile.recentStream(): RecentStream = RecentStream(
    isLive = isLive,
    streamTitle = streamTitle,
    categoryName = categoryName,
    streamStartedAtMillis = streamStartedAtMillis,
    viewerCount = viewerCount,
    sharedViewerCount = sharedViewerCount,
    collaborationCount = collaborationCount,
    collaboratorAvatarUrls = collaboratorAvatarUrls,
)

/** The profile's name and avatar with the saved stream in place of its own stream fields. */
internal fun ChannelProfile.withRecentStream(stream: RecentStream): ChannelProfile = copy(
    isLive = stream.isLive,
    streamTitle = stream.streamTitle,
    categoryName = stream.categoryName,
    streamStartedAtMillis = stream.streamStartedAtMillis,
    viewerCount = stream.viewerCount,
    sharedViewerCount = stream.sharedViewerCount,
    collaborationCount = stream.collaborationCount,
    collaboratorAvatarUrls = stream.collaboratorAvatarUrls,
)

/**
 * The saved stream of [profile] when this run has not loaded the channel yet: a profile restored at start holds
 * only the name and avatar. A loaded profile is current and stays as it is.
 */
internal fun recentProfileForDisplay(
    profile: ChannelProfile,
    streams: Map<String, RecentStream>,
    loaded: (String) -> Boolean,
): ChannelProfile {
    val id = profile.id ?: return profile
    if (loaded(id)) return profile
    return streams[id]?.let(profile::withRecentStream) ?: profile
}

/**
 * The streams to save for the recent channels [ids]: a channel loaded in this run gives its current stream, one the
 * load missed keeps its saved stream, and a channel no longer in the recents is dropped.
 */
internal fun recentStreamsAfterRefresh(
    previous: Map<String, RecentStream>,
    ids: List<String>,
    profiles: List<ChannelProfile>,
    loaded: (String) -> Boolean,
): Map<String, RecentStream> {
    val current = profiles.mapNotNull { profile ->
        profile.id?.takeIf(loaded)?.let { it to profile.recentStream() }
    }.toMap()
    return ids.mapNotNull { id -> (current[id] ?: previous[id])?.let { id to it } }.toMap()
}

internal class RecentChannelsSnapshot(context: Context) {
    private val file = File(context.applicationContext.filesDir, "recent-channels.json")

    fun read(): Map<String, RecentStream> {
        synchronized(gate) {
            if (!file.exists()) return emptyMap()
            val raw = runCatching { file.readText() }.getOrElse { failure ->
                AppLog.w(TAG, "recents snapshot read failed: ${failure.javaClass.simpleName}")
                return emptyMap()
            }
            return decodeRecentStreams(raw)
        }
    }

    fun write(streams: Map<String, RecentStream>) {
        synchronized(gate) {
            runCatching { file.writeText(encodeRecentStreams(streams)) }.onFailure { failure ->
                AppLog.w(TAG, "recents snapshot write failed: ${failure.javaClass.simpleName}")
            }
        }
    }

    private companion object {
        const val TAG = "RecentChannels"
        val gate = Any()
    }
}

internal fun encodeRecentStreams(streams: Map<String, RecentStream>): String {
    val root = JSONObject()
    streams.forEach { (id, stream) ->
        root.put(
            id,
            JSONObject()
                .put("isLive", stream.isLive)
                .putOpt("streamTitle", stream.streamTitle)
                .putOpt("categoryName", stream.categoryName)
                .putOpt("streamStartedAtMillis", stream.streamStartedAtMillis)
                .putOpt("viewerCount", stream.viewerCount)
                .putOpt("sharedViewerCount", stream.sharedViewerCount)
                .putOpt("collaborationCount", stream.collaborationCount)
                .put("collaboratorAvatarUrls", JSONArray(stream.collaboratorAvatarUrls)),
        )
    }
    return root.toString()
}

/** The saved streams by channel ID, or none when the file is unreadable. */
internal fun decodeRecentStreams(raw: String): Map<String, RecentStream> {
    val root = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyMap()
    return buildMap {
        root.keys().forEach { id ->
            val item = root.optJSONObject(id) ?: return@forEach
            if (id.isBlank()) return@forEach
            val collaborators = item.optJSONArray("collaboratorAvatarUrls")
            put(
                id,
                RecentStream(
                    isLive = item.optBoolean("isLive"),
                    streamTitle = item.stringOrNull("streamTitle"),
                    categoryName = item.stringOrNull("categoryName"),
                    streamStartedAtMillis = if (item.has("streamStartedAtMillis")) {
                        item.optLong("streamStartedAtMillis")
                    } else {
                        null
                    },
                    viewerCount = item.intOrNull("viewerCount"),
                    sharedViewerCount = item.intOrNull("sharedViewerCount"),
                    collaborationCount = item.intOrNull("collaborationCount"),
                    collaboratorAvatarUrls = (0 until (collaborators?.length() ?: 0)).mapNotNull { at ->
                        collaborators?.optString(at)?.takeIf { it.isNotBlank() }
                    },
                ),
            )
        }
    }
}

private fun JSONObject.stringOrNull(name: String): String? =
    if (has(name) && !isNull(name)) optString(name) else null

private fun JSONObject.intOrNull(name: String): Int? =
    if (has(name) && !isNull(name)) optInt(name) else null
