package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedChatParserTest {
    @Test
    fun sharedChatMessageFromAnotherRoomKeepsItsSource() {
        fun parse(tags: String): ChatMessage = IrcMessageParser.parsePrivMsg(
            "@display-name=Viewer;id=shared-1;$tags " +
                ":viewer!viewer@viewer.tmi.twitch.tv PRIVMSG #channel :hello",
        )!!

        assertEquals("222", parse("room-id=111;source-room-id=222").sourceRoomId)
        assertNull(parse("room-id=111;source-room-id=111").sourceRoomId)
        assertNull(parse("room-id=111;source-room-id=").sourceRoomId)
        assertNull(parse("room-id=111").sourceRoomId)
    }

    @Test
    fun sharedChatNoticeFromAnotherRoomKeepsItsSource() {
        val notice = IrcMessageParser.parseUserNotice(
            "@display-name=Viewer;login=viewer;id=shared-2;msg-id=sharedchatnotice;room-id=111;" +
                "source-room-id=222;system-msg=Viewer\\ssubscribed " +
                ":tmi.twitch.tv USERNOTICE #channel",
        )!!

        assertEquals("222", notice.sourceRoomId)
    }

    @Test
    fun sharedChatNoticeTakesItsKindFromTheSourceMessageId() {
        fun parse(sourceMsgId: String, extra: String = ""): ChatMessage = IrcMessageParser.parseUserNotice(
            "@display-name=Viewer;login=viewer;id=shared-3;msg-id=sharedchatnotice;room-id=111;" +
                "source-room-id=222;source-msg-id=$sourceMsgId;system-msg=Viewer\\sdid\\ssomething$extra " +
                ":tmi.twitch.tv USERNOTICE #channel :hello",
        )!!

        assertEquals(ChatEventKind.Subscription, parse("resub").eventKind)
        assertTrue(parse("sub", ";msg-param-sub-plan=Prime").primeSubscription)
        assertEquals(ChatEventKind.Announcement, parse("announcement").eventKind)
    }

    @Test
    fun sharedChatRaidsGiftsAndStreaksStayLinesOfTheirOwnChannel() {
        fun parse(sourceMsgId: String, extra: String): ChatMessage = IrcMessageParser.parseUserNotice(
            "@display-name=Viewer;login=viewer;id=shared-4;msg-id=sharedchatnotice;room-id=111;" +
                "source-room-id=222;source-msg-id=$sourceMsgId;system-msg=Viewer\\sdid\\ssomething;$extra " +
                ":tmi.twitch.tv USERNOTICE #channel",
        )!!

        val raid = parse("raid", "msg-param-viewerCount=50;msg-param-displayName=Raider")
        assertEquals(ChatEventKind.System, raid.eventKind)
        assertNull(raid.raid)
        val gift = parse("submysterygift", "msg-param-mass-gift-count=5;msg-param-sub-plan=1000")
        assertEquals(ChatEventKind.Subscription, gift.eventKind)
        assertNull(gift.communityGift)
        val streak = parse("viewermilestone", "msg-param-category=watch-streak;msg-param-value=5")
        assertNull(streak.watchStreak)
    }
}
