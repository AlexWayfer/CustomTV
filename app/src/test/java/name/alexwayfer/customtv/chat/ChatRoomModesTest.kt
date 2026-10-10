package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRoomModesTest {
    @Test
    fun roomStateSetsFollowersSubscribersEmotesAndSlow() {
        val modes = chatRoomModesAfter(
            ChatRoomModes(),
            mapOf(
                "followers-only" to "10",
                "subs-only" to "1",
                "emote-only" to "1",
                "slow" to "30",
            ),
        )
        assertEquals(10, modes.followersOnlyMinutes)
        assertTrue(modes.subscribersOnly)
        assertTrue(modes.emoteOnly)
        assertEquals(30, modes.slowSeconds)
    }

    @Test
    fun partialRoomStateKeepsModesTheMessageDoesNotMention() {
        val current = ChatRoomModes(
            followersOnlyMinutes = 10,
            subscribersOnly = true,
            emoteOnly = true,
            slowSeconds = 30,
        )
        val modes = chatRoomModesAfter(current, mapOf("slow" to "0"))
        assertEquals(10, modes.followersOnlyMinutes)
        assertTrue(modes.subscribersOnly)
        assertTrue(modes.emoteOnly)
        assertNull(modes.slowSeconds)
    }

    @Test
    fun reconnectingTheSameChannelKeepsRoomModes() {
        val current = ChatRoomModes(followersOnlyMinutes = 10, slowSeconds = 30)
        assertEquals(current, roomModesAfterConnect(current, sameChannel = true))
        assertEquals(ChatRoomModes(), roomModesAfterConnect(current, sameChannel = false))
    }

    @Test
    fun followersOnlyTurnsOffAtMinusOne() {
        val modes = chatRoomModesAfter(
            ChatRoomModes(followersOnlyMinutes = 10),
            mapOf("followers-only" to "-1"),
        )
        assertNull(modes.followersOnlyMinutes)
    }

    @Test
    fun notFollowingShowsFollowersPlaque() {
        val plaques = plaques(followersOnlyMinutes = 0, follow = FollowForChatMode.NotFollowing)
        assertEquals(listOf(ChatModePlaque.FollowersNeedFollow("Segall")), plaques)
    }

    @Test
    fun unknownFollowHidesFollowersPlaque() {
        val plaques = plaques(followersOnlyMinutes = 10, follow = FollowForChatMode.Unknown)
        assertTrue(plaques.isEmpty())
    }

    @Test
    fun followingForTenMinutesWithFiveElapsedShowsFiveRemaining() {
        val followedAt = 1_000_000L
        val plaques = plaques(
            followersOnlyMinutes = 10,
            follow = FollowForChatMode.Following(followedAt),
            nowMillis = followedAt + 5 * 60_000L,
        )
        assertEquals(listOf(ChatModePlaque.FollowersWait(5 * 60_000L)), plaques)
    }

    @Test
    fun followingLongEnoughHidesFollowersPlaque() {
        val followedAt = 1_000_000L
        val plaques = plaques(
            followersOnlyMinutes = 10,
            follow = FollowForChatMode.Following(followedAt),
            nowMillis = followedAt + 10 * 60_000L,
        )
        assertTrue(plaques.isEmpty())
    }

    @Test
    fun anyFollowerCanChatWhenFollowersOnlyIsZero() {
        val plaques = plaques(
            followersOnlyMinutes = 0,
            follow = FollowForChatMode.Following(1L),
            nowMillis = 1L,
        )
        assertTrue(plaques.isEmpty())
    }

    @Test
    fun broadcasterSeesRoomModesWithoutAPersonalLimit() {
        val plaques = plaques(
            followersOnlyMinutes = 10,
            subscribersOnly = true,
            emoteOnly = true,
            slowSeconds = 10,
            follow = FollowForChatMode.NotFollowing,
            viewerIsBroadcaster = true,
            slowDeadlineMillis = 2_000L,
            nowMillis = 1_000L,
        )
        val slow = plaques.last() as ChatModePlaque.Slow
        assertEquals(
            listOf(
                ChatModePlaque.FollowersRoom,
                ChatModePlaque.Subscribers(offerSubscribe = false),
                ChatModePlaque.Emotes,
                slow,
            ),
            plaques,
        )
        assertEquals(10, slow.seconds)
        assertNull(slow.remainingMillis)
        assertFalse(slowModeBlocksSend(slow, awaitingOwnEcho = false))
    }

    @Test
    fun enabledModesStackFollowersThenSubscribersEmotesAndSlow() {
        val plaques = plaques(
            followersOnlyMinutes = 0,
            subscribersOnly = true,
            emoteOnly = true,
            slowSeconds = 30,
            follow = FollowForChatMode.NotFollowing,
            subscription = SubForChatMode.NotSubscribed,
        )
        assertEquals(
            listOf(
                ChatModePlaque.FollowersNeedFollow("Segall"),
                ChatModePlaque.Subscribers(offerSubscribe = true),
                ChatModePlaque.Emotes,
                ChatModePlaque.Slow(30, null),
            ),
            plaques,
        )
    }

    @Test
    fun subscribedViewerHidesSubscribersPlaque() {
        val plaques = plaques(subscribersOnly = true, subscription = SubForChatMode.Subscribed)
        assertTrue(plaques.isEmpty())
    }

    @Test
    fun unknownSubscriptionHidesSubscribersPlaque() {
        val plaques = plaques(subscribersOnly = true, subscription = SubForChatMode.Unknown)
        assertTrue(plaques.isEmpty())
    }

    @Test
    fun signedOutFollowOpensLoginAndSignedInFollowOpensTheChannel() {
        assertEquals(TwitchPlaqueAction.LogIn, followersPlaqueAction(signedIn = false, channelLogin = "Segall"))
        assertEquals(
            TwitchPlaqueAction.Open("https://www.twitch.tv/segall"),
            followersPlaqueAction(signedIn = true, channelLogin = "#Segall"),
        )
        assertEquals(TwitchPlaqueAction.LogIn, subscribersPlaqueAction(signedIn = false, channelLogin = "Segall"))
        assertEquals(
            TwitchPlaqueAction.Open("https://www.twitch.tv/subs/segall"),
            subscribersPlaqueAction(signedIn = true, channelLogin = "#Segall"),
        )
    }

    @Test
    fun slowCountdownStartsFromTheDeadlineAndBlocksSend() {
        val deadline = slowModeDeadline(sentAtMillis = 5_000L, slowSeconds = 30)
        val plaques = plaques(slowSeconds = 30, slowDeadlineMillis = deadline, nowMillis = 10_000L)
        val slow = plaques.single() as ChatModePlaque.Slow
        assertEquals(25_000L, slow.remainingMillis)
        assertTrue(slowModeBlocksSend(slow, awaitingOwnEcho = false))
    }

    @Test
    fun slowModeOffDoesNotBlockEvenWithADeadline() {
        val plaques = plaques(slowDeadlineMillis = 50_000L, nowMillis = 10_000L)
        assertTrue(plaques.isEmpty())
        assertFalse(slowModeBlocksSend(null, awaitingOwnEcho = true))
    }

    @Test
    fun slowModeWaitingForOwnEchoBlocksSendWithoutACountdown() {
        val slow = plaques(slowSeconds = 30).single() as ChatModePlaque.Slow
        assertNull(slow.remainingMillis)
        assertTrue(slowModeBlocksSend(slow, awaitingOwnEcho = true))
    }

    @Test
    fun ownLiveMessageFromAnyDeviceIsAnOwnChatLine() {
        assertTrue(isOwnLiveChatLine(liveMessage(userId = "42"), ownUserId = "42"))
    }

    @Test
    fun anotherUsersMessageOrSignedOutIsNotAnOwnChatLine() {
        assertFalse(isOwnLiveChatLine(liveMessage(userId = "7"), ownUserId = "42"))
        assertFalse(isOwnLiveChatLine(liveMessage(userId = "42"), ownUserId = null))
    }

    @Test
    fun ownSubscriptionNoticeIsNotAnOwnChatLine() {
        val notice = liveMessage(userId = "42", eventKind = ChatEventKind.Subscription)
        assertFalse(isOwnLiveChatLine(notice, ownUserId = "42"))
    }

    @Test
    fun ownMessageStartsTheSlowCountdownWhenItArrives() {
        assertEquals(40_000L, slowDeadline(liveMessage(userId = "42"), receivedAtMillis = 10_000L))
    }

    @Test
    fun ownMessageDoesNotStartTheSlowCountdownWhenSlowModeIsOff() {
        assertNull(slowDeadline(liveMessage(userId = "42"), slowSeconds = null))
    }

    @Test
    fun broadcasterModeratorsAndVipsAreExemptFromTheSlowCountdown() {
        assertNull(slowDeadline(liveMessage(userId = "42"), viewerIsBroadcaster = true))
        listOf("broadcaster", "lead_moderator", "moderator", "vip").forEach { setId ->
            assertNull(setId, slowDeadline(liveMessage(userId = "42", badges = listOf(ChatBadge(setId, "1")))))
        }
    }

    @Test
    fun subscriberBadgeDoesNotExemptFromTheSlowCountdown() {
        val subscriber = liveMessage(userId = "42", badges = listOf(ChatBadge("subscriber", "12")))
        assertEquals(30_000L, slowDeadline(subscriber))
    }

    @Test
    fun countdownCeilsToTheNextSecondAndShowsDays() {
        assertEquals("0:01", chatModeCountdown(1L))
        assertEquals("1:30", chatModeCountdown(90_000L))
        assertEquals("1:01:01", chatModeCountdown(3_661_000L))
        assertEquals("1d 1:00:00", chatModeCountdown(90_000_000L))
    }

    private fun plaques(
        followersOnlyMinutes: Int? = null,
        subscribersOnly: Boolean = false,
        emoteOnly: Boolean = false,
        slowSeconds: Int? = null,
        follow: FollowForChatMode = FollowForChatMode.Unknown,
        subscription: SubForChatMode = SubForChatMode.Unknown,
        viewerIsBroadcaster: Boolean = false,
        nowMillis: Long = 0L,
        slowDeadlineMillis: Long? = null,
    ): List<ChatModePlaque> = chatModePlaques(
        modes = ChatRoomModes(followersOnlyMinutes, subscribersOnly, emoteOnly, slowSeconds),
        viewerIsBroadcaster = viewerIsBroadcaster,
        follow = follow,
        subscription = subscription,
        nowMillis = nowMillis,
        slowDeadlineMillis = slowDeadlineMillis,
        channelName = "Segall",
    )

    private fun slowDeadline(
        message: ChatMessage,
        slowSeconds: Int? = 30,
        viewerIsBroadcaster: Boolean = false,
        receivedAtMillis: Long = 0L,
    ): Long? = slowModeDeadlineAfterOwnMessage(message, slowSeconds, viewerIsBroadcaster, receivedAtMillis)

    private fun liveMessage(
        userId: String,
        eventKind: ChatEventKind = ChatEventKind.Normal,
        badges: List<ChatBadge> = emptyList(),
    ) = ChatMessage(
        id = "m",
        userLogin = "viewer",
        displayName = "Viewer",
        color = Color.White,
        rawText = "hi",
        parts = emptyList(),
        timestampMillis = 0L,
        eventKind = eventKind,
        badges = badges,
        userId = userId,
    )
}
