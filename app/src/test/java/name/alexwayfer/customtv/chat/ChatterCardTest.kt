package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.auth.TwitchProfileLink
import name.alexwayfer.customtv.data.parseProfileDetails
import name.alexwayfer.customtv.data.toChatterProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatterCardTest {
    @Test
    fun profileReadsAvatarAndAccountCreated() {
        val profile = parseProfileDetails(
            """{"data":{"user":{"login":"heatlee","displayName":"HeatLee","profileImageURL":"https://example/avatar.png","createdAt":"2015-07-21T00:00:00Z"}}}""",
            fallbackLogin = "heatlee",
        )?.toChatterProfile()

        assertEquals("heatlee", profile?.login)
        assertEquals("HeatLee", profile?.displayName)
        assertEquals("https://example/avatar.png", profile?.avatarUrl)
        assertNull(profile?.bannerImageUrl)
        assertEquals(1437436800000L, profile?.createdAtMillis)
    }

    @Test
    fun headerUsesTheProfileBannerAndIgnoresTheOfflineImage() {
        val profile = parseProfileDetails(
            """{"data":{"user":{"login":"heatlee","displayName":"HeatLee","offlineImageURL":"https://example/offline.png","bannerImageURL":"https://example/banner.png","createdAt":"2015-07-21T00:00:00Z"}}}""",
            fallbackLogin = "heatlee",
        )?.toChatterProfile()

        assertEquals("https://example/banner.png", profile?.bannerImageUrl)
    }

    @Test
    fun profileReadsUserIdAboutAndSocials() {
        val profile = parseProfileDetails(
            """{"data":{"user":{"id":"123","login":"heatlee","description":"  Hi there  ","channel":{"socialMedias":[{"name":"twitter","title":"Twitter","url":"https://twitter.com/heatlee"},{"name":"website","title":"Broken","url":"ftp://example"}]}}}}""",
            fallbackLogin = "heatlee",
        )?.toChatterProfile()

        assertEquals("123", profile?.userId)
        assertEquals("Hi there", profile?.about)
        assertEquals(
            listOf(TwitchProfileLink(title = "Twitter", url = "https://twitter.com/heatlee", name = "twitter")),
            profile?.links,
        )
    }

    @Test
    fun blankAboutAndMissingChannelLeaveBothSectionsEmpty() {
        val profile = parseProfileDetails(
            """{"data":{"user":{"login":"heatlee","description":"","channel":null}}}""",
            fallbackLogin = "heatlee",
        )?.toChatterProfile()

        assertNull(profile?.about)
        assertEquals(emptyList<TwitchProfileLink>(), profile?.links)
    }

    @Test
    fun signedInViewerCanWhisperAnotherChatter() {
        assertTrue(chatterCardOffersWhisper("2", "bob", selfUserId = "1", selfLogin = "alice"))
    }

    @Test
    fun ownCardHasNoWhisperButton() {
        assertFalse(chatterCardOffersWhisper("1", "alice", selfUserId = "1", selfLogin = "alice"))
        assertFalse(chatterCardOffersWhisper(null, "Alice", selfUserId = "1", selfLogin = "alice"))
    }

    @Test
    fun signedOutViewerHasNoWhisperButton() {
        assertFalse(chatterCardOffersWhisper("2", "bob", selfUserId = null, selfLogin = ""))
    }

    @Test
    fun recordingChatWithoutOwnUserIdStillOffersWhisper() {
        assertTrue(chatterCardOffersWhisper("2", "bob", selfUserId = null, selfLogin = "alice"))
    }

    @Test
    fun missingBannerLeavesTheHeaderEmpty() {
        val profile = parseProfileDetails(
            """{"data":{"user":{"login":"iip0ctak","displayName":"iip0ctak","bannerImageURL":null}}}""",
            fallbackLogin = "iip0ctak",
        )?.toChatterProfile()

        assertNull(profile?.bannerImageUrl)
    }

    @Test
    fun badgeLinesUseTheTwitchTitleAndSkipABadgeWithoutAnImage() {
        val lines = chatterBadgeLines(
            badges = listOf(
                ChatBadge("subscriber", "13"),
                ChatBadge("moderator", "1"),
                ChatBadge("vip", "1"),
            ),
            imageUrls = mapOf(
                "subscriber/13" to "https://example/sub.png",
                "vip/1" to "https://example/vip.png",
            ),
            titles = mapOf("subscriber/13" to "1-Year Subscriber"),
        )

        assertEquals(
            listOf(
                ChatterBadgeLine("https://example/sub.png", "1-Year Subscriber"),
                ChatterBadgeLine("https://example/vip.png", "VIP"),
            ),
            lines,
        )
    }

    @Test
    fun missingUserLeavesTheProfileEmpty() {
        assertNull(parseProfileDetails("""{"data":{"user":null}}""", "heatlee")?.toChatterProfile())
    }

    @Test
    fun ownFollowShowsOnlyForTheSignedInUser() {
        assertTrue(chatterCardShowsOwnFollow("7", "alex", "7", "alex"))
        assertFalse(chatterCardShowsOwnFollow("8", "alex", "7", "alex"))
        assertTrue(chatterCardShowsOwnFollow(null, "Alex", null, "alex"))
        assertFalse(chatterCardShowsOwnFollow(null, "other", null, "alex"))
        assertFalse(chatterCardShowsOwnFollow("7", "alex", "7", ""))
    }

    @Test
    fun subscriptionComesFromSubscriberAndFounderBadges() {
        assertNull(chatterSubscription(listOf(ChatBadge("moderator", "1"))))
        assertEquals(
            ChatterSubscription(founder = false, months = 13),
            chatterSubscription(listOf(ChatBadge("subscriber", "13"))),
        )
        assertEquals(
            ChatterSubscription(founder = false, months = null),
            chatterSubscription(listOf(ChatBadge("subscriber", "0"))),
        )
        assertEquals(
            ChatterSubscription(founder = true, months = 13),
            chatterSubscription(listOf(ChatBadge("founder", "0"), ChatBadge("subscriber", "13"))),
        )
        assertEquals(
            ChatterSubscription(founder = false, months = null),
            chatterSubscription(listOf(ChatBadge("subscriber", "2012"))),
        )
    }
}
