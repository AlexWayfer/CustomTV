package name.alexwayfer.customtv.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatRulesTest {
    @Test
    fun rulesKeepTheChannelOrderAndDropBlankOnes() {
        assertEquals(
            listOf("English please", "Fresh memes"),
            parseChatRules(JSONObject("""{"chatSettings":{"rules":["English please"," ",null,"  Fresh memes "]}}""")),
        )
    }

    @Test
    fun aChannelWithoutRulesHasAnEmptyList() {
        assertEquals(emptyList<String>(), parseChatRules(JSONObject("""{"chatSettings":{"rules":[]}}""")))
        assertEquals(emptyList<String>(), parseChatRules(JSONObject("""{"chatSettings":{"rules":null}}""")))
    }

    @Test
    fun anAnswerWithoutChatSettingsLeavesTheRulesUnknown() {
        assertNull(parseChatRules(JSONObject("""{"login":"xqc"}""")))
        assertNull(parseChatRules(JSONObject("""{"chatSettings":null}""")))
    }

    @Test
    fun theChannelProfileCarriesItsRules() {
        val parsed = ChannelProfileParser.parse(
            """{"data":{"user":{"id":"1","login":"xqc","chatSettings":{"rules":["English please"]}}}}""",
            "xqc",
        )
        assertEquals(listOf("English please"), parsed?.profile?.chatRules)
    }

    @Test
    fun theFingerprintStaysForTheSameRulesAndChangesWithAnyEdit() {
        val rules = listOf("English please", "Fresh memes")
        assertEquals(chatRulesFingerprint(rules), chatRulesFingerprint(listOf("English please", "Fresh memes")))
        assertNotEquals(chatRulesFingerprint(rules), chatRulesFingerprint(listOf("English please", "Fresh memes!")))
        assertNotEquals(chatRulesFingerprint(rules), chatRulesFingerprint(rules.reversed()))
        assertNotEquals(chatRulesFingerprint(listOf("ab", "c")), chatRulesFingerprint(listOf("a", "bc")))
    }
}
