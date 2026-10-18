package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.chat.ChatReward
import org.json.JSONObject

internal object ChannelProfileParser {
    fun parse(body: String, requestedLogin: String): ParsedChannelProfile? {
        val user = body.toJsonObjectOrNull()?.optJSONObject("data")?.optJSONObject("user") ?: return null
        return parseUser(user, requestedLogin)
    }

    fun parseUser(user: JSONObject, requestedLogin: String): ParsedChannelProfile {
        val login = user.text("login") ?: requestedLogin
        val displayName = user.text("displayName") ?: login
        val stream = user.optJSONObject("stream")
        val broadcastSettings = user.optJSONObject("broadcastSettings")
        val points = user.optJSONObject("channel")?.optJSONObject("communityPointsSettings")
        val highlight = highlightReward(points)
        val game = streamGame(stream) ?: streamGame(broadcastSettings)
        val profile = ChannelProfile(
            login = login.lowercase(),
            displayName = displayName,
            avatarUrl = user.text("profileImageURL"),
            id = user.text("id"),
            streamTitle = stream?.text("title") ?: broadcastSettings?.text("title"),
            categoryName = game?.text("name"),
            categoryId = game?.text("id"),
            categoryBoxArtUrl = game?.text("boxArtURL"),
            streamStartedAtMillis = stream?.text("createdAt")
                ?.let(::instantMillisOrNull),
            currentStreamVideoId = stream?.optJSONObject("archiveVideo")?.text("id"),
            viewerCount = stream?.intOrNull("viewersCount"),
            sharedViewerCount = sharedViewerCount(stream),
            collaborationCount = collaborationCount(user, login),
            collaboratorAvatarUrls = collaboratorAvatarUrls(user, login),
            isLive = stream != null,
            highlightColorHex = highlight.backgroundColor ?: user.text("primaryColorHex"),
            primaryColorHex = user.text("primaryColorHex"),
            highlightRewardCost = highlight.cost,
            tags = streamTags(stream),
            chatRules = parseChatRules(user),
            channelPointsIconUrl = higherResPointsIcon(
                points?.optJSONObject("image")?.text("url")
                    ?: points?.optJSONObject("defaultImage")?.text("url"),
            ),
        )
        return ParsedChannelProfile(profile, customRewards(points))
    }

    private fun sharedViewerCount(stream: JSONObject?): Int? {
        return sharedStreamViewerCount(
            stream?.intOrNull("viewersCount"),
            stream?.intOrNull("collaborationViewersCount"),
        )
    }

    private fun customRewards(points: JSONObject?): Map<String, ChatReward> {
        val rewards = points?.optJSONArray("customRewards") ?: return emptyMap()
        return rewards.objects().mapNotNull(::customReward).associateBy { it.id }
    }

    private fun customReward(reward: JSONObject): ChatReward? {
        val id = reward.text("id") ?: return null
        val title = reward.text("title") ?: return null
        return ChatReward(
            id = id,
            title = title,
            cost = reward.intOrNull("cost") ?: 0,
            backgroundColorHex = reward.text("backgroundColor"),
            imageUrl = higherResPointsIcon(
                reward.optJSONObject("image")?.text("url")
                    ?: reward.optJSONObject("defaultImage")?.text("url"),
            ),
            prompt = reward.text("prompt"),
        )
    }

    private fun highlightReward(points: JSONObject?): HighlightReward {
        val reward = points?.optJSONArray("automaticRewards")?.objects()
            ?.firstOrNull { it.optString("type") == "SEND_HIGHLIGHTED_MESSAGE" }
            ?: return HighlightReward(null, ChannelProfile.DEFAULT_HIGHLIGHT_COST)
        return HighlightReward(
            backgroundColor = reward.text("backgroundColor"),
            cost = reward.intOrNull("cost")
                ?: reward.intOrNull("defaultCost")
                ?: ChannelProfile.DEFAULT_HIGHLIGHT_COST,
        )
    }

    /** The named category of a stream or of the channel's settings; its ID and box art come from the same one. */
    private fun streamGame(obj: JSONObject?): JSONObject? =
        obj?.optJSONObject("game")?.takeIf { it.text("name") != null }

    private fun streamTags(stream: JSONObject?): List<String> {
        val tags = stream?.optJSONArray("freeformTags") ?: return emptyList()
        return buildList {
            for (index in 0 until tags.length()) {
                val name = tags.optJSONObject(index)?.text("name") ?: continue
                add(name)
            }
        }
    }

    private data class HighlightReward(val backgroundColor: String?, val cost: Int)
}

private fun higherResPointsIcon(url: String?): String? {
    if (url.isNullOrBlank()) return null
    return url.replace(Regex("""-1(\.png)$"""), "-2$1")
}

internal data class ParsedChannelProfile(
    val profile: ChannelProfile,
    val customRewards: Map<String, ChatReward>,
)
