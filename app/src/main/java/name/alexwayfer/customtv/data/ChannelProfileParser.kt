package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.chat.ChatReward
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

internal object ChannelProfileParser {
    fun parse(body: String, requestedLogin: String): ParsedChannelProfile? {
        val user = JSONObject(body).optJSONObject("data")?.optJSONObject("user") ?: return null
        return parseUser(user, requestedLogin)
    }

    fun parseUser(user: JSONObject, requestedLogin: String): ParsedChannelProfile {
        val login = text(user, "login") ?: requestedLogin
        val displayName = text(user, "displayName") ?: login
        val stream = user.optJSONObject("stream")
        val broadcastSettings = user.optJSONObject("broadcastSettings")
        val points = user.optJSONObject("channel")?.optJSONObject("communityPointsSettings")
        val highlight = highlightReward(points)
        val profile = ChannelProfile(
            login = login.lowercase(),
            displayName = displayName,
            avatarUrl = text(user, "profileImageURL"),
            id = text(user, "id"),
            streamTitle = text(stream, "title") ?: text(broadcastSettings, "title"),
            categoryName = gameName(stream) ?: gameName(broadcastSettings),
            streamStartedAtMillis = text(stream, "createdAt")
                ?.let { raw -> runCatching { Instant.parse(raw).toEpochMilli() }.getOrNull() },
            currentStreamVideoId = text(stream?.optJSONObject("archiveVideo"), "id"),
            viewerCount = int(stream, "viewersCount"),
            sharedViewerCount = sharedViewerCount(stream),
            collaborationCount = collaborationCount(user, login),
            collaboratorAvatarUrls = collaboratorAvatarUrls(user, login),
            isLive = stream != null,
            highlightColorHex = highlight.backgroundColor ?: text(user, "primaryColorHex"),
            primaryColorHex = text(user, "primaryColorHex"),
            highlightRewardCost = highlight.cost,
            tags = streamTags(stream),
            chatRules = parseChatRules(user),
            channelPointsIconUrl = higherResPointsIcon(
                text(points?.optJSONObject("image"), "url")
                    ?: text(points?.optJSONObject("defaultImage"), "url"),
            ),
        )
        return ParsedChannelProfile(profile, customRewards(points))
    }

    private fun sharedViewerCount(stream: JSONObject?): Int? {
        return sharedStreamViewerCount(int(stream, "viewersCount"), int(stream, "collaborationViewersCount"))
    }

    private fun collaborationCount(user: JSONObject, channelLogin: String): Int? {
        val collaborators = user.optJSONObject("channel")
            ?.optJSONObject("collaboration")
            ?.optJSONArray("collaborators")
            ?: return null
        return collaborationOthersCount(activeCollaboratorLogins(collaborators), channelLogin)
    }

    private fun collaboratorAvatarUrls(user: JSONObject, channelLogin: String): List<String> {
        val collaborators = user.optJSONObject("channel")
            ?.optJSONObject("collaboration")
            ?.optJSONArray("collaborators")
            ?: return emptyList()
        return activeCollaboratorAvatarUrls(collaborators, channelLogin)
    }

    private fun customRewards(points: JSONObject?): Map<String, ChatReward> {
        val rewards = points?.optJSONArray("customRewards") ?: return emptyMap()
        return buildMap {
            for (index in 0 until rewards.length()) {
                val reward = rewards.optJSONObject(index) ?: continue
                val id = text(reward, "id") ?: continue
                val title = text(reward, "title") ?: continue
                put(
                    id,
                    ChatReward(
                        id = id,
                        title = title,
                        cost = int(reward, "cost") ?: 0,
                        backgroundColorHex = text(reward, "backgroundColor"),
                        imageUrl = higherResPointsIcon(
                            text(reward.optJSONObject("image"), "url")
                                ?: text(reward.optJSONObject("defaultImage"), "url"),
                        ),
                        prompt = text(reward, "prompt"),
                    ),
                )
            }
        }
    }

    private fun highlightReward(points: JSONObject?): HighlightReward {
        val rewards = points?.optJSONArray("automaticRewards")
        if (rewards != null) {
            for (index in 0 until rewards.length()) {
                val reward = rewards.optJSONObject(index) ?: continue
                if (reward.optString("type") != "SEND_HIGHLIGHTED_MESSAGE") continue
                return HighlightReward(
                    backgroundColor = text(reward, "backgroundColor"),
                    cost = int(reward, "cost")
                        ?: int(reward, "defaultCost")
                        ?: ChannelProfile.DEFAULT_HIGHLIGHT_COST,
                )
            }
        }
        return HighlightReward(null, ChannelProfile.DEFAULT_HIGHLIGHT_COST)
    }

    private fun text(obj: JSONObject?, key: String): String? {
        return obj?.optString(key)?.takeIf { it.isNotBlank() && it != "null" }
    }

    private fun gameName(obj: JSONObject?): String? = text(obj?.optJSONObject("game"), "name")

    private fun streamTags(stream: JSONObject?): List<String> {
        val tags = stream?.optJSONArray("freeformTags") ?: return emptyList()
        return buildList {
            for (index in 0 until tags.length()) {
                val name = text(tags.optJSONObject(index), "name") ?: continue
                add(name)
            }
        }
    }

    private fun int(obj: JSONObject?, key: String): Int? {
        if (obj == null || !obj.has(key) || obj.isNull(key)) return null
        return obj.optInt(key)
    }

    private fun higherResPointsIcon(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return url.replace(Regex("""-1(\.png)$"""), "-2$1")
    }

    private data class HighlightReward(val backgroundColor: String?, val cost: Int)
}

internal fun sharedStreamViewerCount(own: Int?, shared: Int?): Int? {
    return shared?.takeIf { own != null && it > own }
}

internal fun activeCollaboratorLogins(collaborators: JSONArray): List<String> {
    return buildList {
        for (index in 0 until collaborators.length()) {
            val item = collaborators.optJSONObject(index) ?: continue
            val status = item.optString("status")
            if (status.isNotBlank() && !status.equals("ACTIVE", ignoreCase = true)) continue
            val login = item.optJSONObject("user")?.optString("login")
                ?.takeIf { it.isNotBlank() && it != "null" }
                ?: continue
            add(login)
        }
    }
}

/** Avatars of the active collaborators other than [channelLogin], skipping those without one. */
internal fun activeCollaboratorAvatarUrls(collaborators: JSONArray, channelLogin: String): List<String> {
    return buildList {
        for (index in 0 until collaborators.length()) {
            val item = collaborators.optJSONObject(index) ?: continue
            val status = item.optString("status")
            if (status.isNotBlank() && !status.equals("ACTIVE", ignoreCase = true)) continue
            val user = item.optJSONObject("user") ?: continue
            if (user.optString("login").equals(channelLogin, ignoreCase = true)) continue
            val avatar = user.optString("profileImageURL").takeIf { it.isNotBlank() && it != "null" } ?: continue
            add(avatar)
        }
    }
}

internal data class ParsedChannelProfile(
    val profile: ChannelProfile,
    val customRewards: Map<String, ChatReward>,
)
