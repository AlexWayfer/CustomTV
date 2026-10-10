package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Immutable
import name.alexwayfer.customtv.chat.ChatPart
import name.alexwayfer.customtv.chat.EmotePlatform
import name.alexwayfer.customtv.chat.EmoteSources
import name.alexwayfer.customtv.chat.SevenTvEmote

/** How every chat surface draws a message: the list, notices, the pinned banner, and reply threads. */
@Immutable
internal data class ChatAppearance(
    val readableColors: Boolean,
    val textSize: Int,
    val meMessageItalic: Boolean,
    val badgeUrls: Map<String, String>,
    /** Emotes of the enabled sources only. 7TV wins over FFZ, and FFZ over BTTV. */
    val thirdPartyEmotes: Map<String, SevenTvEmote>,
    /** Platforms whose modifier words apply, such as BTTV `w!` or FFZ `ffzCursed`. */
    val modifierPlatforms: Set<EmotePlatform>,
    /** Each platform's logo emote for emote change lines. */
    val platformLogos: Map<EmotePlatform, ChatPart.Emote>,
)

internal fun chatAppearance(
    readableColors: Boolean,
    textSize: Int,
    meMessageItalic: Boolean,
    badgeUrls: Map<String, String>,
    sources: EmoteSources,
    sevenTvEmotes: Map<String, SevenTvEmote>,
    ffzEmotes: Map<String, SevenTvEmote>,
    bttvEmotes: Map<String, SevenTvEmote>,
): ChatAppearance = ChatAppearance(
    readableColors = readableColors,
    textSize = textSize,
    meMessageItalic = meMessageItalic,
    badgeUrls = badgeUrls,
    thirdPartyEmotes = buildMap {
        if (sources.bttv) putAll(bttvEmotes)
        if (sources.ffz) putAll(ffzEmotes)
        if (sources.sevenTv) putAll(sevenTvEmotes)
    },
    modifierPlatforms = buildSet {
        if (sources.bttv) add(EmotePlatform.Bttv)
        if (sources.ffz) add(EmotePlatform.Ffz)
    },
    platformLogos = buildMap {
        platformLogoEmote(sevenTvEmotes, listOf("(7TV)", "sevenTV", "7tv"))?.let { put(EmotePlatform.SevenTv, it) }
        platformLogoEmote(bttvEmotes, listOf("bttv", "BTTV"))?.let { put(EmotePlatform.Bttv, it) }
        platformLogoEmote(ffzEmotes, listOf("ffz", "FFZ"))?.let { put(EmotePlatform.Ffz, it) }
    },
)

private fun platformLogoEmote(emotes: Map<String, SevenTvEmote>, names: List<String>): ChatPart.Emote? {
    val found = names.firstNotNullOfOrNull { name ->
        emotes.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }
    } ?: return null
    return ChatPart.Emote(found.key, found.value.url, found.value.aspectRatio)
}
