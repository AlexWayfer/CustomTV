package name.alexwayfer.customtv.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TwitchEmoteDetailsRepositoryTest {
    @Test
    fun cachesTheChannelSubSetAndGlobalsFromOneResponse() {
        val parsed = TwitchEmoteDetailsRepository.parse(
            JSONObject(
                """
                {
                  "data": {
                    "emote": {
                      "id": "slam",
                      "type": "SUBSCRIPTIONS",
                      "subscriptionTier": "TIER_1",
                      "owner": {
                        "displayName": "xQc",
                        "channel": {
                          "localEmoteSets": [
                            {
                              "emotes": [
                                {"id": "follow", "type": "FOLLOWER", "subscriptionTier": null}
                              ]
                            }
                          ]
                        },
                        "subscriptionProducts": [
                          {
                            "emotes": [
                              {"id": "slam", "type": "SUBSCRIPTIONS", "subscriptionTier": "TIER_1"},
                              {"id": "cheer", "type": "SUBSCRIPTIONS", "subscriptionTier": "TIER_1"}
                            ]
                          },
                          {
                            "emotes": [
                              {"id": "tier3", "type": "SUBSCRIPTIONS", "subscriptionTier": "TIER_3"}
                            ]
                          }
                        ]
                      }
                    },
                    "emoteSet": {
                      "emotes": [
                        {"id": "25", "type": "GLOBALS"},
                        {"id": "slam", "type": "GLOBALS"}
                      ]
                    }
                  }
                }
                """.trimIndent(),
            ),
        )

        assertEquals(TwitchEmoteDetails("SUBSCRIPTIONS", "TIER_1", "xQc"), parsed["slam"])
        assertEquals(TwitchEmoteDetails("SUBSCRIPTIONS", "TIER_1", "xQc"), parsed["cheer"])
        assertEquals(TwitchEmoteDetails("SUBSCRIPTIONS", "TIER_3", "xQc"), parsed["tier3"])
        assertEquals(TwitchEmoteDetails("FOLLOWER", null, "xQc"), parsed["follow"])
        assertEquals(TwitchEmoteDetails("GLOBALS", null, null), parsed["25"])
    }

    @Test
    fun emoteQueryKeepsTheGlobalSetInsideTheOperation() {
        listOf(true, false).forEach { includeGlobals ->
            val query = TwitchEmoteDetailsRepository.emoteDetailsQuery(includeGlobals)
            var depth = 0
            query.forEach { char ->
                if (char == '{') depth++
                if (char == '}') depth--
                assertTrue(depth >= 0)
            }
            assertEquals(0, depth)
        }
        val withGlobals = TwitchEmoteDetailsRepository.emoteDetailsQuery(includeGlobals = true)
        assertTrue(withGlobals.endsWith("emoteSet(id:\"0\"){emotes{id type}}}"))
    }

    @Test
    fun dropsAMissingEmote() {
        val parsed = TwitchEmoteDetailsRepository.parse(JSONObject("""{"data":{"emote":null}}"""))
        assertTrue(parsed.isEmpty())
        assertNull(parsed["missing"])
    }
}
