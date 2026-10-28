package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import name.alexwayfer.customtv.data.AppSettings
import name.alexwayfer.customtv.data.CustomTvBadgeEdition
import name.alexwayfer.customtv.data.FfzBadgeList
import name.alexwayfer.customtv.data.parseCustomTvBadges
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatCosmeticsTest {
    private val sevenTvBadge = ThirdPartyBadge(url = "https://7tv/badge", title = "7TV Subscriber")
    private val ffzBadge = ThirdPartyBadge(url = "https://ffz/badge", title = "FFZ Supporter", background = Color.Blue)
    private val bttvBadge = ThirdPartyBadge(url = "https://bttv/badge.svg", title = "BetterTTV Translator")
    private val paint = NamePaint(function = PaintFunction.Image, imageUrl = "https://paint")

    private val cosmetics = ChatCosmetics(
        sources = cosmeticSources(AppSettings()),
        sevenTv = SevenTvCosmetics()
            .apply(SevenTvCosmeticEvent.PaintDefined("p", paint))
            .apply(SevenTvCosmeticEvent.BadgeDefined("b", sevenTvBadge))
            .apply(SevenTvCosmeticEvent.Assigned(SevenTvCosmeticKind.Paint, "p", listOf("42")))
            .apply(SevenTvCosmeticEvent.Assigned(SevenTvCosmeticKind.Badge, "b", listOf("42"))),
        ffz = FfzBadgeList(listOf(ffzBadge to longArrayOf(7, 42, 100))),
        bttv = mapOf("42" to bttvBadge),
    )

    @Test
    fun chatterHasTheBadgesOf7tvFfzAndBttvInThatOrderAndThePaint() {
        val chatter = cosmetics.forUser("42")

        assertEquals(listOf(sevenTvBadge, ffzBadge, bttvBadge), chatter.badges)
        assertEquals(paint, chatter.paint)
    }

    @Test
    fun switchesTurnOffEachPart() {
        val chatter = cosmetics.copy(
            sources = cosmeticSources(AppSettings(sevenTvBadges = false, sevenTvPaints = false, bttvBadges = false)),
        ).forUser("42")

        assertEquals(listOf(ffzBadge), chatter.badges)
        assertNull(chatter.paint)
    }

    @Test
    fun customTvBadgeComesAfterTheOthersInItsEdition() {
        val premium = ThirdPartyBadge(url = "android.resource://app/1", title = "CustomTV Premium")
        val free = ThirdPartyBadge(url = "android.resource://app/2", title = "CustomTV")
        val withCustomTv = cosmetics.copy(
            customTv = parseCustomTvBadges("""{"premium":[42],"free":[8]}""")!!,
            customTvPictures = mapOf(CustomTvBadgeEdition.Premium to premium, CustomTvBadgeEdition.Free to free),
        )

        assertEquals(listOf(sevenTvBadge, ffzBadge, bttvBadge, premium), withCustomTv.forUser("42").badges)
        assertEquals(listOf(free), withCustomTv.forUser("8").badges)
        assertEquals(CustomTvBadgeEdition.Free, withCustomTv.customTvEdition("8"))

        val off = withCustomTv.copy(sources = cosmeticSources(AppSettings(customTvBadges = false)))
        assertEquals(listOf(sevenTvBadge, ffzBadge, bttvBadge), off.forUser("42").badges)
        assertNull(off.customTvEdition("8"))
    }

    @Test
    fun mentionedNickHasThePaintOfTheChatterWithThatLogin() {
        val known = cosmetics.copy(chatterIds = mapOf("viewer" to "42", "quiet" to "8"))

        assertEquals(paint, known.paintForNick("viewer"))
        assertNull(known.paintForNick("quiet"))
        assertNull(known.paintForNick("stranger"))
        assertNull(
            known.copy(sources = cosmeticSources(AppSettings(sevenTvPaints = false))).paintForNick("viewer"),
        )
    }

    @Test
    fun chatterWithoutAnyHasNone() {
        assertEquals(ChatterCosmetics(), cosmetics.forUser("8"))
        assertEquals(ChatterCosmetics(), cosmetics.forUser(null))
        assertEquals(ChatterCosmetics(), cosmetics.forUser("not-a-number"))
    }

    @Test
    fun ffzListFindsEveryBadgeOfAChatter() {
        val bot = ThirdPartyBadge(url = "https://ffz/bot", title = "Bot", replaces = "moderator")
        val list = FfzBadgeList(listOf(bot to longArrayOf(5), ffzBadge to longArrayOf(1, 5, 9)))

        assertEquals(listOf(bot, ffzBadge), list.forUser("5"))
        assertEquals(listOf(ffzBadge), list.forUser("9"))
        assertEquals(emptyList<ThirdPartyBadge>(), list.forUser("6"))
    }

    @Test
    fun thirdPartyBadgesFollowTwitchOnes() {
        val shown = shownBadges(
            twitch = listOf(ChatBadge("subscriber", "12"), ChatBadge("unknown", "1")),
            badgeUrls = mapOf("subscriber/12" to "https://twitch/sub"),
            extra = listOf(sevenTvBadge, ffzBadge),
        )

        assertEquals(
            listOf(
                ShownBadge(url = "https://twitch/sub", description = "subscriber"),
                ShownBadge(url = "https://7tv/badge", description = "7TV Subscriber"),
                ShownBadge(url = "https://ffz/badge", description = "FFZ Supporter", background = Color.Blue),
            ),
            shown,
        )
    }

    @Test
    fun badgeThatReplacesATwitchOneTakesItsPlace() {
        val bot = ThirdPartyBadge(
            url = "https://ffz/bot",
            title = "Bot",
            background = Color.Gray,
            replaces = "moderator",
        )
        val shown = shownBadges(
            twitch = listOf(ChatBadge("moderator", "1"), ChatBadge("subscriber", "12")),
            badgeUrls = mapOf("moderator/1" to "https://twitch/mod", "subscriber/12" to "https://twitch/sub"),
            extra = listOf(bot),
        )

        assertEquals(
            listOf(
                ShownBadge(url = "https://ffz/bot", description = "Bot", background = Color.Gray),
                ShownBadge(url = "https://twitch/sub", description = "subscriber"),
            ),
            shown,
        )
    }

    @Test
    fun badgeThatReplacesAMissingTwitchOneFollowsTheOthers() {
        val bot = ThirdPartyBadge(url = "https://ffz/bot", title = "Bot", replaces = "moderator")
        val shown = shownBadges(
            twitch = listOf(ChatBadge("subscriber", "12")),
            badgeUrls = mapOf("subscriber/12" to "https://twitch/sub"),
            extra = listOf(bot),
        )

        assertEquals(listOf("https://twitch/sub", "https://ffz/bot"), shown.map { it.url })
    }
}
