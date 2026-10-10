package name.alexwayfer.customtv.auth

import name.alexwayfer.customtv.data.ProfileDetails
import name.alexwayfer.customtv.data.parseProfileDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountProfileTest {
    @Test
    fun detailsReadBannerFollowersDescriptionAndLinks() {
        val details = parseProfileDetails(
            """
            {"data":{"user":{
              "description":" Software Developer. ",
              "bannerImageURL":"https://example/banner.png",
              "followers":{"totalCount":255},
              "channel":{"socialMedias":[
                {"name":"website","title":"Personal site","url":"https://example.com"},
                {"name":"twitter","title":"","url":"https://twitter.com/alice"},
                {"name":"junk","title":"Junk","url":"javascript:alert(1)"},
                {"name":"again","title":"Personal site","url":"https://example.com"}
              ]}
            }}}
            """.trimIndent(),
            fallbackLogin = "alice",
        )

        assertEquals("https://example/banner.png", details?.bannerUrl)
        assertEquals("Software Developer.", details?.description)
        assertEquals(255, details?.followerCount)
        assertEquals(
            listOf(
                TwitchProfileLink(title = "Personal site", url = "https://example.com", name = "website"),
                TwitchProfileLink(title = "twitter", url = "https://twitter.com/alice", name = "twitter"),
            ),
            details?.links,
        )
    }

    @Test
    fun missingUserKeepsThePreviousProfile() {
        assertNull(parseProfileDetails(fallbackLogin = "alice", body = """{"data":{"user":null}}"""))
        val previous = TwitchAccount(
            login = "alice",
            displayName = "Alice",
            avatarUrl = null,
            bannerUrl = "https://example/banner.png",
            followerCount = 10,
            links = listOf(TwitchProfileLink("GitHub", "https://github.com/alice")),
        )
        val fresh = TwitchAccount(login = "alice", displayName = "Alice", avatarUrl = "https://example/a.png")

        val kept = profileAfterRefresh(previous, fresh, details = null)

        assertEquals("https://example/banner.png", kept.bannerUrl)
        assertEquals(10, kept.followerCount)
        assertEquals(previous.links, kept.links)
        assertEquals("https://example/a.png", kept.avatarUrl)
    }

    @Test
    fun freshDetailsReplaceTheSavedBanner() {
        val previous = TwitchAccount(
            login = "alice",
            displayName = "Alice",
            avatarUrl = null,
            bannerUrl = "https://example/old.png",
            followerCount = 1,
        )
        val fresh = TwitchAccount(login = "alice", displayName = "Alice", avatarUrl = null)
        val details = ProfileDetails(
            userId = "1",
            login = "alice",
            displayName = "Alice",
            avatarUrl = null,
            bannerUrl = null,
            description = "Hello",
            followerCount = 0,
            createdAtMillis = null,
            links = emptyList(),
        )

        val updated = profileAfterRefresh(previous, fresh, details)

        assertNull(updated.bannerUrl)
        assertEquals(0, updated.followerCount)
        assertEquals("Hello", updated.description)
    }

    @Test
    fun nullSocialMediaFieldKeepsSavedLinks() {
        val savedLinks = listOf(TwitchProfileLink("Personal site", "https://example.com"))
        val previous = TwitchAccount(
            login = "alice",
            displayName = "Alice",
            avatarUrl = null,
            links = savedLinks,
        )
        val fresh = TwitchAccount(login = "alice", displayName = "Alice", avatarUrl = null)
        val details = parseProfileDetails(
            """{"data":{"user":{"channel":{"socialMedias":null}}}}""",
            fallbackLogin = "alice",
        )

        val updated = profileAfterRefresh(previous, fresh, details)

        assertEquals(savedLinks, updated.links)
    }

    @Test
    fun emptySocialMediaArrayClearsSavedLinks() {
        val previous = TwitchAccount(
            login = "alice",
            displayName = "Alice",
            avatarUrl = null,
            links = listOf(TwitchProfileLink("Personal site", "https://example.com")),
        )
        val fresh = TwitchAccount(login = "alice", displayName = "Alice", avatarUrl = null)
        val details = parseProfileDetails(
            """{"data":{"user":{"channel":{"socialMedias":[]}}}}""",
            fallbackLogin = "alice",
        )

        val updated = profileAfterRefresh(previous, fresh, details)

        assertEquals(emptyList<TwitchProfileLink>(), updated.links)
    }

    @Test
    fun anotherLoginDoesNotKeepThePreviousBanner() {
        val previous = TwitchAccount(
            login = "alice",
            displayName = "Alice",
            avatarUrl = null,
            bannerUrl = "https://example/banner.png",
        )
        val fresh = TwitchAccount(login = "bob", displayName = "Bob", avatarUrl = null)

        val updated = profileAfterRefresh(previous, fresh, details = null)

        assertNull(updated.bannerUrl)
        assertEquals("bob", updated.login)
    }
}
