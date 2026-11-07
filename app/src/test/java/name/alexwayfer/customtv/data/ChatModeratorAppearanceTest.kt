package name.alexwayfer.customtv.data

import androidx.compose.ui.graphics.Color
import name.alexwayfer.customtv.chat.ChatBadge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class ChatModeratorAppearanceTest {
    @Test
    fun theModeratorListCanCarryColorsAndBadgesInTheSameResponse() {
        val page = parseChatModeratorsPage(
            """{"data":{"user":{"mods":{"edges":[{"isActive":true,"node":{
                "id":"2","login":"alice","displayName":"Alice","chatColor":"#123456",
                "displayBadges":[{"setID":"moderator","version":"1","imageURL":"mod.png"}]
            }}],"pageInfo":{"hasNextPage":false}}}}}""",
        )!!
        val appearance = page.moderators.single().appearance

        assertNotNull(appearance)
        assertEquals(Color(0xFF123456), appearance!!.color)
        assertEquals("mod.png", appearance.badges.imageUrls["moderator/1"])
    }

    @Test
    fun colorsAndChannelBadgesAreMatchedByUserId() {
        val parsed = parseModeratorAppearances(
            """{"data":{"users":[
                {"id":"2","chatColor":"#123456","displayBadges":[
                    {"setID":"moderator","version":"1","imageURL":"mod.png"}
                ]},
                null,
                {"id":"1","chatColor":null,"displayBadges":[]}
            ]}}""",
        )!!

        assertEquals(Color(0xFF123456), parsed.getValue("2").color)
        assertEquals(listOf(ChatBadge("moderator", "1")), parsed.getValue("2").badges.badges)
        assertEquals("mod.png", parsed.getValue("2").badges.imageUrls["moderator/1"])
        assertEquals(Color.Unspecified, parsed.getValue("1").color)
        assertEquals(emptyList<ChatBadge>(), parsed.getValue("1").badges.badges)
    }

    @Test
    fun aMissingAnswerStaysDifferentFromAnEmptyList() {
        assertNull(parseModeratorAppearances("""{"data":null}"""))
        assertNull(parseModeratorAppearances("invalid"))
        assertEquals(
            emptyMap<String, ChatModeratorAppearance>(), parseModeratorAppearances("""{"data":{"users":[]}}"""),
        )
    }
}
