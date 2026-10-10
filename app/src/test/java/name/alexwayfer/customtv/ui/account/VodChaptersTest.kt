package name.alexwayfer.customtv.ui.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

        assertEquals("Just Chatting", vodCategoryAt(categories, 0.0))
        assertEquals("Just Chatting", vodCategoryAt(categories, 119.9))
        assertEquals("Grand Theft Auto V", vodCategoryAt(categories, 120.0))
        assertEquals("Just Chatting", vodCategoryAt(categories, 500.0))
    }

    @Test
    fun aPositionBeforeTheFirstChapterStillUsesThatChapter() {
        val categories = VodCategories(
            chapters = listOf(VodChapter(8.0, "Art")),
            fallbackCategory = "Other",
        )

        assertEquals("Art", vodCategoryAt(categories, 0.0))
    }

    @Test
    fun anEmptyChapterListUsesTheVideosOwnCategory() {
        val categories = VodCategories(chapters = emptyList(), fallbackCategory = "Minecraft")

        assertEquals("Minecraft", vodCategoryAt(categories, 30.0))
        assertNull(vodCategoryAt(VodCategories(emptyList(), null), 30.0))
        assertNull(vodCategoryAt(VodCategories(emptyList(), "  "), 30.0))
    }

    @Test
    fun markersParseTheGameAtEachPosition() {
        val parsed = parseVodCategories(
            """
            {"data":{"video":{
              "game":{"displayName":"Just Chatting"},
              "moments":{"edges":[
                {"node":{"positionMilliseconds":0,"description":"Just Chatting","details":{"__typename":"GameChangeMomentDetails","game":{"displayName":"Just Chatting"}}}},
                {"node":{"positionMilliseconds":4447000,"description":"Grand Theft Auto V","details":{"__typename":"GameChangeMomentDetails","game":{"displayName":"Grand Theft Auto V"}}}}
              ]}
            }}}
            """.trimIndent(),
        )

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
    fun aMissingVideoDoesNotParse() {
        assertNull(parseVodCategories("""{"data":{"video":null}}"""))
    }
}
