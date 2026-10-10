package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class FfzRepositoryTest {
    @Test
    fun parseRoomBadgesMapsCustomModeratorAndVipToTwitchKeysAtScaleTwo() {
        val badges = FfzRepository.parseRoomBadges(
            """
            {
              "room": {
                "moderator_badge": "https://cdn.frankerfacez.com/room-badge/mod/id/1/v/a/1",
                "mod_urls": {
                  "1": "https://cdn.frankerfacez.com/room-badge/mod/id/1/v/a/1",
                  "2": "https://cdn.frankerfacez.com/room-badge/mod/id/1/v/a/2"
                },
                "vip_badge": {
                  "1": "https://cdn.frankerfacez.com/room-badge/vip/id/1/v/b/1",
                  "2": "https://cdn.frankerfacez.com/room-badge/vip/id/1/v/b/2"
                }
              }
            }
            """.trimIndent(),
        )

        assertEquals(
            mapOf(
                "moderator/1" to "https://cdn.frankerfacez.com/room-badge/mod/id/1/v/a/2",
                "vip/1" to "https://cdn.frankerfacez.com/room-badge/vip/id/1/v/b/2",
            ),
            badges,
        )
    }

    @Test
    fun parseRoomBadgesFallsBackToSingleModeratorUrl() {
        val badges = FfzRepository.parseRoomBadges(
            """{"room": {"moderator_badge": "https://cdn.frankerfacez.com/room-badge/mod/id/1/v/a/1", "mod_urls": null, "vip_badge": null}}""",
        )

        assertEquals(mapOf("moderator/1" to "https://cdn.frankerfacez.com/room-badge/mod/id/1/v/a/1"), badges)
    }

    @Test
    fun parseRoomBadgesWithoutCustomBadgesIsEmpty() {
        val badges = FfzRepository.parseRoomBadges(
            """{"room": {"moderator_badge": null, "mod_urls": null, "vip_badge": null}}""",
        )

        assertEquals(emptyMap<String, String>(), badges)
    }

    @Test
    fun parseGlobalUsesDefaultSetsSkipsHiddenAndPrefersAnimatedScale() {
        val emotes = FfzRepository.parseGlobal(
            """
            {
              "default_sets": [3],
              "sets": {
                "3": {
                  "emoticons": [
                    {
                      "name": "ZrehplaR",
                      "hidden": true,
                      "modifier": false,
                      "width": 32,
                      "height": 32,
                      "urls": { "1": "https://cdn.frankerfacez.com/emote/hidden/1" }
                    },
                    {
                      "name": "OMEGALUL",
                      "hidden": false,
                      "modifier": false,
                      "width": 32,
                      "height": 32,
                      "urls": {
                        "1": "https://cdn.frankerfacez.com/emote/omega/1",
                        "2": "https://cdn.frankerfacez.com/emote/omega/2"
                      }
                    },
                    {
                      "name": "ffzW",
                      "hidden": true,
                      "modifier": true,
                      "modifier_flags": 9,
                      "width": 32,
                      "height": 32,
                      "animated": {
                        "2": "https://cdn.frankerfacez.com/emote/ffzw/animated/2"
                      },
                      "urls": { "2": "https://cdn.frankerfacez.com/emote/ffzw/2" }
                    }
                  ]
                },
                "99": {
                  "emoticons": [
                    {
                      "name": "notInDefault",
                      "hidden": false,
                      "modifier": false,
                      "width": 32,
                      "height": 32,
                      "urls": { "1": "https://cdn.frankerfacez.com/emote/other/1" }
                    }
                  ]
                }
              }
            }
            """.trimIndent(),
        )
        assertNull(emotes["ZrehplaR"])
        assertNull(emotes["notInDefault"])
        assertEquals("https://cdn.frankerfacez.com/emote/omega/2", emotes.getValue("OMEGALUL").url)
        assertFalse(emotes.getValue("OMEGALUL").overlay)
        assertEquals(
            "https://cdn.frankerfacez.com/emote/ffzw/animated/2",
            emotes.getValue("ffzW").url,
        )
        assertFalse(emotes.getValue("ffzW").overlay)
        assertEquals(2f, emotes.getValue("ffzW").effects!!.widthMultiplier, 0.001f)
    }

    @Test
    fun parseRoomKeepsWideAspectAndAllSets() {
        val emotes = FfzRepository.parseRoom(
            """
            {
              "room": { "set": 85287 },
              "sets": {
                "85287": {
                  "emoticons": [
                    {
                      "name": "forsenWall",
                      "hidden": false,
                      "modifier": false,
                      "width": 25,
                      "height": 32,
                      "urls": {
                        "1": "//cdn.frankerfacez.com/emote/290052/1",
                        "2": "//cdn.frankerfacez.com/emote/290052/2"
                      }
                    }
                  ]
                }
              }
            }
            """.trimIndent(),
        )
        val emote = emotes.getValue("forsenWall")
        assertEquals("https://cdn.frankerfacez.com/emote/290052/2", emote.url)
        assertEquals(25f / 32f, emote.aspectRatio, 0.001f)
        assertFalse(emote.overlay)
    }
}
