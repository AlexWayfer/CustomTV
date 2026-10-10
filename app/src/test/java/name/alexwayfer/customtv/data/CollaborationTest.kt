package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CollaborationTest {
    @Test
    fun aParticipantChannelListsTheLeaderFirstThenGuestsByTheirViewers() {
        val channels = parseCollaborationChannels(
            """
            {"data":{"channel":{"collaboration":{"collaborators":[
              {"role":"MEMBER","status":"ACTIVE","user":{"id":"guest","login":"quiet","displayName":"Quiet","profileImageURL":null,"stream":{"viewersCount":10}}},
              {"role":"LEADER","status":"ACTIVE","user":{"id":"host","login":"host","displayName":"Host","profileImageURL":"host.png","stream":{"viewersCount":4}}},
              {"role":"MEMBER","status":"ACTIVE","user":{"id":"loud","login":"loud","displayName":"Loud","profileImageURL":"loud.png","stream":{"viewersCount":50}}},
              {"role":"MEMBER","status":"INVITED","user":{"id":"wait","login":"wait","displayName":"Wait","stream":{"viewersCount":99}}}
            ]}}}}
            """.trimIndent(),
        )
        assertEquals(listOf("host", "loud", "guest"), channels?.map { it.id })
        assertEquals(50, channels?.get(1)?.viewerCount)
    }

    @Test
    fun theCurrentChannelIsBeforeTheLeaderAndATapOnItDoesNotOpenAnotherChannel() {
        val channels = parseCollaborationChannels(
            """
            {"data":{"channel":{"collaboration":{"collaborators":[
              {"role":"MEMBER","status":"ACTIVE","user":{"id":"guest","login":"quiet","displayName":"Quiet","stream":{"viewersCount":10}}},
              {"role":"LEADER","status":"ACTIVE","user":{"id":"host","login":"host","displayName":"Host","stream":{"viewersCount":4}}},
              {"role":"MEMBER","status":"ACTIVE","user":{"id":"loud","login":"loud","displayName":"Loud","stream":{"viewersCount":50}}}
            ]}}}}
            """.trimIndent(),
            currentLogin = "Quiet",
        )
        assertEquals(listOf("guest", "host", "loud"), channels?.map { it.id })
        assertEquals(false, collaborationRowOpensChannel("Quiet", "quiet"))
        assertEquals(true, collaborationRowOpensChannel("host", "quiet"))
    }

    @Test
    fun aChannelWithoutACollaborationIsAnEmptyList() {
        val channels = parseCollaborationChannels("""{"data":{"channel":{"collaboration":null}}}""")
        assertEquals(emptyList<CollaborationChannel>(), channels)
    }

    @Test
    fun aResponseWithoutAChannelDoesNotParse() {
        assertNull(parseCollaborationChannels("""{"errors":[{"message":"bad"}]}"""))
    }
}
