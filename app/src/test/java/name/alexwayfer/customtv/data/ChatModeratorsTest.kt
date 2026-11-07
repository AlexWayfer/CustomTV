package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatModeratorsTest {
    @Test
    fun thePublicListDoesNotInventPresenceAndIgnoresVips() {
        assertEquals(
            listOf(ChatModerator("1", "alice", "Alice", null)),
            parsePublicChatModerators(
                """{"mods":[{"id":"1","login":"alice","displayName":"Alice"}],
                    "vips":[{"id":"2","login":"bob","displayName":"Bob"}],"ttl":1200}""",
            ),
        )
        assertEquals(emptyList<ChatModerator>(), parsePublicChatModerators("""{"mods":[]}"""))
        assertNull(parsePublicChatModerators("""{"message":"unavailable"}"""))
        assertNull(parsePublicChatModerators("""{"mods":[{"id":"1"}]}"""))
    }

    @Test
    fun moderatorsKeepTheirNamesAndThreePresenceStates() {
        val page = parseChatModeratorsPage(
            """{"data":{"user":{"mods":{"edges":[
                {"cursor":"a","isActive":true,"node":{"id":"1","login":"alice","displayName":"Alice"}},
                {"cursor":"b","isActive":false,"node":{"id":"2","login":"bob","displayName":"Боб"}},
                {"cursor":"c","isActive":null,"node":{"id":"3","login":"carol","displayName":""}}
            ],"pageInfo":{"hasNextPage":false}}}}}""",
        )!!

        assertEquals(
            listOf(
                ChatModerator("1", "alice", "Alice", true),
                ChatModerator("2", "bob", "Боб", false),
                ChatModerator("3", "carol", "carol", null),
            ),
            page.moderators,
        )
        assertNull(page.nextCursor)
    }

    @Test
    fun anEmptyCompleteListIsNotAFailedLoad() {
        assertEquals(
            ChatModeratorsPage(emptyList(), null),
            parseChatModeratorsPage(
                """{"data":{"user":{"mods":{"edges":[],"pageInfo":{"hasNextPage":false}}}}}""",
            ),
        )
        assertNull(parseChatModeratorsPage("""{"data":{"user":{"mods":null}}}"""))
        assertNull(parseChatModeratorsPage("not JSON"))
    }

    @Test
    fun theLastEdgePagesTheListAndMissingPaginationFails() {
        val body = """{"data":{"user":{"mods":{"edges":[
            {"cursor":"next","isActive":true,"node":{"id":"1","login":"alice","displayName":"Alice"}}
        ],"pageInfo":{"hasNextPage":true}}}}}"""

        assertEquals("next", parseChatModeratorsPage(body)!!.nextCursor)
        assertNull(parseChatModeratorsPage(body.replace("\"cursor\":\"next\",", "")))
        assertNull(parseChatModeratorsPage(body.replace("\"pageInfo\":{\"hasNextPage\":true}", "\"pageInfo\":{}")))
    }

    @Test
    fun aBrokenModeratorDoesNotMakeAnIncompleteListLookComplete() {
        assertNull(
            parseChatModeratorsPage(
                """{"data":{"user":{"mods":{"edges":[{"node":null}],"pageInfo":{"hasNextPage":false}}}}}""",
            ),
        )
    }

    @Test
    fun presentModeratorsComeFirstThenAbsentThenUnknownEachByLogin() {
        val alice = ChatModerator("1", "alice", "Alice", true)
        val bob = ChatModerator("2", "bob", "Боб", false)
        val carol = ChatModerator("3", "carol", "Carol", null)
        val dave = ChatModerator("4", "dave", "Dave", true)

        assertEquals(listOf(alice, dave, bob, carol), sortedChatModerators(listOf(carol, bob, dave, alice)))
    }
}
