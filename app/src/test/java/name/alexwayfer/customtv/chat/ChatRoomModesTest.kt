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
    fun r9kTurnsUniqueChatOnAndOff() {
        val on = chatRoomModesAfter(ChatRoomModes(), mapOf("r9k" to "1"))
        assertTrue(on.uniqueChat)
        assertFalse(chatRoomModesAfter(on, mapOf("r9k" to "0")).uniqueChat)
        assertTrue(chatRoomModesAfter(on, mapOf("slow" to "0")).uniqueChat)
    }

    @Test
    fun aChangeSetsOnlyItsMode() {
        val current = ChatRoomModes(followersOnlyMinutes = 10, slowSeconds = 30)
        assertEquals(current.copy(slowSeconds = 5), current.with(ChatModeChange.Slow(5)))
        assertEquals(current.copy(slowSeconds = null), current.with(ChatModeChange.Slow(null)))
        assertEquals(current.copy(followersOnlyMinutes = 0), current.with(ChatModeChange.Followers(0)))
        assertEquals(current.copy(followersOnlyMinutes = null), current.with(ChatModeChange.Followers(null)))
        assertEquals(current.copy(subscribersOnly = true), current.with(ChatModeChange.Subscribers(true)))
        assertEquals(current.copy(emoteOnly = true), current.with(ChatModeChange.EmoteOnly(true)))
        assertEquals(current.copy(uniqueChat = true), current.with(ChatModeChange.UniqueChat(true)))
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
        val plaques = plaques(ChatRoomModes(followersOnlyMinutes = 0), viewer(follow = FollowForChatMode.NotFollowing))
        assertEquals(listOf(ChatModePlaque.FollowersNeedFollow("Segall")), plaques)
    }

    @Test
    fun unknownFollowHidesFollowersPlaque() {
        val plaques = plaques(ChatRoomModes(followersOnlyMinutes = 10), viewer(follow = FollowForChatMode.Unknown))
        assertTrue(plaques.isEmpty())
    }

    @Test
    fun followingForTenMinutesWithFiveElapsedShowsFiveRemaining() {
        val followedAt = 1_000_000L
        val plaques = plaques(
            ChatRoomModes(followersOnlyMinutes = 10),
            viewer(follow = FollowForChatMode.Following(followedAt)),
            nowMillis = followedAt + 5 * 60_000L,
        )
        assertEquals(listOf(ChatModePlaque.FollowersWait(5 * 60_000L)), plaques)
    }

    @Test
    fun followingLongEnoughHidesFollowersPlaque() {
        val followedAt = 1_000_000L
        val plaques = plaques(
            ChatRoomModes(followersOnlyMinutes = 10),
            viewer(follow = FollowForChatMode.Following(followedAt)),
            nowMillis = followedAt + 10 * 60_000L,
        )
        assertTrue(plaques.isEmpty())
    }

    @Test
    fun anyFollowerCanChatWhenFollowersOnlyIsZero() {
        val plaques = plaques(
            ChatRoomModes(followersOnlyMinutes = 0),
            viewer(follow = FollowForChatMode.Following(1L)),
            nowMillis = 1L,
        )
        assertTrue(plaques.isEmpty())
    }

    @Test
    fun broadcasterModeratorsAndVipsAreExemptFromChatModes() {
        assertTrue(chatModesExempt(isBroadcaster = true, ownBadges = null))
        listOf("broadcaster", "lead_moderator", "moderator", "vip").forEach { setId ->
            assertTrue(setId, chatModesExempt(isBroadcaster = false, ownBadges = listOf(ChatBadge(setId, "1"))))
        }
    }

    @Test
    fun subscriberOrUnknownBadgesAreHeldByChatModes() {
        assertFalse(chatModesExempt(isBroadcaster = false, ownBadges = null))
        assertFalse(chatModesExempt(isBroadcaster = false, ownBadges = listOf(ChatBadge("subscriber", "12"))))
    }

    @Test
    fun ownerFollowsAndSubscribesSoSeesOnlyEmotesAndSlowWithoutACountdown() {
        val plaques = plaques(
            ChatRoomModes(followersOnlyMinutes = 10, subscribersOnly = true, emoteOnly = true, slowSeconds = 10),
            viewer(
                follow = FollowForChatMode.NotFollowing,
                subscription = SubForChatMode.NotSubscribed,
                exempt = chatModesExempt(isBroadcaster = true, ownBadges = null),
                ownsChannel = true,
            ),
            slowDeadlineMillis = 2_000L,
            nowMillis = 1_000L,
        )
        assertEquals(listOf(ChatModePlaque.Emotes, ChatModePlaque.Slow(10, null)), plaques)
    }

    @Test
    fun moderatorSeesRoomModesWithoutAPersonalLimit() {
        assertRoomModesWithoutAPersonalLimit(
            chatModesExempt(isBroadcaster = false, ownBadges = listOf(ChatBadge("moderator", "1"))),
        )
    }

    @Test
    fun vipSeesRoomModesWithoutAPersonalLimit() {
        assertRoomModesWithoutAPersonalLimit(
            chatModesExempt(isBroadcaster = false, ownBadges = listOf(ChatBadge("vip", "1"))),
        )
    }

    @Test
    fun exemptFollowerOfLongEnoughSeesNoFollowersPlaque() {
        val followedAt = 1_000_000L
        val plaques = plaques(
            ChatRoomModes(followersOnlyMinutes = 10),
            viewer(follow = FollowForChatMode.Following(followedAt), exempt = true),
            nowMillis = followedAt + 10 * 60_000L,
        )
        assertTrue(plaques.isEmpty())
    }

    @Test
    fun exemptFollowerStillShortOfTheTimeSeesFollowersWithoutACountdown() {
        val followedAt = 1_000_000L
        val plaques = plaques(
            ChatRoomModes(followersOnlyMinutes = 10),
            viewer(follow = FollowForChatMode.Following(followedAt), exempt = true),
            nowMillis = followedAt + 5 * 60_000L,
        )
        assertEquals(listOf(ChatModePlaque.FollowersRoom), plaques)
    }

    @Test
    fun exemptViewerWithUnknownFollowOrSubscriptionSeesNeitherPlaque() {
        val plaques = plaques(
            ChatRoomModes(followersOnlyMinutes = 10, subscribersOnly = true),
            viewer(exempt = true),
        )
        assertTrue(plaques.isEmpty())
    }

    @Test
    fun exemptSubscriberSeesNoSubscribersPlaque() {
        val plaques = plaques(
            ChatRoomModes(subscribersOnly = true),
            viewer(subscription = SubForChatMode.Subscribed, exempt = true),
        )
        assertTrue(plaques.isEmpty())
    }

    private fun assertRoomModesWithoutAPersonalLimit(exempt: Boolean) {
        val plaques = plaques(
            ChatRoomModes(followersOnlyMinutes = 10, subscribersOnly = true, emoteOnly = true, slowSeconds = 10),
            viewer(
                follow = FollowForChatMode.NotFollowing,
                subscription = SubForChatMode.NotSubscribed,
                exempt = exempt,
            ),
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
            ChatRoomModes(followersOnlyMinutes = 0, subscribersOnly = true, emoteOnly = true, slowSeconds = 30),
            viewer(follow = FollowForChatMode.NotFollowing, subscription = SubForChatMode.NotSubscribed),
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
        val plaques = plaques(ChatRoomModes(subscribersOnly = true), viewer(subscription = SubForChatMode.Subscribed))
        assertTrue(plaques.isEmpty())
    }

    @Test
    fun unknownSubscriptionHidesSubscribersPlaque() {
        val plaques = plaques(ChatRoomModes(subscribersOnly = true), viewer(subscription = SubForChatMode.Unknown))
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
        val plaques = plaques(ChatRoomModes(slowSeconds = 30), slowDeadlineMillis = deadline, nowMillis = 10_000L)
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
        val slow = plaques(ChatRoomModes(slowSeconds = 30)).single() as ChatModePlaque.Slow
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
        assertNull(slowDeadline(liveMessage(userId = "42"), viewerExempt = true))
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

    /** The plaques in Segall's chat. */
    private fun plaques(
        modes: ChatRoomModes = ChatRoomModes(),
        viewer: ChatModeViewer = viewer(),
        nowMillis: Long = 0L,
        slowDeadlineMillis: Long? = null,
    ): List<ChatModePlaque> = chatModePlaques(modes, viewer, nowMillis, slowDeadlineMillis, channelName = "Segall")

    private fun viewer(
        follow: FollowForChatMode = FollowForChatMode.Unknown,
        subscription: SubForChatMode = SubForChatMode.Unknown,
        exempt: Boolean = false,
        ownsChannel: Boolean = false,
    ) = ChatModeViewer(exempt, follow, subscription, ownsChannel)

    private fun slowDeadline(
        message: ChatMessage,
        slowSeconds: Int? = 30,
        viewerExempt: Boolean = false,
        receivedAtMillis: Long = 0L,
    ): Long? = slowModeDeadlineAfterOwnMessage(message, slowSeconds, viewerExempt, receivedAtMillis)

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
