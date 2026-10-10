package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import name.alexwayfer.customtv.data.parseDisplayedChatBadges
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OwnChatBadgeTest {
    @Test
    fun theNewestOwnMessageCarriesTheCurrentBadges() {
        val messages = listOf(chat("1", "alex"), chat("2", "other"), chat("3", "Alex"), chat("4", "other"))
        assertEquals("3", latestOwnChatMessage(messages, "alex")?.id)
    }

    @Test
    fun noOwnMessageOrNoLoginFindsNothing() {
        assertNull(latestOwnChatMessage(listOf(chat("1", "other")), "alex"))
        assertNull(latestOwnChatMessage(listOf(chat("1", "alex")), ""))
    }

    @Test
    fun displayBadgesParseWithTheirImages() {
        val body = """{"data":{"user":{"displayBadges":[{"setID":"moderator","version":"1","imageURL":"https://b/mod"},{"setID":"subscriber","version":"12","imageURL":null}]}}}"""
        val parsed = parseDisplayedChatBadges(body)!!
        assertEquals(listOf("moderator/1", "subscriber/12"), parsed.badges.map(ChatBadge::key))
        assertEquals(mapOf("moderator/1" to "https://b/mod"), parsed.imageUrls)
    }

    @Test
    fun aMissingUserParsesAsNoAnswer() = assertNull(parseDisplayedChatBadges("""{"data":{"user":null}}"""))

    @Test
    fun anOwnMessageWrittenAfterTheLoadReplacesTheLoadedBadges() {
        val written = chat("1", "alex").copy(badges = badges("subscriber/1"), timestampMillis = 2_000)
        assertEquals(badges("subscriber/1"), ownChatBadgesNow(badges("premium/1"), 1_000, written))
    }

    @Test
    fun anOwnMessageFromBeforeTheLoadKeepsTheLoadedBadges() {
        val history = chat("1", "alex").copy(badges = emptyList(), timestampMillis = 500)
        assertEquals(badges("premium/1"), ownChatBadgesNow(badges("premium/1"), 1_000, history))
    }

    @Test
    fun withoutALoadedAnswerTheNewestOwnMessageGivesTheBadges() {
        val history = chat("1", "alex").copy(badges = badges("vip/1"), timestampMillis = 500)
        assertEquals(badges("vip/1"), ownChatBadgesNow(null, 0, history))
        assertNull(ownChatBadgesNow(null, 0, null))
    }

    private fun badges(vararg keys: String) = keys.map { key -> key.split("/").let { ChatBadge(it[0], it[1]) } }

    private fun chat(id: String, login: String) = ChatMessage(
        id = id,
        userLogin = login,
        displayName = login,
        color = Color.White,
        rawText = "hi",
        parts = listOf(ChatPart.Text("hi")),
        timestampMillis = 1L,
    )
}
