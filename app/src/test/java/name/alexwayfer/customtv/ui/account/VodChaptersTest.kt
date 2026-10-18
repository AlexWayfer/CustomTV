package name.alexwayfer.customtv.ui.account

import name.alexwayfer.customtv.data.TwitchCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VodChaptersTest {
    @Test
    fun theCategoryIsTheChapterCoveringThePlaybackPosition() {
        val categories = VodCategories(
            chapters = listOf(
                VodChapter(0.0, "Just Chatting"),
                VodChapter(120.0, "Grand Theft Auto V"),
                VodChapter(400.5, "Just Chatting"),
            ),
            fallbackCategory = "Old",
        )

        assertEquals("Just Chatting", vodChapterCategoryAt(categories, 0.0)?.name)
        assertEquals("Just Chatting", vodChapterCategoryAt(categories, 119.9)?.name)
        assertEquals("Grand Theft Auto V", vodChapterCategoryAt(categories, 120.0)?.name)
        assertEquals("Just Chatting", vodChapterCategoryAt(categories, 500.0)?.name)
    }

    @Test
    fun aPositionBeforeTheFirstChapterStillUsesThatChapter() {
        val categories = VodCategories(
            chapters = listOf(VodChapter(8.0, "Art")),
            fallbackCategory = "Other",
        )

        assertEquals("Art", vodChapterCategoryAt(categories, 0.0)?.name)
    }

    @Test
    fun anEmptyChapterListUsesTheVideosOwnCategory() {
        val categories = VodCategories(chapters = emptyList(), fallbackCategory = "Minecraft")

        assertEquals("Minecraft", vodChapterCategoryAt(categories, 30.0)?.name)
        assertNull(vodChapterCategoryAt(VodCategories(emptyList(), null), 30.0))
        assertNull(vodChapterCategoryAt(VodCategories(emptyList(), "  "), 30.0))
    }

    @Test
    fun theCategoryAtAPositionCarriesItsChaptersIdAndBoxArt() {
        val categories = VodCategories(
            chapters = listOf(
                VodChapter(0.0, "Just Chatting", "jc.jpg", "509658"),
                VodChapter(120.0, "Chapter 2"),
            ),
            fallbackCategory = "Old",
        )

        assertEquals(TwitchCategory("Just Chatting", "jc.jpg", "509658"), vodChapterCategoryAt(categories, 60.0))
        // A chapter without a game has no ID to open.
        assertEquals(TwitchCategory("Chapter 2", null, null), vodChapterCategoryAt(categories, 130.0))
    }

    @Test
    fun withoutChaptersTheCategoryIsTheVideosOwnWithItsId() {
        val categories = VodCategories(emptyList(), "Minecraft", "mc.jpg", "27471")

        assertEquals(TwitchCategory("Minecraft", "mc.jpg", "27471"), vodChapterCategoryAt(categories, 30.0))
    }

    @Test
    fun markersParseTheGameAtEachPosition() {
        val parsed = parseVodCategoriesBatch(
            """
            {"data":{"v0":{
              "game":{"displayName":"Just Chatting"},
              "moments":{"edges":[
                {"node":{
                  "positionMilliseconds":0,
                  "description":"Just Chatting",
                  "details":{"__typename":"GameChangeMomentDetails","game":{"displayName":"Just Chatting"}}
                }},
                {"node":{
                  "positionMilliseconds":4447000,
                  "description":"Grand Theft Auto V",
                  "details":{"__typename":"GameChangeMomentDetails","game":{"displayName":"Grand Theft Auto V"}}
                }}
              ]}
            }}}
            """.trimIndent(),
            listOf("42"),
        )["42"]

        assertEquals("Just Chatting", parsed?.fallbackCategory)
        assertEquals(
            listOf(
                VodChapter(0.0, "Just Chatting"),
                VodChapter(4447.0, "Grand Theft Auto V"),
            ),
            parsed?.chapters,
        )
    }

    @Test
    fun aChapterAndTheRecordingKeepTheirCategorysId() {
        val parsed = parseVodCategoriesBatch(
            """
            {"data":{"v0":{
              "game":{"id":"509658","displayName":"Just Chatting"},
              "moments":{"edges":[{"node":{"positionMilliseconds":0,"description":"GTA V",
                "details":{"__typename":"GameChangeMomentDetails","game":{"id":"32982","displayName":"GTA V"}}}}]}
            }}}
            """.trimIndent(),
            listOf("42"),
        )["42"]

        assertEquals("509658", parsed?.fallbackCategoryId)
        assertEquals("32982", parsed?.chapters?.single()?.categoryId)
    }

    @Test
    fun aMissingVideoDoesNotParse() {
        val parsed = parseVodCategoriesBatch("""{"data":{"v0":null}}""", listOf("1"))

        assertEquals(emptyMap<String, VodCategories>(), parsed)
    }

    @Test
    fun aRecordingWithoutChaptersHasAnEmptyListNotAFailure() {
        val parsed = parseVodCategoriesBatch(
            """{"data":{"v0":{"game":{"displayName":"Art"},"moments":{"edges":[]}}}}""",
            listOf("1"),
        )

        assertEquals(mapOf("1" to VodCategories(emptyList(), "Art")), parsed)
    }

    @Test
    fun chaptersAServerFailureLeftOutAreUnknownNotEmpty() {
        val parsed = parseVodCategoriesBatch(
            """
            {"errors":[{"message":"service error","path":["v1","moments"]}],
             "data":{
               "v0":{"game":{"displayName":"Art"},"moments":{"edges":[]}},
               "v1":{"game":{"displayName":"Art"},"moments":null},
               "v2":{"game":{"displayName":"Art"}}
             }}
            """.trimIndent(),
            listOf("10", "11", "12"),
        )

        assertEquals(setOf("10"), parsed.keys)
    }

    @Test
    fun anErrorInsideARecordingLeavesItOutEvenWithItsChapterList() {
        val parsed = parseVodCategoriesBatch(
            """
            {"errors":[{"message":"service error","path":["v0","game"]}],
             "data":{"v0":{"game":null,"moments":{"edges":[]}}}}
            """.trimIndent(),
            listOf("10"),
        )

        assertEquals(emptyMap<String, VodCategories>(), parsed)
    }

    @Test
    fun queryAsksEachRecordingByItsOwnVariable() {
        val query = vodChaptersQuery(2)

        assertTrue(query, query.startsWith($$"query($v0:ID!,$v1:ID!){v0:video(id:$v0){game{"))
        assertTrue(query, query.contains($$" v1:video(id:$v1){game{"))
    }
}
