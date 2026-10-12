package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.instantMillisOrNull
import name.alexwayfer.customtv.data.intOrNull
import name.alexwayfer.customtv.data.text
import name.alexwayfer.customtv.data.toJsonObjectOrNull
import org.json.JSONObject

object ChannelPointsPubSubParser {
    fun parseFrame(raw: String): ChatMessage? {
        val frame = raw.toJsonObjectOrNull() ?: return null
        if (frame.optString("type") != "MESSAGE") return null
        val inner = frame.optJSONObject("data")?.text("message") ?: return null
        return parseRedemptionMessage(inner)
    }

    fun parseRedemptionMessage(raw: String): ChatMessage? {
        val payload = raw.toJsonObjectOrNull() ?: return null
        if (payload.optString("type") != "reward-redeemed") return null
        val redemption = payload.optJSONObject("data")?.optJSONObject("redemption")
            ?: return null
        val user = redemption.optJSONObject("user") ?: return null
        val reward = redemption.optJSONObject("reward") ?: return null
        val login = user.text("login")?.lowercase().orEmpty()
        val displayName = user.text("display_name") ?: login
        if (displayName.isBlank()) return null
        val title = reward.text("title") ?: return null
        val rewardId = reward.text("id") ?: redemption.text("id") ?: return null
        val userInput = redemption.text("user_input") ?: redemption.text("userInput")
        val timestampMillis = timestampMillis(payload, redemption)
        val id = redemption.text("id")?.let { "reward-$it" }
            ?: "reward-$rewardId-$timestampMillis"
        return ChatMessage(
            id = id,
            userId = user.text("id"),
            userLogin = login,
            displayName = displayName,
            color = IrcMessageParser.nameColor(displayName.ifEmpty { login }),
            rawText = userInput.orEmpty(),
            parts = listOfNotNull(userInput).map { ChatPart.Text(it) },
            timestampMillis = timestampMillis,
            eventKind = ChatEventKind.Reward,
            reward = chatReward(reward, rewardId, title),
        )
    }

    private fun chatReward(reward: JSONObject, rewardId: String, title: String): ChatReward = ChatReward(
        id = rewardId,
        title = title,
        cost = reward.intOrNull("cost") ?: 0,
        backgroundColorHex = reward.text("background_color")
            ?: reward.text("backgroundColor"),
        imageUrl = imageUrl(reward),
        prompt = reward.text("prompt"),
    )

    private fun imageUrl(reward: JSONObject): String? {
        val custom = reward.optJSONObject("image")
        val fallback = reward.optJSONObject("default_image")
            ?: reward.optJSONObject("defaultImage")
        return custom?.text("url_2x")
            ?: custom?.text("url")
            ?: fallback?.text("url_2x")
            ?: fallback?.text("url")
    }

    private fun timestampMillis(payload: JSONObject, redemption: JSONObject): Long {
        val raw = payload.optJSONObject("data")?.text("timestamp")
            ?: redemption.text("redeemed_at")
        return raw
            ?.let(::instantMillisOrNull)
            ?: System.currentTimeMillis()
    }
}
