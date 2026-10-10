package name.alexwayfer.customtv.chat

import org.json.JSONObject
import java.time.Instant

object ChannelPointsPubSubParser {
    fun parseFrame(raw: String): ChatMessage? {
        val frame = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        if (frame.optString("type") != "MESSAGE") return null
        val inner = frame.optJSONObject("data")?.optString("message")
            ?.takeIf { it.isNotBlank() && it != "null" }
            ?: return null
        return parseRedemptionMessage(inner)
    }

    fun parseRedemptionMessage(raw: String): ChatMessage? {
        val payload = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        if (payload.optString("type") != "reward-redeemed") return null
        val redemption = payload.optJSONObject("data")?.optJSONObject("redemption")
            ?: return null
        val user = redemption.optJSONObject("user") ?: return null
        val reward = redemption.optJSONObject("reward") ?: return null
        val login = text(user, "login")?.lowercase().orEmpty()
        val displayName = text(user, "display_name") ?: login
        if (displayName.isBlank()) return null
        val title = text(reward, "title") ?: return null
        val rewardId = text(reward, "id") ?: text(redemption, "id") ?: return null
        val userInput = text(redemption, "user_input") ?: text(redemption, "userInput")
        val timestampMillis = timestampMillis(payload, redemption)
        val id = text(redemption, "id")?.let { "reward-$it" }
            ?: "reward-$rewardId-$timestampMillis"
        return ChatMessage(
            id = id,
            userId = text(user, "id"),
            userLogin = login,
            displayName = displayName,
            color = IrcMessageParser.nameColor(displayName.ifEmpty { login }),
            rawText = userInput.orEmpty(),
            parts = if (userInput != null) listOf(ChatPart.Text(userInput)) else emptyList(),
            timestampMillis = timestampMillis,
            eventKind = ChatEventKind.Reward,
            reward = ChatReward(
                id = rewardId,
                title = title,
                cost = if (reward.has("cost") && !reward.isNull("cost")) reward.optInt("cost") else 0,
                backgroundColorHex = text(reward, "background_color")
                    ?: text(reward, "backgroundColor"),
                imageUrl = imageUrl(reward),
                prompt = text(reward, "prompt"),
            ),
        )
    }

    private fun imageUrl(reward: JSONObject): String? {
        val custom = reward.optJSONObject("image")
        val fallback = reward.optJSONObject("default_image")
            ?: reward.optJSONObject("defaultImage")
        return text(custom, "url_2x")
            ?: text(custom, "url")
            ?: text(fallback, "url_2x")
            ?: text(fallback, "url")
    }

    private fun timestampMillis(payload: JSONObject, redemption: JSONObject): Long {
        val raw = text(payload.optJSONObject("data"), "timestamp")
            ?: text(redemption, "redeemed_at")
        return raw
            ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
            ?: System.currentTimeMillis()
    }

    private fun text(obj: JSONObject?, key: String): String? {
        return obj?.optString(key)?.takeIf { it.isNotBlank() && it != "null" }
    }
}
