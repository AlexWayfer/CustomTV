package name.alexwayfer.customtv.ui.account

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TwitchVideosTest {
    @Test
    fun responseParsesVideoMetadataAndItsChapters() {
        val page = parseChannelRecordings(
            """
            {"data":{"user":{"videos":{"edges":[{"node":{
              "id":"123","title":" A stream ","previewThumbnailURL":"https://cdn/640x360.jpg",
              "createdAt":"2026-09-20T12:25:00Z","publishedAt":"2026-09-20T12:30:00Z",
              "viewCount":1234,"lengthSeconds":7384
            }}]},
            "chapters":{"edges":[{"node":{
              "id":"123",
              "game":{"displayName":"Just Chatting","boxArtURL":"jc.jpg"},
              "moments":{"edges":[
                {"node":{
                  "positionMilliseconds":60000,
                  "details":{"game":{"displayName":"Dota 2","boxArtURL":"dota.jpg"}}
                }}
              ]}
            }}]}}}}
            """.trimIndent(),
        )

        val video = page?.videos?.single()
        assertEquals("123", video?.id)
        assertEquals("A stream", video?.title)
        assertEquals("https://cdn/640x360.jpg", video?.thumbnailUrl)
        assertEquals(Instant.parse("2026-09-20T12:25:00Z").toEpochMilli(), video?.startedAtMillis)
        assertEquals(Instant.parse("2026-09-20T12:30:00Z").toEpochMilli(), video?.publishedAtMillis)
        assertEquals(1234L, video?.viewCount)
        assertEquals(7_384L, video?.durationSeconds)
        assertEquals(listOf(VodChapter(60.0, "Dota 2", "dota.jpg")), page?.chapters?.get("123")?.chapters)
        assertEquals("jc.jpg", page?.chapters?.get("123")?.fallbackBoxArtUrl)
        assertEquals(emptySet<String>(), page?.chaptersFailed)
    }

    @Test
    fun aFailedChaptersAliasKeepsTheRecordingsAndMarksAllTheirChaptersFailed() {
        val page = parseChannelRecordings(
            """
            {"errors":[{"message":"service error","path":["user","chapters"]}],
             "data":{"user":{"videos":{"edges":[{"node":{"id":"1"}},{"node":{"id":"2"}}]},"chapters":null}}}
            """.trimIndent(),
        )

        assertEquals(listOf("1", "2"), page?.videos?.map { it.id })
        assertEquals(emptyMap<String, VodCategories>(), page?.chapters)
        assertEquals(setOf("1", "2"), page?.chaptersFailed)
    }

    @Test
    fun chaptersOneRecordingFailedOnAreFailedAndTheOthersLoad() {
        val page = parseChannelRecordings(
            """
            {"errors":[{"message":"service error","path":["user","chapters","edges",1,"node","moments"]}],
             "data":{"user":{
               "videos":{"edges":[{"node":{"id":"1"}},{"node":{"id":"2"}},{"node":{"id":"3"}}]},
               "chapters":{"edges":[
                 {"node":{"id":"1","moments":{"edges":[]}}},
                 {"node":{"id":"2","moments":null}},
                 {"node":{"id":"3","moments":{"edges":[]}}}
               ]}
             }}}
            """.trimIndent(),
        )

        assertEquals(setOf("1", "3"), page?.chapters?.keys)
        assertEquals(setOf("2"), page?.chaptersFailed)
    }

    @Test
    fun aRecordingMissingFromTheChaptersAliasHasFailedChapters() {
        val page = parseChannelRecordings(
            """
            {"data":{"user":{
              "videos":{"edges":[{"node":{"id":"new"}},{"node":{"id":"1"}}]},
              "chapters":{"edges":[{"node":{"id":"1","moments":{"edges":[]}}}]}
            }}}
            """.trimIndent(),
        )

        assertEquals(setOf("new"), page?.chaptersFailed)
    }

    @Test
    fun suggestedChannelsKeepTheStreamersOrderAndTheirLiveState() {
        val page = parseChannelRecordings(
            """
            {"data":{"user":{"videos":{"edges":[]},"channel":{"home":{"shelves":{"streamerShelf":{"edges":[
              {"node":{"id":"1","login":"Shelik","displayName":"Shelik","stream":null}},
              {"node":{"id":"2","login":"ilame","displayName":"iLame",
                "stream":{"title":"Dogs","viewersCount":1880,"game":{"name":"WARDOGS"}}}},
              {"node":{"id":"3","displayName":"No login"}}
            ]}}}}}}}
            """.trimIndent(),
        )

        val suggested = page?.suggestedChannels.orEmpty()
        assertEquals(listOf("shelik", "ilame"), suggested.map { it.login })
        assertEquals(listOf(false, true), suggested.map { it.isLive })
        assertEquals("Dogs", suggested[1].streamTitle)
        assertEquals("WARDOGS", suggested[1].categoryName)
        assertEquals(1880, suggested[1].viewerCount)
    }

    @Test
    fun aFailedSuggestedShelfKeepsTheRecordings() {
        val page = parseChannelRecordings(
            """
            {
              "errors":[{"message":"shelf failed"}],
              "data":{"user":{
                "videos":{"edges":[{"node":{"id":"1"}}]},
                "channel":{"home":{"shelves":{"streamerShelf":null}}}
              }}
            }
            """.trimIndent(),
        )

        assertEquals(listOf("1"), page?.videos?.map { it.id })
        assertEquals(emptyList<Any>(), page?.suggestedChannels)
    }

    @Test
    fun theRecordingsAreAskedAgainOnlyWhenAServerFailureTookThem() {
        val lost = """
            {"errors":[{"message":"service error","path":["user","videos"]}],"data":{"user":{"videos":null}}}
        """.trimIndent()
        val chaptersOnly = """
            {"errors":[{"message":"service error","path":["user","chapters"]}],
             "data":{"user":{"videos":{"edges":[{"node":{"id":"1"}}]},"chapters":null}}}
        """.trimIndent()
        val schema = """{"errors":[{"message":"Cannot query field"}],"data":{"user":{"videos":null}}}"""

        assertEquals(listOf(true, false, false), listOf(lost, chaptersOnly, schema).map(::recordingsRetried))
    }

    @Test
    fun aChannelThatDoesNotExistHasNoRecordings() {
        assertEquals(
            ChannelRecordingsPage(emptyList(), emptyMap()),
            parseChannelRecordings("""{"data":{"user":null}}"""),
        )
    }

    @Test
    fun errorsThatLeaveOutTheUserAreUnavailableNotEmpty() {
        assertNull(parseChannelRecordings("""{"errors":[{"message":"service timeout"}],"data":{"user":null}}"""))
    }

    @Test
    fun errorsThatLeaveOutTheVideosAreUnavailableNotEmpty() {
        assertNull(parseChannelRecordings("""{"errors":[{"message":"failed"}],"data":{"user":{"videos":null}}}"""))
    }

    @Test
    fun malformedResponseIsUnavailable() {
        assertNull(parseChannelRecordings("not json"))
        assertNull(parseChannelRecordings("""{"errors":[{"message":"bad"}]}"""))
    }

    @Test
    fun durationLabelShowsHoursOnlyWhenThereAreAny() {
        assertEquals("2:03:04", twitchVideoDurationLabel(7_384L))
        assertEquals("3:04", twitchVideoDurationLabel(184L))
    }
}
