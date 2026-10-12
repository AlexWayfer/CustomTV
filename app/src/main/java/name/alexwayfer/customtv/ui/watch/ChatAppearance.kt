package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatPart
import name.alexwayfer.customtv.chat.EmotePlatform
import name.alexwayfer.customtv.chat.EmoteSources
import name.alexwayfer.customtv.chat.SevenTvEmote
import name.alexwayfer.customtv.chat.withDisplayParts

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

/** The chat's 7TV, FFZ, and BTTV emotes, and which of these platforms are turned on. */
internal data class ThirdPartyEmotes(
    val sources: EmoteSources,
    val sevenTv: Map<String, SevenTvEmote>,
    val ffz: Map<String, SevenTvEmote>,
    val bttv: Map<String, SevenTvEmote>,
)

internal fun chatAppearance(
    readableColors: Boolean,
    textSize: Int,
    meMessageItalic: Boolean,
    badgeUrls: Map<String, String>,
    emotes: ThirdPartyEmotes,
): ChatAppearance = ChatAppearance(
    readableColors = readableColors,
    textSize = textSize,
    meMessageItalic = meMessageItalic,
    badgeUrls = badgeUrls,
    thirdPartyEmotes = buildMap {
        if (emotes.sources.bttv) putAll(emotes.bttv)
        if (emotes.sources.ffz) putAll(emotes.ffz)
        if (emotes.sources.sevenTv) putAll(emotes.sevenTv)
    },
    modifierPlatforms = buildSet {
        if (emotes.sources.bttv) add(EmotePlatform.Bttv)
        if (emotes.sources.ffz) add(EmotePlatform.Ffz)
    },
    platformLogos = buildMap {
        platformLogoEmote(emotes.sevenTv, listOf("(7TV)", "sevenTV", "7tv"))?.let { put(EmotePlatform.SevenTv, it) }
        platformLogoEmote(emotes.bttv, listOf("bttv", "BTTV"))?.let { put(EmotePlatform.Bttv, it) }
        platformLogoEmote(emotes.ffz, listOf("ffz", "FFZ"))?.let { put(EmotePlatform.Ffz, it) }
    },
)

/** The [message] with the third-party emotes and modifiers of [appearance] put into its text. */
@Composable
internal fun rememberDisplayMessage(message: ChatMessage, appearance: ChatAppearance): ChatMessage =
    remember(message, appearance.thirdPartyEmotes, appearance.modifierPlatforms) {
        message.withDisplayParts(appearance.thirdPartyEmotes, appearance.modifierPlatforms)
    }

private fun platformLogoEmote(emotes: Map<String, SevenTvEmote>, names: List<String>): ChatPart.Emote? {
    val found = names.firstNotNullOfOrNull { name ->
        emotes.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }
    } ?: return null
    return ChatPart.Emote(found.key, found.value.url, found.value.aspectRatio)
}
