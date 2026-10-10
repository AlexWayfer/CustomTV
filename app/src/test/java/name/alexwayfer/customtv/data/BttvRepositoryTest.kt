package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BttvRepositoryTest {
    @Test
    fun parseGlobalSkipsModifiersKeepsWideAspectAndZeroWidthOverlays() {
        val emotes = BttvRepository.parseGlobal(
            """
            [
              {
                "id": "54fa8f1401e468494b85b537",
                "code": ":tf:",
                "imageType": "png",
                "animated": false,
                "modifier": false
              },
              {
                "id": "566ca38765dbbdab32ec0560",
                "code": "AngelThump",
                "imageType": "png",
                "animated": false,
                "modifier": false,
                "width": 84,
                "height": 28
              },
              {
                "id": "5e76d399d6581c3724c0f0b8",
                "code": "cvMask",
                "imageType": "png",
                "animated": false,
                "modifier": false
              },
              {
                "id": "6468f7acaee1f7f47567708e",
                "code": "c!",
                "imageType": "png",
                "animated": false,
                "modifier": true
              }
            ]
            """.trimIndent(),
        )
        assertEquals(
            "https://cdn.betterttv.net/emote/54fa8f1401e468494b85b537/2x.webp",
            emotes.getValue(":tf:").url,
        )
        assertFalse(emotes.getValue(":tf:").overlay)
        assertEquals(84f / 28f, emotes.getValue("AngelThump").aspectRatio, 0.001f)
        assertTrue(emotes.getValue("cvMask").overlay)
        assertNull(emotes["c!"])
    }

    @Test
    fun parseChannelLetsChannelEmotesOverrideShared() {
        val emotes = BttvRepository.parseChannel(
            """
            {
              "channelEmotes": [
                {
                  "id": "channel-sadge",
                  "code": "Sadge",
                  "imageType": "png",
                  "animated": false
                }
              ],
              "sharedEmotes": [
                {
                  "id": "shared-sadge",
                  "code": "Sadge",
                  "imageType": "png",
                  "animated": false
                },
                {
                  "id": "54fa92ee01e468494b85b553",
                  "code": "RebeccaBlack",
                  "imageType": "png",
                  "animated": false
                }
              ]
            }
            """.trimIndent(),
        )
        assertEquals(
            "https://cdn.betterttv.net/emote/channel-sadge/2x.webp",
            emotes.getValue("Sadge").url,
        )
        assertEquals(
            "https://cdn.betterttv.net/emote/54fa92ee01e468494b85b553/2x.webp",
            emotes.getValue("RebeccaBlack").url,
        )
    }
}
