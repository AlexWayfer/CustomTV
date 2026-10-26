package name.alexwayfer.customtv.data

import androidx.compose.ui.graphics.Color
import name.alexwayfer.customtv.chat.ThirdPartyBadge
import org.junit.Assert.assertEquals
import org.junit.Test

class ThirdPartyBadgeListsTest {
    @Test
    fun ffzBadgesComeAtTwiceTheBaseSizeOnTheirColorInSlotOrder() {
        val list = ThirdPartyBadgeLists.parseFfzBadges(
            """
            {
              "badges": [
                {"id": 3, "title": "FFZ Supporter", "slot": 5, "replaces": null, "color": "#755000",
                 "image": "https://cdn.frankerfacez.com/badge/3/1",
                 "urls": {"1": "https://cdn.frankerfacez.com/badge/3/1", "2": "https://cdn.frankerfacez.com/badge/3/2"}},
                {"id": 2, "title": "Bot", "slot": 1, "replaces": "moderator", "color": "#595959",
                 "image": "https://cdn.frankerfacez.com/badge/2/1", "urls": {}},
                {"id": 9, "title": "Nobody", "slot": 2, "color": "#000000", "image": "https://cdn.frankerfacez.com/badge/9/1"}
              ],
              "users": {"2": [100, 5], "3": [5, 42], "7": [5]}
            }
            """.trimIndent(),
        )

        val bot = ThirdPartyBadge(
            url = "https://cdn.frankerfacez.com/badge/2/1",
            title = "Bot",
            background = Color(red = 0x59, green = 0x59, blue = 0x59),
            replaces = "moderator",
        )
        val supporter = ThirdPartyBadge(
            url = "https://cdn.frankerfacez.com/badge/3/2",
            title = "FFZ Supporter",
            background = Color(red = 0x75, green = 0x50, blue = 0),
        )
        assertEquals(listOf(bot, supporter), list.forUser("5"))
        assertEquals(listOf(bot), list.forUser("100"))
        assertEquals(listOf(supporter), list.forUser("42"))
    }

    @Test
    fun bttvBadgesGoByTheirTwitchUserId() {
        val badges = ThirdPartyBadgeLists.parseBttvBadges(
            """
            [
              {"id": "a", "name": "night", "providerId": "11785491",
               "badge": {"type": 1, "description": "NightDev Developer", "svg": "https://cdn.betterttv.net/badges/developer.svg"}},
              {"id": "b", "name": "nobadge", "providerId": "1"}
            ]
            """.trimIndent(),
        )

        assertEquals(
            mapOf(
                "11785491" to ThirdPartyBadge(
                    url = "https://cdn.betterttv.net/badges/developer.svg",
                    title = "NightDev Developer",
                ),
            ),
            badges,
        )
    }

    @Test
    fun channelShowsFfzModeratorAndVipBadgesOnlyWhileFfzBadgesAreOn() {
        val urls = ChannelBadgeUrls(
            twitch = mapOf("moderator/1" to "https://twitch/mod", "vip/1" to "https://twitch/vip"),
            ffzRoom = mapOf("moderator/1" to "https://ffz/mod"),
        )

        assertEquals(
            mapOf("moderator/1" to "https://ffz/mod", "vip/1" to "https://twitch/vip"),
            urls.shown(ffzBadges = true),
        )
        assertEquals(
            mapOf("moderator/1" to "https://twitch/mod", "vip/1" to "https://twitch/vip"),
            urls.shown(ffzBadges = false),
        )
    }
}
