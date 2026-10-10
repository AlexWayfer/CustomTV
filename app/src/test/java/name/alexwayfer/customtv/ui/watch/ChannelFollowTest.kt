package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.chat.FollowForChatMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelFollowTest {
    @Test
    fun aFollowInFollowersOnlyChatCountsFromNow() {
        assertEquals(
            FollowForChatMode.Following(1_000L),
            chatModeFollowAfterChange(followersOnly = true, following = true, nowMillis = 1_000L),
        )
    }

    @Test
    fun anUnfollowInFollowersOnlyChatNeedsTheFollowAgain() {
        assertEquals(
            FollowForChatMode.NotFollowing,
            chatModeFollowAfterChange(followersOnly = true, following = false, nowMillis = 1_000L),
        )
    }

    @Test
    fun outsideFollowersOnlyChatTheFollowStaysUnknown() {
        assertEquals(
            FollowForChatMode.Unknown,
            chatModeFollowAfterChange(followersOnly = false, following = true, nowMillis = 1_000L),
        )
    }
}
