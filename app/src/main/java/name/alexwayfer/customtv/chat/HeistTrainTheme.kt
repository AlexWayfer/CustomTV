package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.objects
import name.alexwayfer.customtv.data.trimmedText
import org.json.JSONObject

internal data class HypeTrainTheme(val key: String) {
    fun shared(): HypeTrainTheme = when {
        this == Regular || key.startsWith("shared:") -> this
        else -> HypeTrainTheme("shared:$key")
    }

    companion object {
        val Regular = HypeTrainTheme("regular")
        val Heist = HypeTrainTheme("gta-vi")
        val SharedHeist = Heist.shared()
    }
}

internal data class HeistTrainFlags(val enabled: Boolean, val skipCategory: Boolean)

private const val GTA_VI_CATEGORY = "1867279146"
private val wordBoundary = Regex("(?<=[a-z0-9])(?=[A-Z])")
private val typesWithSeparateDetails = setOf("regular", "treasure", "golden-kappa")

/** Twitch checks the watching channel and every participant in the execution's shared progress. */
internal fun hypeTrainTheme(
    user: JSONObject,
    phase: HypeTrainPhase,
    sharedChat: JSONObject? = null,
    flags: () -> HeistTrainFlags?,
): HypeTrainTheme? {
    val approaching = phase == HypeTrainPhase.Approaching
    val train = user.optJSONObject("channel")?.optJSONObject("hypeTrain")
        ?.optJSONObject(if (approaching) "approaching" else "execution") ?: return null
    val shared = train.optJSONObject("sharedHypeTrainDetails")
    val isShared = shared != null || approaching && sharedChat.isActiveParticipant(user.optString("id"))
    val theme = train.specialTheme() ?: heistCampaignTheme(user, shared, flags())
    return if (isShared) theme?.shared() else theme
}

private fun heistCampaignTheme(user: JSONObject, shared: JSONObject?, campaign: HeistTrainFlags?): HypeTrainTheme? {
    if (campaign == null) return null
    val gtaCategory = user.streamsGtaVi() || shared?.optJSONArray("sharedProgress")?.objects()
        .orEmpty().any { it.optJSONObject("user")?.streamsGtaVi() == true }
    val enabled = campaign.enabled && (campaign.skipCategory || gtaCategory)
    return if (enabled) HypeTrainTheme.Heist else HypeTrainTheme.Regular
}

private fun JSONObject.streamsGtaVi(): Boolean =
    optJSONObject("stream")?.optJSONObject("game")?.optString("id") == GTA_VI_CATEGORY

private fun JSONObject.specialTheme(): HypeTrainTheme? {
    val details = optJSONObject("variantTrainDetails")?.optString("__typename").orEmpty()
    val type = trimmedText("hypeTrainType")?.lowercase()?.replace('_', '-')
    val key = when {
        optBoolean("isGoldenKappaTrain") -> "golden-kappa"
        optBoolean("isTreasureTrain") && optJSONObject("treasureTrainDetails") != null -> "treasure"
        details.endsWith("TrainDetails") -> details.removeSuffix("TrainDetails").replace(wordBoundary, "-").lowercase()
        type != null && type !in typesWithSeparateDetails -> type
        else -> return null
    }
    return if (key == "pinata") HypeTrainTheme.Regular else HypeTrainTheme(key)
}

private fun JSONObject?.isActiveParticipant(channelId: String): Boolean =
    this?.optString("status") == "ACTIVE" && optJSONArray("participants")?.objects().orEmpty().any {
        it.optJSONObject("channel")?.optString("id") == channelId && it.optString("status") == "ACTIVE"
    }
