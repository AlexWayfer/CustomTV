package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.chat.ChatterFollow
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowedChannelsTest {
    private fun millis(instant: String): Long = Instant.parse(instant).toEpochMilli()
    @Test
    fun followedPageKeepsTheCursorAndSkipsABlankId() {
        val page = parseFollowedPage(
            """
            {"data":[
              {"broadcaster_id":"1","broadcaster_login":"alice","broadcaster_name":"Alice"},
              {"broadcaster_id":"","broadcaster_login":"skip","broadcaster_name":"Skip"},
              {"broadcaster_id":"2","broadcaster_login":"bob","broadcaster_name":"Bob"}
            ],"pagination":{"cursor":"next"}}
            """.trimIndent(),
        )

        assertEquals(listOf("alice", "bob"), page?.channels?.map { it.login })
        assertEquals("Alice", page?.channels?.first()?.displayName)
        assertEquals("next", page?.cursor)
    }

    @Test
    fun ownFollowDateMatchesThatBroadcaster() {
        val url = ownFollowedAtUrl("42", "99")
        assertTrue(url.contains("user_id=42"))
        assertTrue(url.contains("broadcaster_id=99"))
        val body = """
            {"data":[
              {"broadcaster_id":"99","broadcaster_login":"channel","followed_at":"2020-05-01T12:00:00Z"},
              {"broadcaster_id":"1","followed_at":"2019-01-01T00:00:00Z"}
            ]}
        """.trimIndent()
        assertEquals(millis("2020-05-01T12:00:00Z"), parseOwnFollowedAt(body, "99"))
    }

    @Test
    fun subscriptionCheckUsesTheUserAndTheBroadcaster() {
        val url = userSubscriptionUrl("42", "99")
        assertTrue(url.contains("subscriptions/user"))
        assertTrue(url.contains("user_id=42"))
        assertTrue(url.contains("broadcaster_id=99"))
    }

    @Test
    fun missingSubscriptionIsNotFoundAndAListedOneIsSubscribed() {
        assertEquals(
            UserSubscriptionRead.NotSubscribed,
            userSubscriptionRead(404, """{"error":"Not Found","status":404}"""),
        )
        assertEquals(
            UserSubscriptionRead.Subscribed,
            userSubscriptionRead(200, """{"data":[{"broadcaster_id":"99","tier":"1000"}]}"""),
        )
        assertEquals(UserSubscriptionRead.NotSubscribed, userSubscriptionRead(200, """{"data":[]}"""))
        assertEquals(UserSubscriptionRead.Unavailable, userSubscriptionRead(401, """{"message":"missing scope"}"""))
        assertEquals(UserSubscriptionRead.Unavailable, userSubscriptionRead(500, """{"error":"Internal Server Error"}"""))
    }

    @Test
    fun ownFollowDateIsMissingWhenTheUserDoesNotFollow() {
        assertNull(parseOwnFollowedAt("""{"data":[]}""", "99"))
        assertNull(parseOwnFollowedAt("""{"data":[{"broadcaster_id":"99","followed_at":"nope"}]}""", "99"))
        assertNull(parseOwnFollowedAt("""{"data":[{"broadcaster_id":"1","followed_at":"2020-05-01T12:00:00Z"}]}""", "99"))
    }

    @Test
    fun followedPageWithoutACursorIsTheLastPage() {
        val page = parseFollowedPage("""{"data":[],"pagination":{}}""")
        assertEquals(emptyList<FollowedChannel>(), page?.channels)
        assertNull(page?.cursor)
    }

    @Test
    fun liveStreamsSkipAnythingThatIsNotLive() {
        val live = parseFollowedLive(
            """
            {"data":[
              {"user_id":"1","type":"live","game_name":"Art","viewer_count":12,"title":"Painting"},
              {"user_id":"2","type":"","game_name":"Old","viewer_count":3}
            ]}
            """.trimIndent(),
        )

        assertEquals(setOf("1"), live?.keys)
        assertEquals("Art", live?.get("1")?.categoryName)
        assertEquals(12, live?.get("1")?.viewerCount)
        assertEquals("Painting", live?.get("1")?.title)
    }

    @Test
    fun avatarsSkipABlankPicture() {
        val avatars = parseFollowedAvatars(
            """
            {"data":[
              {"id":"1","profile_image_url":"https://example/a.png"},
              {"id":"2","profile_image_url":""}
            ]}
            """.trimIndent(),
        )

        assertEquals(mapOf("1" to "https://example/a.png"), avatars)
    }

    @Test
    fun liveChannelsComeFirstByViewersThenName() {
        val ordered = mergeFollowedChannels(
            followed = listOf(
                FollowedChannel(id = "offline", login = "zoe", displayName = "Zoe"),
                FollowedChannel(id = "small", login = "amy", displayName = "amy"),
                FollowedChannel(id = "big", login = "Bea", displayName = "Bea"),
            ),
            avatarsById = mapOf("offline" to "https://example/z.png"),
            liveById = mapOf(
                "small" to FollowedLive(categoryName = "Just Chatting", viewerCount = 4),
                "big" to FollowedLive(categoryName = null, viewerCount = 40),
            ),
        )

        assertEquals(listOf("Bea", "amy", "Zoe"), ordered.map { it.displayName })
        assertEquals("https://example/z.png", ordered.last().avatarUrl)
        assertFalse(ordered.last().isLive)
        assertEquals(40, ordered.first().viewerCount)
    }

    @Test
    fun namesWithoutLiveOrDatesSortByDisplayName() {
        val ordered = mergeFollowedChannels(
            followed = listOf(
                FollowedChannel(id = "2", login = "zoe", displayName = "Zoe"),
                FollowedChannel(id = "1", login = "amy", displayName = "amy"),
            ),
            avatarsById = emptyMap(),
            liveById = emptyMap(),
        )

        assertEquals(listOf("amy", "Zoe"), ordered.map { it.displayName })
        assertFalse(ordered.any { it.isLive })
    }

    @Test
    fun refreshingLiveClearsAChannelThatWentOffline() {
        val refreshed = refreshFollowedLive(
            channels = listOf(
                FollowedChannel(
                    id = "1",
                    login = "amy",
                    displayName = "Amy",
                    avatarUrl = "https://example/a.png",
                    isLive = true,
                    categoryName = "Art",
                    viewerCount = 9,
                    lastBroadcastAtMillis = 5_000L,
                ),
            ),
            liveById = emptyMap(),
        )

        assertFalse(refreshed.single().isLive)
        assertNull(refreshed.single().categoryName)
        assertNull(refreshed.single().viewerCount)
        assertEquals("https://example/a.png", refreshed.single().avatarUrl)
        assertEquals(5_000L, refreshed.single().lastBroadcastAtMillis)
        assertNull(refreshed.single().sharedViewerCount)
        assertNull(refreshed.single().collaborationCount)
    }

    @Test
    fun refreshingLiveKeepsCollaborationWhenTheSharedLookupFailsAndAppliesItWhenFetched() {
        val channel = FollowedChannel(
            id = "1",
            login = "Amy",
            displayName = "Amy",
            isLive = true,
            viewerCount = 9,
            sharedViewerCount = 40,
            collaborationCount = 2,
        )
        val kept = refreshFollowedLive(
            channels = listOf(channel),
            liveById = mapOf("1" to FollowedLive(categoryName = "Art", viewerCount = 12)),
            collaborationByLogin = emptyMap(),
            collaborationFetchedLogins = emptySet(),
        )
        assertEquals(12, kept.single().viewerCount)
        assertEquals(40, kept.single().sharedViewerCount)
        assertEquals(2, kept.single().collaborationCount)

        val applied = refreshFollowedLive(
            channels = listOf(channel),
            liveById = mapOf("1" to FollowedLive(categoryName = "Art", viewerCount = 12)),
            collaborationByLogin = mapOf("amy" to FollowedCollaboration(sharedViewerCount = 80, collaboratorCount = 4)),
            collaborationFetchedLogins = setOf("amy"),
        )
        assertEquals(80, applied.single().sharedViewerCount)
        assertEquals(4, applied.single().collaborationCount)
    }

    @Test
    fun offlineChannelsWithABroadcastDateComeBeforeUndatedNames() {
        val ordered = orderedFollowedChannels(
            listOf(
                FollowedChannel(id = "undated-z", login = "zoe", displayName = "Zoe"),
                FollowedChannel(id = "old", login = "bea", displayName = "Bea", lastBroadcastAtMillis = 1_000L),
                FollowedChannel(
                    id = "live-small",
                    login = "cal",
                    displayName = "Cal",
                    isLive = true,
                    viewerCount = 5,
                    lastBroadcastAtMillis = 9_000L,
                ),
                FollowedChannel(id = "undated-a", login = "amy", displayName = "amy"),
                FollowedChannel(
                    id = "live-big",
                    login = "dee",
                    displayName = "Dee",
                    isLive = true,
                    viewerCount = 50,
                ),
                FollowedChannel(id = "recent", login = "mo", displayName = "Mo", lastBroadcastAtMillis = 8_000L),
            ),
        )

        assertEquals(listOf("Dee", "Cal", "Mo", "Bea", "amy", "Zoe"), ordered.map { it.displayName })
        assertNull(followedLastBroadcastMillis(ordered.first { it.login == "cal" }))
        assertEquals(8_000L, followedLastBroadcastMillis(ordered.first { it.login == "mo" }))
    }

    @Test
    fun lastBroadcastJsonKeepsAParsedStartAndSkipsABlankOne() {
        val parsed = parseLastBroadcasts(
            """
            {"data":{"users":[
              {"login":"Mo","lastBroadcast":{"startedAt":"2026-09-23T21:59:27.009321Z"}},
              {"login":"bea","lastBroadcast":null},
              {"login":"zoe","lastBroadcast":{"startedAt":"not-a-time"}}
            ]}}
            """.trimIndent(),
        )

        assertEquals(
            mapOf("mo" to Instant.parse("2026-09-23T21:59:27.009321Z").toEpochMilli()),
            parsed,
        )
        assertNull(parseLastBroadcasts("""{"errors":[{"message":"no"}]}"""))
        assertTrue(lastBroadcastsQuery(listOf("mo", "bea")).contains("mo"))
        assertTrue(lastBroadcastsQuery(listOf("mo")).contains("collaborators"))
    }

    @Test
    fun collaborationJsonCountsActiveOthersAndKeepsALargerSharedTotal() {
        val parsed = parseFollowedCollaborations(
            """
            {"data":{"users":[
              {"login":"Amy","stream":{"viewersCount":10,"collaborationViewersCount":40},
               "channel":{"collaboration":{"collaborators":[
                 {"status":"ACTIVE","user":{"login":"Amy"}},
                 {"status":"ACTIVE","user":{"login":"bea"}},
                 {"status":"ACTIVE","user":{"login":"cal"}},
                 {"status":"ACTIVE","user":{"login":"dee"}},
                 {"status":"ACTIVE","user":{"login":"mo"}},
                 {"status":"LEFT","user":{"login":"zoe"}}
               ]}}},
              {"login":"solo","stream":{"viewersCount":10,"collaborationViewersCount":10},
               "channel":{"collaboration":{"collaborators":[{"status":"ACTIVE","user":{"login":"solo"}}]}}}
            ]}}
            """.trimIndent(),
        )

        assertEquals(FollowedCollaboration(sharedViewerCount = 40, collaboratorCount = 4), parsed?.get("amy"))
        assertNull(parsed?.get("solo"))
        assertNull(parseFollowedCollaborations("""{"errors":[{"message":"no"}]}"""))
    }

    @Test
    fun collaborationJsonKeepsOtherActiveAvatarsInOrderAndSkipsOwnLeftAndMissing() {
        val parsed = parseFollowedCollaborations(
            """
            {"data":{"users":[
              {"login":"Amy","stream":{"viewersCount":10},
               "channel":{"collaboration":{"collaborators":[
                 {"status":"ACTIVE","user":{"login":"amy","profileImageURL":"amy.png"}},
                 {"status":"ACTIVE","user":{"login":"cal","profileImageURL":"cal.png"}},
                 {"status":"ACTIVE","user":{"login":"bea","profileImageURL":null}},
                 {"status":"ACTIVE","user":{"login":"dee","profileImageURL":"dee.png"}},
                 {"status":"LEFT","user":{"login":"zoe","profileImageURL":"zoe.png"}}
               ]}}}
            ]}}
            """.trimIndent(),
        )

        assertEquals(listOf("cal.png", "dee.png"), parsed?.get("amy")?.collaboratorAvatarUrls)
        assertEquals(3, parsed?.get("amy")?.collaboratorCount)
    }

    @Test
    fun aBroadcastEarlierTodayIsToday() {
        assertEquals(
            FollowedLastBroadcastAge.Today,
            followedLastBroadcastAge(millis("2026-09-24T01:00:00Z"), millis("2026-09-24T15:00:00Z"), ZoneOffset.UTC),
        )
    }

    @Test
    fun aBroadcastOnThePreviousCalendarDayIsYesterday() {
        assertEquals(
            FollowedLastBroadcastAge.Yesterday,
            followedLastBroadcastAge(millis("2026-09-23T23:00:00Z"), millis("2026-09-24T01:00:00Z"), ZoneOffset.UTC),
        )
    }

    @Test
    fun aBroadcastThreeDaysAgoIsThreeDays() {
        assertEquals(
            FollowedLastBroadcastAge.DaysAgo(3),
            followedLastBroadcastAge(millis("2026-09-21T15:00:00Z"), millis("2026-09-24T15:00:00Z"), ZoneOffset.UTC),
        )
    }

    @Test
    fun aBroadcastOneCalendarMonthAgoIsOneMonth() {
        assertEquals(
            FollowedLastBroadcastAge.MonthsAgo(1),
            followedLastBroadcastAge(millis("2026-08-24T15:00:00Z"), millis("2026-09-24T15:00:00Z"), ZoneOffset.UTC),
        )
    }

    @Test
    fun aBroadcastOneYearAgoIsOneYear() {
        assertEquals(
            FollowedLastBroadcastAge.YearsAgo(1),
            followedLastBroadcastAge(millis("2025-09-24T15:00:00Z"), millis("2026-09-24T15:00:00Z"), ZoneOffset.UTC),
        )
    }

    @Test
    fun aRecentOfflineChannelComesBeforeAnUndatedOneWhenDatesArriveWithTheList() {
        val ordered = mergeFollowedChannels(
            followed = listOf(
                FollowedChannel(id = "z", login = "zoe", displayName = "Zoe"),
                FollowedChannel(id = "a", login = "amy", displayName = "amy"),
            ),
            avatarsById = emptyMap(),
            liveById = emptyMap(),
            lastBroadcastByLogin = mapOf("zoe" to 5_000L),
        )

        assertEquals(listOf("Zoe", "amy"), ordered.map { it.displayName })
        assertEquals(5_000L, ordered.first().lastBroadcastAtMillis)
    }

    @Test
    fun scopeFailureIsOnlyTheFollowsPermission() {
        assertTrue(helixRejectsFollowScope(401, """{"message":"missing scope"}"""))
        assertFalse(helixRejectsFollowScope(401, """{"message":"invalid access token"}"""))
        assertFalse(helixRejectsFollowScope(500, """{"message":"missing scope"}"""))
    }

    @Test
    fun followedUrlPagesWithTheCursor() {
        assertEquals(
            "https://api.twitch.tv/helix/channels/followed?user_id=42&first=100",
            followedChannelsUrl(userId = "42", cursor = null),
        )
        assertTrue(followedChannelsUrl(userId = "42", cursor = "abc").contains("after=abc"))
        assertTrue(followedStreamsUrl(listOf("1", "2")).contains("user_id=1"))
        assertTrue(followedStreamsUrl(listOf("1", "2")).contains("user_id=2"))
    }

    @Test
    fun liveFollowedStreamsUrlAsksForTheUsersLiveFollowsAfterTheCursor() {
        val url = liveFollowedStreamsUrl(userId = "42", cursor = "abc")

        assertTrue(url.contains("/streams/followed?"))
        assertTrue(url.contains("user_id=42"))
        assertTrue(url.contains("first=$FOLLOWED_PAGE_SIZE"))
        assertTrue(url.contains("after=abc"))
        assertFalse(liveFollowedStreamsUrl(userId = "42", cursor = null).contains("after="))
    }

    @Test
    fun liveFollowedPageKeepsTheStreamAndCursorAndSkipsABlankId() {
        val page = parseLiveFollowedStreamsPage(
            """
            {"data":[
              {"user_id":"1","user_login":"alice","user_name":"Alice","type":"live",
                "game_name":"Art","title":"  Drawing  ","viewer_count":12},
              {"user_id":"","user_login":"skip","user_name":"Skip","type":"live"},
              {"user_id":"2","user_login":"bob","user_name":"","type":"live","game_name":"","title":null}
            ],"pagination":{"cursor":"next"}}
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                FollowedChannel(
                    id = "1",
                    login = "alice",
                    displayName = "Alice",
                    isLive = true,
                    categoryName = "Art",
                    viewerCount = 12,
                    streamTitle = "Drawing",
                ),
                FollowedChannel(id = "2", login = "bob", displayName = "bob", isLive = true, viewerCount = 0, streamTitle = ""),
            ),
            page?.channels,
        )
        assertEquals("next", page?.cursor)
    }

    @Test
    fun theLastLiveFollowedPageHasNoCursor() {
        val page = parseLiveFollowedStreamsPage("""{"data":[],"pagination":{}}""")

        assertEquals(emptyList<FollowedChannel>(), page?.channels)
        assertNull(page?.cursor)
    }

    @Test
    fun aLiveFollowedBodyWithoutDataIsUnreadable() {
        assertNull(parseLiveFollowedStreamsPage("""{"error":"Unauthorized"}"""))
    }

    @Test
    fun theHomeListKeepsTheLiveTitleAndClearsItOffline() {
        val channels = listOf(
            FollowedChannel(id = "1", login = "alice", displayName = "Alice"),
            FollowedChannel(id = "2", login = "bob", displayName = "Bob", isLive = true, streamTitle = "Old"),
        )
        val live = mapOf("1" to FollowedLive(categoryName = "Art", viewerCount = 5, title = "Drawing"))

        val merged = mergeFollowedChannels(channels, emptyMap(), live).associateBy { it.id }
        val refreshed = refreshFollowedLive(channels, live).associateBy { it.id }

        assertEquals("Drawing", merged.getValue("1").streamTitle)
        assertNull(merged.getValue("2").streamTitle)
        assertEquals("Drawing", refreshed.getValue("1").streamTitle)
        assertNull(refreshed.getValue("2").streamTitle)
    }

    @Test
    fun rateLimitResetSecondsBecomeMillis() {
        assertEquals(1_790_853_002_000L, helixRateLimitResetMillis("1790853002"))
    }

    @Test
    fun aMissingOrBrokenRateLimitResetIsUnknown() {
        assertNull(helixRateLimitResetMillis(null))
        assertNull(helixRateLimitResetMillis("soon"))
        assertNull(helixRateLimitResetMillis("0"))
    }

    @Test
    fun anOwnFollowReadBecomesTheCardsFollowLine() {
        assertEquals(ChatterFollow.Following(5), chatterFollowOf(OwnFollowRead.Following(5)))
        assertEquals(ChatterFollow.NotFollowing, chatterFollowOf(OwnFollowRead.NotFollowing))
        assertNull(chatterFollowOf(OwnFollowRead.Unavailable))
    }
}
