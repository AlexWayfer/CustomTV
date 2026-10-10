package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelProfileParserTest {
    @Test
    fun offlineProfileFallsBackToBroadcastSettingsAndRequestedLogin() {
        val parsed = ChannelProfileParser.parse(
            body = """
                {"data":{"user":{
                  "id":"42",
                  "login":"",
                  "displayName":"",
                  "stream":null,
                  "broadcastSettings":{"title":"Offline title","game":{"name":"Just Chatting"}}
                }}}
            """.trimIndent(),
            requestedLogin = "requested",
        )!!

        assertEquals("requested", parsed.profile.login)
        assertEquals("requested", parsed.profile.displayName)
        assertEquals("Offline title", parsed.profile.streamTitle)
        assertEquals("Just Chatting", parsed.profile.categoryName)
        assertFalse(parsed.profile.isLive)
        assertNull(parsed.profile.viewerCount)
    }

    @Test
    fun liveProfileKeepsOwnAndSharedViewerCountsSeparateAndParsesRewards() {
        val parsed = ChannelProfileParser.parse(
            body = """
                {"data":{"user":{
                  "id":"42",
                  "login":"Streamer",
                  "displayName":"Streamer",
                  "profileImageURL":"https://avatar",
                  "primaryColorHex":"#111111",
                  "stream":{
                    "title":"Live title",
                    "createdAt":"2025-01-02T03:04:05Z",
                    "archiveVideo":{"id":"video-42"},
                    "viewersCount":100,
                    "collaborationViewersCount":250,
                    "game":{"name":"Game"},
                    "freeformTags":[
                      {"name":"English"},
                      {"name":" "},
                      {"name":"DropsEnabled"}
                    ]
                  },
                  "channel":{
                    "collaboration":{"collaborators":[
                      {"status":"ACTIVE","user":{"login":"Streamer"}},
                      {"status":"ACTIVE","user":{"login":"a"}},
                      {"status":"ACTIVE","user":{"login":"b"}},
                      {"status":"INVITED","user":{"login":"c"}},
                      {"status":"ACTIVE","user":{"login":"d"}},
                      {"status":"ACTIVE","user":{"login":"e"}}
                    ]},
                    "communityPointsSettings":{
                    "image":{"url":"https://points-1.png"},
                    "automaticRewards":[{
                      "type":"SEND_HIGHLIGHTED_MESSAGE",
                      "defaultCost":300,
                      "backgroundColor":"#222222"
                    }],
                    "customRewards":[{
                      "id":"reward-1",
                      "title":"Reward",
                      "prompt":"Prompt",
                      "cost":500,
                      "backgroundColor":"#333333",
                      "defaultImage":{"url":"https://reward-1.png"}
                    }]
                  }}
                }}}
            """.trimIndent(),
            requestedLogin = "ignored",
        )!!

        assertEquals("streamer", parsed.profile.login)
        assertTrue(parsed.profile.isLive)
        assertEquals("video-42", parsed.profile.currentStreamVideoId)
        assertEquals(100, parsed.profile.viewerCount)
        assertEquals(250, parsed.profile.sharedViewerCount)
        assertEquals(4, parsed.profile.collaborationCount)
        assertEquals(300, parsed.profile.highlightRewardCost)
        assertEquals("#222222", parsed.profile.highlightColorHex)
        assertEquals("#111111", parsed.profile.primaryColorHex)
        assertEquals("https://points-2.png", parsed.profile.channelPointsIconUrl)
        assertEquals("Reward", parsed.customRewards["reward-1"]?.title)
        assertEquals("https://reward-2.png", parsed.customRewards["reward-1"]?.imageUrl)
        assertEquals(listOf("English", "DropsEnabled"), parsed.profile.tags)
    }

    @Test
    fun missingUserReturnsNull() {
        assertNull(ChannelProfileParser.parse("{\"data\":{\"user\":null}}", "channel"))
    }

    @Test
    fun collaborationCountEqualToOwnDoesNotMarkSharedStream() {
        val parsed = ChannelProfileParser.parse(
            """{"data":{"user":{"login":"solo","stream":{"viewersCount":100,"collaborationViewersCount":100}}}}""",
            "solo",
        )!!
        assertEquals(100, parsed.profile.viewerCount)
        assertNull(parsed.profile.sharedViewerCount)
    }

    @Test
    fun missingOwnViewerCountDoesNotShowSharedStream() {
        val parsed = ChannelProfileParser.parse(
            """{"data":{"user":{"login":"solo","stream":{"collaborationViewersCount":250}}}}""",
            "solo",
        )!!
        assertNull(parsed.profile.viewerCount)
        assertNull(parsed.profile.sharedViewerCount)
    }
}
