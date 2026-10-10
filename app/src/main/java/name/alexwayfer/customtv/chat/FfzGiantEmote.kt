package name.alexwayfer.customtv.chat

/**
 * FrankerFaceZ Giant Emotes: if a custom reward title or prompt contains `FFZ:GE`,
 * the last (or only) emote in the redemption text is hidden inline and shown large below.
 */
object FfzGiantEmote {
    const val TOKEN = "FFZ:GE"

    fun isMarked(value: String?): Boolean = value?.contains(TOKEN) == true

    fun pluckLast(parts: List<ChatPart>): List<ChatPart> {
        val index = parts.indexOfLast { it is ChatPart.Emote }
        if (index < 0) return parts
        val emote = parts[index] as ChatPart.Emote
        return parts.toMutableList().apply {
            this[index] = ChatPart.Gif(
                name = emote.name,
                url = highResolutionUrl(emote.url),
                aspectRatio = emote.aspectRatio,
            )
        }
    }

    internal fun highResolutionUrl(url: String): String {
        return when {
            url.contains("cdn.betterttv.net/emote/", ignoreCase = true) ->
                WEBP_SCALE.replace(url, "/3x.")
            url.contains("7tv.app/", ignoreCase = true) ->
                WEBP_SCALE.replace(url, "/4x.")
            url.contains("static-cdn.jtvnw.net/emoticons/v2/", ignoreCase = true) ->
                TWITCH_SCALE.replace(url, "/4.0")
            url.contains("frankerfacez.com/", ignoreCase = true) ->
                FFZ_SCALE.replace(url) { match -> "/${match.groupValues[1]}4" }
            else -> url
        }
    }

    private val WEBP_SCALE = Regex("/[1-4]x\\.")
    private val TWITCH_SCALE = Regex("/[1-4]\\.0$")
    private val FFZ_SCALE = Regex("/(animated/)?[1-4]$")
}

fun ChatMessage.withDisplayParts(
    thirdPartyEmotes: Map<String, SevenTvEmote>,
    modifierPlatforms: Set<EmotePlatform>,
): ChatMessage {
    val overlaid = ThirdPartyEmoteMatcher.overlay(parts, thirdPartyEmotes, modifierPlatforms)
    val next = if (reward?.giantEmote == true) FfzGiantEmote.pluckLast(overlaid) else overlaid
    return if (next == parts) this else copy(parts = next)
}
