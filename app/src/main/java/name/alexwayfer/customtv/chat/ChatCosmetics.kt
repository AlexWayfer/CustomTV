package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import name.alexwayfer.customtv.data.AppSettings
import name.alexwayfer.customtv.data.CustomTvBadgeEdition
import name.alexwayfer.customtv.data.CustomTvBadgeList
import name.alexwayfer.customtv.data.FfzBadgeList

/** A badge a third-party service gives a chatter, shown after Twitch's own. */
internal data class ThirdPartyBadge(
    val url: String,
    val title: String,
    /** The color FFZ draws its transparent badge on; unspecified for a badge drawn as it is. */
    val background: Color = Color.Unspecified,
    /** The Twitch badge set this one takes the place of, such as FFZ's bot badge over the moderator one. */
    val replaces: String? = null,
)

/** Which third-party badges and paints the settings turn on. */
internal data class CosmeticSources(
    val sevenTvBadges: Boolean,
    val sevenTvPaints: Boolean,
    val ffzBadges: Boolean,
    val bttvBadges: Boolean,
    val customTvBadges: Boolean,
)

internal fun cosmeticSources(settings: AppSettings): CosmeticSources = CosmeticSources(
    sevenTvBadges = settings.sevenTvBadges,
    sevenTvPaints = settings.sevenTvPaints,
    ffzBadges = settings.ffzBadges,
    bttvBadges = settings.bttvBadges,
    customTvBadges = settings.customTvBadges,
)

private val NoSources = CosmeticSources(
    sevenTvBadges = false,
    sevenTvPaints = false,
    ffzBadges = false,
    bttvBadges = false,
    customTvBadges = false,
)

/** What a chatter's name shows beyond Twitch: badges after Twitch's own, and a 7TV paint over the name. */
internal data class ChatterCosmetics(
    val badges: List<ThirdPartyBadge> = emptyList(),
    val paint: NamePaint? = null,
)

/** The third-party badges and paints of the open chat, of the services the settings turn on. */
internal data class ChatCosmetics(
    val sources: CosmeticSources = NoSources,
    val sevenTv: SevenTvCosmetics = SevenTvCosmetics(),
    val ffz: FfzBadgeList = FfzBadgeList(),
    /** BTTV's badges by Twitch user ID. */
    val bttv: Map<String, ThirdPartyBadge> = emptyMap(),
    /** The Twitch user IDs of the chatters seen, by lowercase login and display name. */
    val chatterIds: Map<String, String> = emptyMap(),
    val customTv: CustomTvBadgeList = CustomTvBadgeList(),
    /** The picture and name of each CustomTV badge. */
    val customTvPictures: Map<CustomTvBadgeEdition, ThirdPartyBadge> = emptyMap(),
) {
    /** The 7TV paint of the chatter a message names by [nick], a lowercase login; null for one not seen in chat. */
    fun paintForNick(nick: String): NamePaint? =
        if (sources.sevenTvPaints) chatterIds[nick]?.let(sevenTv::paintFor) else null

    /** The CustomTV badge [userId] shows; null for none, or while the switch is off. */
    fun customTvEdition(userId: String?): CustomTvBadgeEdition? =
        if (sources.customTvBadges) customTv.editionOf(userId) else null

    /** The badges of [userId] by 7TV, FFZ, BTTV, and CustomTV in that order, and their 7TV paint. */
    fun forUser(userId: String?): ChatterCosmetics {
        if (userId.isNullOrBlank()) return NoCosmetics
        val badges = buildList {
            if (sources.sevenTvBadges) sevenTv.badgeFor(userId)?.let(::add)
            if (sources.ffzBadges) addAll(ffz.forUser(userId))
            if (sources.bttvBadges) bttv[userId]?.let(::add)
            customTvEdition(userId)?.let(customTvPictures::get)?.let(::add)
        }
        val paint = if (sources.sevenTvPaints) sevenTv.paintFor(userId) else null
        return if (badges.isEmpty() && paint == null) NoCosmetics else ChatterCosmetics(badges, paint)
    }
}

private val NoCosmetics = ChatterCosmetics()

/** A badge in front of a chatter's name, with what TalkBack reads for it. */
internal data class ShownBadge(
    val url: String,
    val description: String,
    val background: Color = Color.Unspecified,
)

/**
 * The badges in front of a chatter's name: Twitch's own, each with a third-party badge in its place when one replaces
 * it, then the other third-party badges. A Twitch badge without a picture is left out.
 */
internal fun shownBadges(
    twitch: List<ChatBadge>,
    badgeUrls: Map<String, String>,
    extra: List<ThirdPartyBadge>,
): List<ShownBadge> {
    val replacing = extra.filter { it.replaces != null }.associateBy { it.replaces }
    val used = mutableSetOf<ThirdPartyBadge>()
    val shown = mutableListOf<ShownBadge>()
    twitch.forEach { badge ->
        val replacement = replacing[badge.setId]
        if (replacement != null) {
            used += replacement
            shown += replacement.shown()
        } else {
            badgeUrls[badge.key]?.let { url -> shown += ShownBadge(url, badge.setId) }
        }
    }
    extra.forEach { badge -> if (badge !in used) shown += badge.shown() }
    return shown
}

private fun ThirdPartyBadge.shown(): ShownBadge = ShownBadge(url, title, background)
