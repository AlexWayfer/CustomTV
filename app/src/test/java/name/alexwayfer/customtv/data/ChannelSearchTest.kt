package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelSearchTest {
    @Test
    fun oneCharacterDoesNotSearchTwitch() {
        assertNull(channelSearchQuery("x"))
    }

    @Test
    fun twoCharactersSearchTheNormalizedLogin() {
        assertEquals("xq", channelSearchQuery("  XQ "))
    }

    @Test
    fun threeCharactersSearchTheNormalizedLogin() {
        assertEquals("xqc", channelSearchQuery("https://twitch.tv/@XQC"))
    }

    @Test
    fun suggestionsKeepChannelsAndSkipCategories() {
        val hits = parseChannelSearchSuggestions(
            """
            {"data":{"searchSuggestions":{"edges":[
              {"node":{"text":"Poko","content":{
                "__typename":"SearchSuggestionChannel",
                "id":"123",
                "login":"Poko",
                "profileImageURL":"https://cdn/poko.png",
                "isLive":true,
                "user":{"stream":{"viewersCount":3816,"game":{"displayName":"Общение"}}}
              }}},
              {"node":{"text":"Poker","content":{"__typename":"SearchSuggestionCategory"}}},
              {"node":{"text":"akira","content":null}},
              {"node":{"text":"","content":{"__typename":"SearchSuggestionChannel","login":"","isLive":false}}}
            ]}}}
            """.trimIndent(),
        )
        assertEquals(1, hits.size)
        assertEquals("poko", hits[0].login)
        assertEquals("Poko", hits[0].displayName)
        assertEquals("https://cdn/poko.png", hits[0].avatarUrl)
        assertTrue(hits[0].isLive)
        assertEquals("123", hits[0].userId)
        assertEquals(3816, hits[0].details?.viewerCount)
        assertEquals("Общение", hits[0].details?.categoryName)
    }

    @Test
    fun aSuggestionWithoutAnIdHasNoUserId() {
        val hits = parseChannelSearchSuggestions(
            """{"data":{"searchSuggestions":{"edges":[{"node":{"text":"Poko","content":""" +
                """{"__typename":"SearchSuggestionChannel","login":"poko","isLive":false}}}]}}}""",
        )
        assertNull(hits.single().userId)
    }

    @Test
    fun offlineSuggestionReadsItsBroadcastDate() {
        val user = org.json.JSONObject("""{"stream":null,"lastBroadcast":{"startedAt":"2026-11-01T12:00:00Z"}}""")
        assertEquals(1793534400000L, channelSearchUserDetails(user).lastBroadcastAtMillis)
        assertNull(channelSearchUserDetails(org.json.JSONObject()).lastBroadcastAtMillis)
    }
}
