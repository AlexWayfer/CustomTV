package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.chat.ChatBadge
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatterModerationLockTest {
    private val moderator = listOf(ChatBadge("moderator", "1"))

    @Test
    fun aModeratorSeesModerationLockedOnAChatter() {
        assertTrue(chatterCardModerationLocked(moderator, broadcasterId = "10", targetUserId = "20"))
    }

    @Test
    fun theBroadcasterSeesModerationLockedOnAChatter() {
        assertTrue(chatterCardModerationLocked(listOf(ChatBadge("broadcaster", "1")), "10", "20"))
    }

    @Test
    fun aLeadModeratorSeesModerationLocked() {
        assertTrue(chatterCardModerationLocked(listOf(ChatBadge("lead_moderator", "1")), "10", "20"))
    }

    @Test
    fun aViewerSeesNoModeration() {
        assertFalse(chatterCardModerationLocked(listOf(ChatBadge("subscriber", "12")), "10", "20"))
    }

    @Test
    fun unknownOwnBadgesShowNoModeration() {
        assertFalse(chatterCardModerationLocked(null, "10", "20"))
    }

    @Test
    fun theBroadcastersCardHasNoModeration() {
        assertFalse(chatterCardModerationLocked(moderator, broadcasterId = "10", targetUserId = "10"))
    }

    @Test
    fun aChatterWithoutAnIdHasNoModeration() {
        assertFalse(chatterCardModerationLocked(moderator, broadcasterId = "10", targetUserId = null))
    }
}
