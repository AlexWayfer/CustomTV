package name.alexwayfer.customtv.data

import androidx.compose.ui.graphics.Color
import name.alexwayfer.customtv.chat.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedChatSourcesTest {
    @Test
    fun collaboratorsNameTheSourcesByRoomId() {
        val sources = sharedChatSources(
            collaborators = listOf(collaborator("1", "Host", "host.png"), collaborator("2", "Guest", null)),
            profiles = emptyList(),
        )

        assertEquals(SharedChatSource("Host", "host.png"), sources["1"])
        assertEquals(SharedChatSource("Guest", null), sources["2"])
    }

    @Test
    fun aProfileLoadedByIdFillsAChannelTheCollaborationLacks() {
        val sources = sharedChatSources(
            collaborators = listOf(collaborator("1", "Host", "host.png")),
            profiles = listOf(
                ChannelProfile(login = "late", displayName = "Late", avatarUrl = "late.png", id = "3"),
                ChannelProfile(login = "host", displayName = "Old", avatarUrl = "old.png", id = "1"),
            ),
        )

        assertEquals(SharedChatSource("Late", "late.png"), sources["3"])
        assertEquals(SharedChatSource("Host", "host.png"), sources["1"])
    }

    @Test
    fun aProfileWithoutIdOrNameNamesNoSource() {
        val sources = sharedChatSources(
            collaborators = emptyList(),
            profiles = listOf(
                ChannelProfile(login = "noid", displayName = "No ID", avatarUrl = null),
                ChannelProfile(login = "", displayName = "", avatarUrl = "blank.png", id = "4"),
            ),
        )

        assertEquals(emptyMap<String, SharedChatSource>(), sources)
    }

    @Test
    fun nothingLoadsWhileTheCollaborationStillLoads() {
        assertEquals(
            emptySet<String>(),
            sharedChatSourcesToLoad(
                seen = setOf("3"),
                known = emptySet(),
                requested = emptySet(),
                collaborationLoading = true,
            ),
        )
    }

    @Test
    fun onlyUnknownSourcesNotAskedBeforeLoad() {
        assertEquals(
            setOf("4"),
            sharedChatSourcesToLoad(
                seen = setOf("1", "3", "4"),
                known = setOf("1"),
                requested = setOf("3"),
                collaborationLoading = false,
            ),
        )
    }

    @Test
    fun theCollaborationMayStillComeUntilTheChannelIsKnownAndWhileItStreamsTogether() {
        assertTrue(sharedChatCollaborationLoading(channelId = null, sharedViewerCount = null, collaboration = null))
        assertTrue(sharedChatCollaborationLoading(channelId = "1", sharedViewerCount = 500, collaboration = null))
        assertFalse(
            sharedChatCollaborationLoading(
                channelId = "1",
                sharedViewerCount = 500,
                collaboration = CollaborationResult.Unavailable,
            ),
        )
        assertFalse(sharedChatCollaborationLoading(channelId = "1", sharedViewerCount = null, collaboration = null))
    }

    @Test
    fun aCardOpenedFromAMessageShowsThatMessagesSource() {
        val history = listOf(message("a", 1, "222"), message("b", 2, "333"))

        assertEquals("222", chatterSharedChatSourceRoomId(message("a", 1, "222"), history))
        assertNull(chatterSharedChatSourceRoomId(message("own", 3, null), history))
    }

    @Test
    fun aCardOpenedWithoutAMessageShowsTheLatestMessagesSource() {
        assertEquals(
            "333",
            chatterSharedChatSourceRoomId(null, listOf(message("b", 2, "333"), message("a", 1, "222"))),
        )
        assertNull(chatterSharedChatSourceRoomId(null, listOf(message("a", 1, "222"), message("b", 2, null))))
        assertNull(chatterSharedChatSourceRoomId(null, emptyList()))
    }

    @Test
    fun theSourceStandsOnTheEventLineWhenTheRowHasOne() {
        assertFalse(sharedChatSourceOnUserMessage(systemText = "Viewer subscribed for 5 months"))
        assertTrue(sharedChatSourceOnUserMessage(systemText = null))
        assertTrue(sharedChatSourceOnUserMessage(systemText = " "))
    }

    private fun message(id: String, sentAt: Long, sourceRoomId: String?) = ChatMessage(
        id = id,
        userLogin = "viewer",
        displayName = "Viewer",
        color = Color.Unspecified,
        rawText = "hi",
        parts = emptyList(),
        timestampMillis = sentAt,
        sourceRoomId = sourceRoomId,
    )

    private fun collaborator(id: String, displayName: String, avatarUrl: String?) = CollaborationChannel(
        id = id,
        login = displayName.lowercase(),
        displayName = displayName,
        avatarUrl = avatarUrl,
        viewerCount = null,
    )
}
