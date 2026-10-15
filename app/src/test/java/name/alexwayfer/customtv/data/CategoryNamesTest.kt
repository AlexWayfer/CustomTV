package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryNamesTest {
    @Test
    fun theNamesAreAskedOneAliasAndOneVariableEach() {
        val payload = categoryNamesPayload(listOf("509658", "21779"))
        assertEquals(
            $$"query($c0:ID!,$c1:ID!){c0:game(id:$c0){displayName} c1:game(id:$c1){displayName}}",
            payload.getString("query"),
        )
        val variables = payload.getJSONObject("variables")
        assertEquals("509658", variables.getString("c0"))
        assertEquals("21779", variables.getString("c1"))
    }

    @Test
    fun theAnswerNamesEachCategoryByItsId() {
        assertEquals(
            mapOf("509658" to "Общение", "21779" to "League of Legends"),
            parseCategoryNames(
                """{"data":{"c0":{"displayName":"Общение"},"c1":{"displayName":"League of Legends"},"c2":null}}""",
                listOf("509658", "21779", "999"),
            ),
        )
    }

    @Test
    fun anAnswerWithoutDataNamesNothing() {
        assertEquals(emptyMap<String, String>(), parseCategoryNames("""{"errors":[{"message":"x"}]}""", listOf("1")))
        assertEquals(emptyMap<String, String>(), parseCategoryNames("not json", listOf("1")))
    }

    @Test
    fun onlyNumericIdsAreAskedEachOnce() {
        assertEquals(listOf("509658", "21779"), categoryIdsToAsk(listOf(" 509658", "21779", "509658", "", "x\"){")))
    }

    @Test
    fun aLiveStreamTakesItsCategorysNameInTheAppsLanguage() {
        val live = mapOf(
            "1" to FollowedLive("Just Chatting", viewerCount = 5, categoryId = "509658"),
            "2" to FollowedLive("Art", viewerCount = 3, categoryId = "509660"),
            "3" to FollowedLive(null, viewerCount = 1),
        )
        val localized = localizedFollowedLive(live, mapOf("509658" to "Общение"))
        assertEquals("Общение", localized.getValue("1").categoryName)
        assertEquals("Art", localized.getValue("2").categoryName)
        assertNull(localized.getValue("3").categoryName)
    }

    @Test
    fun aHelixStreamKeepsItsCategoryId() {
        val live = parseFollowedLive(
            """{"data":[{"type":"live","user_id":"1","game_id":"509658","game_name":"Just Chatting",
                "viewer_count":5,"title":"Hi"}]}""",
        )!!
        assertEquals("509658", live.getValue("1").categoryId)
    }

    @Test
    fun aRequestCarriesTheAppsLanguage() {
        assertEquals("ru", twitchGqlRequest("{}", language = "ru").build().header(ACCEPT_LANGUAGE))
    }

    @Test
    fun beforeTheAppStartsTheLanguageIsEnglish() {
        assertEquals("en-US", TwitchLanguage.current())
    }
}
