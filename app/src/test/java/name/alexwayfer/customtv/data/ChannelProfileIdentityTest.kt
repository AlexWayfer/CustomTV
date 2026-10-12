package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelProfileIdentityTest {
    private val identity = ChannelProfile(
        login = "streamer",
        displayName = "Streamer",
        avatarUrl = "https://avatar.test/streamer.png",
        id = "123",
        highlightColorHex = "#9146FF",
        channelPointsIconUrl = "https://points.test/icon.png",
    )

    @Test
    fun theStreamOfTheMomentIsDroppedAndWhoTheChannelIsKept() {
        val live = identity.copy(
            streamTitle = "Title",
            categoryName = "Just Chatting",
            streamStartedAtMillis = 1_000,
            currentStreamVideoId = "v1",
            viewerCount = 10,
            sharedViewerCount = 20,
            collaboratorAvatarUrls = listOf("https://avatar.test/other.png"),
            isLive = true,
            tags = listOf("English"),
            chatRules = listOf("Be nice"),
        )

        assertEquals(identity, live.identity())
    }

    @Test
    fun aProfileWithoutAStreamIsItsOwnIdentity() {
        assertEquals(identity, identity.identity())
    }
}
