package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.chat.TwitchEmoteGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.json.JSONArray

class TwitchEmoteCatalogTest {
    @Test
    fun channelEmotesKeepFollowAndSubscriptionsAndUseAnAnimatedUrlWhenOffered() {
        val parsed = parseHelixEmotes(
            """
            {"data":[
              {"id":"follow1","name":"QuinLove","emote_type":"follower","format":["static"]},
              {"id":"sub1","name":"QuinSub","emote_type":"subscriptions","format":["static","animated"]},
              {"id":"bits1","name":"QuinBits","emote_type":"bitstier","format":["static"]}
            ]}
            """.trimIndent(),
            global = false,
        )

        assertEquals(
            listOf(
                "QuinLove" to TwitchEmoteGroup.Follower,
                "QuinSub" to TwitchEmoteGroup.Subscriptions,
            ),
            parsed?.map { it.name to it.group },
        )
        assertEquals(
            "https://static-cdn.jtvnw.net/emoticons/v2/follow1/static/dark/2.0",
            parsed?.get(0)?.url,
        )
        assertEquals(
            "https://static-cdn.jtvnw.net/emoticons/v2/sub1/animated/dark/2.0",
            parsed?.get(1)?.url,
        )
    }

    @Test
    fun globalEmotesIgnoreTheChannelType() {
        val parsed = parseHelixEmotes(
            """{"data":[{"id":"k","name":"Kappa","emote_type":"globals","format":["static"]}]}""",
            global = true,
        )
        assertEquals(TwitchEmoteGroup.Global, parsed?.single()?.group)
        assertNull(parseHelixEmotes("""{"errors":[{"message":"no"}]}""", global = false))
    }

    @Test
    fun userEmotesKeepSmiliesAndTurboAndReadTheNextPage() {
        val parsed = parseUserEmotePage(
            """
            {"data":[
              {"id":"t","name":":)","emote_type":"turbo","format":["static"]},
              {"id":"s","name":":D","emote_type":"smilies","format":["static"]},
              {"id":"p","name":":P","emote_type":"prime","format":["static"]},
              {"id":"sub","name":"Sub","emote_type":"subscriptions","format":["static"]},
              {"id":"g","name":"Kappa","emote_type":"globals","format":["static"]},
              {"id":"w","name":":)","emote_type":"globals","format":["static"]},
              {"id":"pride","name":"PrideCute","emote_type":"limitedtime","format":["static"]}
            ],"pagination":{"cursor":"next"}}
            """.trimIndent(),
        )

        assertEquals(listOf(":D", ":P", ":)"), parsed?.smilies?.map { it.name })
        assertEquals(
            mapOf("turbo" to 1, "smilies" to 1, "prime" to 1, "subscriptions" to 1, "globals" to 2, "limitedtime" to 1),
            parsed?.typeCounts,
        )
        assertEquals(listOf("PrideCute"), parsed?.unlocked?.map { it.name })
        assertEquals(listOf(":)"), parsed?.turbo?.map { it.name })
        assertEquals("next", parsed?.cursor)
        assertEquals(
            "https://static-cdn.jtvnw.net/emoticons/v2/t/static/dark/2.0",
            parsed?.turbo?.single()?.url,
        )
        assertNull(parseUserEmotePage("""{"errors":[{"message":"no"}]}"""))
    }

    @Test
    fun storedUserEmotesJoinTheKeptPagesInOrder() {
        val first = """{"data":[{"id":"1","name":"Kappa","emote_type":"turbo","format":["static"]}],"pagination":{"cursor":"a"}}"""
        val second = """{"data":[{"id":"2","name":"Kreygasm","emote_type":"turbo","format":["animated"]}],"pagination":{}}"""
        val body = JSONArray().put(first).put(second).toString()

        val emotes = parseStoredUserEmotes(body)!!

        assertEquals(listOf("Kappa", "Kreygasm"), emotes.turbo.map { it.name })
        assertEquals(
            "https://static-cdn.jtvnw.net/emoticons/v2/2/animated/dark/2.0",
            emotes.turbo[1].url,
        )
    }

    @Test
    fun storedUserEmotesWithABrokenPageAreDropped() {
        val body = JSONArray().put("""{"data":[]}""").put("not json").toString()

        assertNull(parseStoredUserEmotes(body))
    }
}
