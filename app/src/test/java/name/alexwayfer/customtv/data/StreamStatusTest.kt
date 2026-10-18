package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamStatusTest {
    private val profile = ChannelProfile(
        login = "foo",
        displayName = "Foo",
        avatarUrl = null,
        streamTitle = "Old title",
        categoryName = "Old game",
        viewerCount = 12,
        sharedViewerCount = 40,
        isLive = true,
        streamStartedAtMillis = 5L,
    )

    @Test
    fun aTitleUpdateKeepsViewersAndLiveState() {
        val next = profileAfterStreamStatus(
            profile,
            StreamStatusEvent.Title(login = "foo", title = "New title", categoryName = "Just Chatting"),
        )
        assertEquals("New title", next?.streamTitle)
        assertEquals("Just Chatting", next?.categoryName)
        assertEquals(12, next?.viewerCount)
        assertEquals(true, next?.isLive)
    }

    @Test
    fun aNewCategoryTakesItsIdAndDropsTheOldBoxArt() {
        val withCategory = profile.copy(categoryName = "Art", categoryId = "509660", categoryBoxArtUrl = "art.jpg")
        val next = profileAfterStreamStatus(
            withCategory,
            StreamStatusEvent.Title(login = "foo", title = "t", categoryName = "Just Chatting", categoryId = "509658"),
        )
        assertEquals("509658", next?.categoryId)
        assertNull(next?.categoryBoxArtUrl)
    }

    @Test
    fun theSameCategoryKeepsItsBoxArt() {
        val withCategory = profile.copy(categoryName = "Art", categoryId = "509660", categoryBoxArtUrl = "art.jpg")
        val next = profileAfterStreamStatus(
            withCategory,
            StreamStatusEvent.Title(login = "foo", title = "New title", categoryName = "Art", categoryId = "509660"),
        )
        assertEquals("art.jpg", next?.categoryBoxArtUrl)
    }

    @Test
    fun aBlankCategoryClearsTheCategory() {
        val next = profileAfterStreamStatus(
            profile,
            StreamStatusEvent.Title(login = "Foo", title = "New title", categoryName = null),
        )
        assertNull(next?.categoryName)
    }

    @Test
    fun goingLiveKeepsThePreviousTitle() {
        val offline = profile.copy(isLive = false, streamStartedAtMillis = null)
        val next = profileAfterStreamStatus(
            offline,
            StreamStatusEvent.Online(login = "foo", startedAtMillis = 90L),
        )
        assertEquals(true, next?.isLive)
        assertEquals(90L, next?.streamStartedAtMillis)
        assertEquals("Old title", next?.streamTitle)
    }

    @Test
    fun goingOfflineClearsViewersAndKeepsTheTitle() {
        val next = profileAfterStreamStatus(profile, StreamStatusEvent.Offline("foo"))
        assertEquals(false, next?.isLive)
        assertNull(next?.viewerCount)
        assertNull(next?.sharedViewerCount)
        assertEquals("Old title", next?.streamTitle)
    }

    @Test
    fun anEventForAnotherChannelIsIgnored() {
        assertNull(profileAfterStreamStatus(profile, StreamStatusEvent.Offline("other")))
    }

    @Test
    fun aChannelUpdateNotificationBecomesATitle() {
        val event = parseStreamStatusNotification(
            """
            {"metadata":{"message_type":"notification"},"payload":{
              "subscription":{"type":"channel.update"},
              "event":{"broadcaster_user_login":"Foo","title":"Hello","category_name":"  "}
            }}
            """.trimIndent(),
        )
        assertEquals(StreamStatusEvent.Title("foo", "Hello", null), event)
    }

    @Test
    fun aChannelUpdateKeepsItsCategoryId() {
        val event = parseStreamStatusNotification(
            """
            {"metadata":{"message_type":"notification"},"payload":{
              "subscription":{"type":"channel.update"},
              "event":{"broadcaster_user_login":"foo","title":"Hi","category_name":"Just Chatting",
                "category_id":"509658"}
            }}
            """.trimIndent(),
        )
        assertEquals(StreamStatusEvent.Title("foo", "Hi", "Just Chatting", categoryId = "509658"), event)
    }

    @Test
    fun aNewCategoryIsNamedInTheAppsLanguage() {
        val event = StreamStatusEvent.Title("foo", "Hi", "Just Chatting", categoryId = "509658")
        assertEquals(
            event.copy(categoryName = "Общение"),
            localizedStreamStatus(event, mapOf("509658" to "Общение")),
        )
    }

    @Test
    fun withoutItsNameACategoryKeepsTwitchsEnglishOne() {
        val event = StreamStatusEvent.Title("foo", "Hi", "Just Chatting", categoryId = "509658")
        assertEquals(event, localizedStreamStatus(event, emptyMap()))
        val offline = StreamStatusEvent.Offline("foo")
        assertEquals(offline, localizedStreamStatus(offline, mapOf("509658" to "Общение")))
    }

    @Test
    fun aStreamOnlineNotificationKeepsTheStartTime() {
        val event = parseStreamStatusNotification(
            """
            {"metadata":{"message_type":"notification"},"payload":{
              "subscription":{"type":"stream.online"},
              "event":{"broadcaster_user_login":"foo","started_at":"2020-01-01T00:00:00Z"}
            }}
            """.trimIndent(),
        )
        assertEquals(
            StreamStatusEvent.Online("foo", java.time.Instant.parse("2020-01-01T00:00:00Z").toEpochMilli()),
            event,
        )
    }

    @Test
    fun aKeepaliveIsNotAStreamUpdate() {
        assertNull(parseStreamStatusNotification("""{"metadata":{"message_type":"session_keepalive"}}"""))
    }

    @Test
    fun aWelcomeMessageExposesTheSessionId() {
        assertEquals(
            "session-1",
            eventSubSessionId(
                """
                {"metadata":{"message_type":"session_welcome"},"payload":{"session":{"id":"session-1"}}}
                """.trimIndent(),
            ),
        )
    }
}
