package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.compose.ui.graphics.Color

class IrcMessageParserTest {
    @Test
    fun minHashNickGetsTheFirstPaletteColor() {
        assertEquals(Int.MIN_VALUE, "polygenelubricants".hashCode())
        assertEquals(0, nameColorIndex(Int.MIN_VALUE, 15))
        assertEquals(Color(0xFFFF0000), IrcMessageParser.nameColor("polygenelubricants"))
    }

    @Test
    fun negativeHashKeepsTheAbsoluteRemainder() {
        assertEquals(8, nameColorIndex(-8, 15))
        assertEquals(4, nameColorIndex(4, 15))
    }

    @Test
    fun userIdDoesNotDependOnLogin() {
        val message = IrcMessageParser.parsePrivMsg(
            "@display-name=NewName;id=message-1;user-id=12345 " +
                ":newlogin!newlogin@newlogin.tmi.twitch.tv PRIVMSG #channel :hello",
        )!!

        assertEquals("12345", message.userId)
        assertEquals("newlogin", message.userLogin)
    }

    @Test
    fun parsesBitsAmountWithoutChangingOriginalText() {
        val message = IrcMessageParser.parsePrivMsg(
            "@bits=150;display-name=Viewer;id=cheer-1 " +
                ":viewer!viewer@viewer.tmi.twitch.tv PRIVMSG #channel :Cheer100 Cheer50 nice!",
        )!!

        assertEquals(150, message.cheerBits)
        assertEquals("Cheer100 Cheer50 nice!", message.rawText)
        assertNull(
            IrcMessageParser.parsePrivMsg(
                "@display-name=Viewer;id=normal-1 " +
                    ":viewer!viewer@viewer.tmi.twitch.tv PRIVMSG #channel :Cheer100",
            )!!.cheerBits,
        )
    }

    @Test
    fun firstMsgTagDistinguishesChannelFirstFromSessionFirst() {
        fun parse(tag: String): ChatMessage = IrcMessageParser.parsePrivMsg(
            "@display-name=NewViewer;id=first-1;$tag " +
                ":newviewer!newviewer@newviewer.tmi.twitch.tv PRIVMSG #channel :hello",
        )!!

        assertTrue(parse("first-msg=1").firstInChannel)
        assertFalse(parse("first-msg=0").firstInChannel)
        assertFalse(parse("tmi-sent-ts=1700000000000").firstInChannel)
        assertFalse(parse("first-msg=1").firstInSession)
    }

    @Test
    fun parsePrivMsgWithTwitchEmote() {
        val raw = "@badge-info=;badges=;color=#FF4500;display-name=CoolUser;emotes=25:6-10;id=abc-123;mod=0;tmi-sent-ts=1700000000000 " +
            ":cooluser!cooluser@cooluser.tmi.twitch.tv PRIVMSG #channel :Hello Kappa there"
        val message = IrcMessageParser.parsePrivMsg(raw)
        assertNotNull(message)
        assertEquals("CoolUser", message!!.displayName)
        assertEquals("cooluser", message.userLogin)
        assertEquals("abc-123", message.id)
        assertEquals(1700000000000L, message.timestampMillis)
        assertEquals(3, message.parts.size)
        assertEquals(ChatPart.Text("Hello "), message.parts[0])
        val emote = message.parts[1] as ChatPart.Emote
        assertEquals("Kappa", emote.name)
        assertTrue(emote.url.contains("/25/animated/"))
        assertEquals(
            "https://static-cdn.jtvnw.net/emoticons/v2/25/default/dark/2.0",
            twitchEmoteUrl("25", animated = false),
        )
        assertEquals(ChatPart.Text(" there"), message.parts[2])
        assertTrue(message.badges.isEmpty())
    }

    @Test
    fun parseReplyParentTags() {
        val raw = "@badge-info=;badges=;color=#FF4500;display-name=Replier;emotes=;id=reply-1;" +
            "reply-parent-display-name=CoolUser;reply-parent-msg-body=Hello\\sKappa\\sthere;" +
            "reply-parent-msg-id=abc-123;reply-parent-user-id=1;reply-parent-user-login=cooluser;" +
            "reply-thread-parent-msg-id=thread-root;reply-thread-parent-user-login=rootuser;" +
            "tmi-sent-ts=1700000000000 " +
            ":replier!replier@replier.tmi.twitch.tv PRIVMSG #channel :@CoolUser nice"
        val message = IrcMessageParser.parsePrivMsg(raw)
        assertNotNull(message)
        val reply = message!!.reply
        assertNotNull(reply)
        assertEquals("abc-123", reply!!.parentMsgId)
        assertEquals("cooluser", reply.parentUserLogin)
        assertEquals("CoolUser", reply.parentDisplayName)
        assertEquals("thread-root", reply.threadParentMsgId)
        assertEquals("rootuser", reply.threadParentUserLogin)
        assertEquals("Hello Kappa there", reply.parentBody)
        assertFalse(reply.parentIsAction)
        assertEquals("nice", message.rawText)
        assertEquals("@CoolUser nice", message.copyableText())
        assertEquals(listOf(ChatPart.Text("nice")), message.parts)
        assertNull(IrcMessageParser.parseReply(emptyMap()))
    }

    @Test
    fun stripReplyMentionKeepsEmotesAligned() {
        val raw = "@badge-info=;badges=;color=#FF4500;display-name=Replier;emotes=25:10-14;id=reply-2;" +
            "reply-parent-display-name=CoolUser;reply-parent-msg-body=Hi;reply-parent-msg-id=abc-123;" +
            "reply-parent-user-login=cooluser;tmi-sent-ts=1700000000000 " +
            ":replier!replier@replier.tmi.twitch.tv PRIVMSG #channel :@CoolUser Kappa"
        val message = IrcMessageParser.parsePrivMsg(raw)
        assertNotNull(message)
        assertEquals("Kappa", message!!.rawText)
        assertEquals(1, message.parts.size)
        assertEquals("Kappa", (message.parts[0] as ChatPart.Emote).name)
    }

    @Test
    fun keepMentionWhenMessageIsNotAReply() {
        val raw = "@badge-info=;badges=;color=#FF4500;display-name=Replier;emotes=;id=not-reply;" +
            "tmi-sent-ts=1700000000000 " +
            ":replier!replier@replier.tmi.twitch.tv PRIVMSG #channel :@CoolUser hi"
        val message = IrcMessageParser.parsePrivMsg(raw)
        assertEquals("@CoolUser hi", message!!.rawText)
        assertNull(message.reply)
    }

    @Test
    fun doNotStripPartialMentionPrefix() {
        val reply = ChatReply("id", "cooluser", "CoolUser", "Hi")
        assertEquals(
            "@CoolUserish hello",
            IrcMessageParser.stripLeadingReplyMention("@CoolUserish hello", reply),
        )
        assertEquals(
            "hello",
            IrcMessageParser.stripLeadingReplyMention("@cooluser hello", reply),
        )
    }

    @Test
    fun parseRepeatedEmotesAsSeparateParts() {
        val raw = "@badge-info=;badges=;color=#FF4500;display-name=CoolUser;emotes=25:0-4,6-10,12-16;id=abc-456;tmi-sent-ts=1700000000000 " +
            ":cooluser!cooluser@cooluser.tmi.twitch.tv PRIVMSG #channel :Kappa Kappa Kappa"
        val message = IrcMessageParser.parsePrivMsg(raw)
        assertNotNull(message)
        assertEquals(5, message!!.parts.size)
        assertEquals("Kappa", (message.parts[0] as ChatPart.Emote).name)
        assertEquals(ChatPart.Text(" "), message.parts[1])
        assertEquals("Kappa", (message.parts[2] as ChatPart.Emote).name)
        assertEquals(ChatPart.Text(" "), message.parts[3])
        assertEquals("Kappa", (message.parts[4] as ChatPart.Emote).name)
        assertEquals(3, message.parts.filterIsInstance<ChatPart.Emote>().size)
    }

    @Test
    fun parseBadgesFromTag() {
        val badges = IrcMessageParser.parseBadges("broadcaster/1,subscriber/12,premium/1")
        assertEquals(3, badges.size)
        assertEquals("broadcaster", badges[0].setId)
        assertEquals("1", badges[0].version)
        assertEquals("subscriber/12", badges[1].key)
        assertEquals("premium/1", badges[2].key)
        assertTrue(IrcMessageParser.parseBadges("").isEmpty())
        assertTrue(IrcMessageParser.parseBadges(null).isEmpty())
    }

    @Test
    fun parseResubUserNoticeWithMessage() {
        val raw = "@badge-info=;badges=staff/1,broadcaster/1,turbo/1;color=#008000;display-name=ronni;emotes=;id=db25007f-7a18-43eb-9379-80131e44d633;login=ronni;mod=0;msg-id=resub;msg-param-cumulative-months=6;msg-param-streak-months=2;msg-param-should-share-streak=1;msg-param-sub-plan=Prime;msg-param-sub-plan-name=Prime;room-id=1337;subscriber=1;system-msg=ronni\\shas\\ssubscribed\\sfor\\s6\\smonths!;tmi-sent-ts=1507246572675;turbo=1;user-id=1337;user-type=staff " +
            ":tmi.twitch.tv USERNOTICE #dallas :Great stream -- keep it up!"
        val message = IrcMessageParser.parseUserNotice(raw)
        assertNotNull(message)
        assertEquals(ChatEventKind.Subscription, message!!.eventKind)
        assertEquals("ronni", message.userLogin)
        assertTrue(message.primeSubscription)
        assertEquals("ronni", message.displayName)
        assertEquals("ronni has subscribed for 6 months!", message.systemText)
        assertEquals("Great stream -- keep it up!", message.rawText.substringAfter("! "))
        assertEquals(1, message.parts.size)
        assertEquals(ChatPart.Text("Great stream -- keep it up!"), message.parts[0])
    }

    @Test
    fun parseAnnouncementUserNotice() {
        val raw = "@badge-info=;badges=broadcaster/1;color=#FFFFFF;display-name=Streamer;emotes=;id=ann-1;login=streamer;mod=1;msg-id=announcement;msg-param-color=PURPLE;room-id=123;subscriber=0;system-msg=;tmi-sent-ts=1700000000000;user-id=123;user-type=mod " +
            ":tmi.twitch.tv USERNOTICE #channel :Hello everyone"
        val message = IrcMessageParser.parseUserNotice(raw)
        assertNotNull(message)
        assertEquals(ChatEventKind.Announcement, message!!.eventKind)
        assertEquals("streamer", message.userLogin)
        assertEquals("Streamer", message.displayName)
        assertEquals(null, message.systemText)
        assertEquals(ChatPart.Text("Hello everyone"), message.parts[0])
        assertEquals(Color(0xFFBF94FF), checkNotNull(message.accentColor))
    }

    @Test
    fun announcementColorsMatchTwitchCommands() {
        assertEquals(Color(0xFF00D6D6), IrcMessageParser.announcementColor("BLUE"))
        assertEquals(Color(0xFF00DB84), IrcMessageParser.announcementColor("GREEN"))
        assertEquals(Color(0xFFFFB31A), IrcMessageParser.announcementColor("ORANGE"))
        assertEquals(Color(0xFFBF94FF), IrcMessageParser.announcementColor("PURPLE"))
        assertEquals(Color(0xFFADADB8), IrcMessageParser.announcementColor("PRIMARY"))
        assertEquals(Color(0xFFADADB8), IrcMessageParser.announcementColor(null))
    }

    @Test
    fun parseSubGiftUserNoticeWithoutTrailing() {
        val raw = "@display-name=Gifter;login=gifter;msg-id=subgift;msg-param-months=1;msg-param-recipient-display-name=Target;msg-param-recipient-user-name=target;msg-param-sub-plan=1000;system-msg=Gifter\\sgifted\\sa\\sTier\\s1\\ssub\\sto\\sTarget!;tmi-sent-ts=1700000000000;id=gift-1 " +
            ":tmi.twitch.tv USERNOTICE #channel"
        val message = IrcMessageParser.parseUserNotice(raw)
        assertNotNull(message)
        assertEquals(ChatEventKind.Subscription, message!!.eventKind)
        assertEquals("Gifter gifted a Tier 1 sub to Target!", message.systemText)
        assertFalse(message.primeSubscription)
        assertTrue(message.parts.isEmpty())
    }

    @Test
    fun parseCommunityGiftUserNotice() {
        val message = IrcMessageParser.parseUserNotice(
            "@display-name=Generous;login=generous;msg-id=submysterygift;" +
                "msg-param-mass-gift-count=100;msg-param-sender-count=125;" +
                "msg-param-sub-plan=2000;system-msg=Generous\\sgifted\\s100\\ssubs;" +
                "tmi-sent-ts=1700000000000;id=community-gift-1 " +
                ":tmi.twitch.tv USERNOTICE #channel",
        )!!

        assertEquals(
            ChatCommunityGift(
                count = 100,
                tier = 2,
                gifterDisplayName = "Generous",
                cumulativeCount = 125,
                anonymous = false,
            ),
            message.communityGift,
        )
        assertEquals(ChatEventKind.Subscription, message.eventKind)
    }

    @Test
    fun parseAnonymousCommunityGiftUserNotice() {
        val message = IrcMessageParser.parseUserNotice(
            "@display-name=;login=ananonymousgifter;msg-id=anonsubmysterygift;" +
                "msg-param-mass-gift-count=10;msg-param-sub-plan=1000;" +
                "system-msg=An\\sanonymous\\suser\\sgifted\\s10\\ssubs;id=anonymous-gift-1 " +
                ":tmi.twitch.tv USERNOTICE #channel",
        )!!

        assertEquals(10, message.communityGift?.count)
        assertEquals(true, message.communityGift?.anonymous)
        assertEquals(null, message.communityGift?.gifterDisplayName)
    }

    @Test
    fun primePaidUpgradeDoesNotUsePrimeCrown() {
        val message = IrcMessageParser.parseUserNotice(
            "@display-name=Viewer;login=viewer;msg-id=primepaidupgrade;" +
                "msg-param-sub-plan=Prime;system-msg=Viewer\\supgraded;id=upgrade-1 " +
                ":tmi.twitch.tv USERNOTICE #channel",
        )!!

        assertEquals(ChatEventKind.Subscription, message.eventKind)
        assertFalse(message.primeSubscription)
    }

    @Test
    fun parseRaidUserNotice() {
        val raw = "@display-name=Raider;login=raider;msg-id=raid;msg-param-displayName=Raider;msg-param-viewerCount=1234;system-msg=1234\\sraiders\\sfrom\\sRaider\\shave\\sjoined!;tmi-sent-ts=1700000000000;id=raid-1 " +
            ":tmi.twitch.tv USERNOTICE #channel"
        val message = IrcMessageParser.parseUserNotice(raw)
        assertNotNull(message)
        assertEquals(ChatEventKind.Raid, message!!.eventKind)
        assertEquals("1234 raiders from Raider have joined!", message.systemText)
        assertEquals(1234, message.raid?.viewerCount)
        assertEquals("Raider", message.raid?.fromDisplayName)
        assertEquals(false, message.raid?.canceled)
    }

    @Test
    fun parseRoomNotice() {
        val raw = "@msg-id=slow_on :tmi.twitch.tv NOTICE #channel :This room is now in slow mode. You may send messages every 30 seconds."
        val message = IrcMessageParser.parseNotice(raw)
        assertNotNull(message)
        assertEquals(ChatEventKind.System, message!!.eventKind)
        assertEquals("This room is now in slow mode. You may send messages every 30 seconds.", message.systemText)
        assertTrue(message.displayName.isEmpty())
    }

    @Test
    fun parseHighlightedPrivMsg() {
        val raw = "@badge-info=;badges=;color=#FF4500;display-name=CoolUser;emotes=;id=hl-1;msg-id=highlighted-message;tmi-sent-ts=1700000000000 " +
            ":cooluser!cooluser@cooluser.tmi.twitch.tv PRIVMSG #channel :Pay attention"
        val message = IrcMessageParser.parsePrivMsg(raw)
        assertNotNull(message)
        assertEquals(ChatEventKind.Highlight, message!!.eventKind)
        assertEquals("Pay attention", (message.parts[0] as ChatPart.Text).text)
    }

    @Test
    fun parseCustomRewardPrivMsg() {
        val raw = "@badge-info=;badges=;color=#FF4500;custom-reward-id=4eb8ee0f-2c05-4848-9ed4-d369b3ff5986;display-name=AlexWayfer;emotes=;id=rw-1;tmi-sent-ts=1700000000000 " +
            ":alexwayfer!alexwayfer@alexwayfer.tmi.twitch.tv PRIVMSG #alexwayfer :stand up"
        val message = IrcMessageParser.parsePrivMsg(raw)
        assertNotNull(message)
        assertEquals(ChatEventKind.Reward, message!!.eventKind)
        assertEquals("4eb8ee0f-2c05-4848-9ed4-d369b3ff5986", message.reward?.id)
        assertEquals("stand up", (message.parts[0] as ChatPart.Text).text)
    }

    @Test
    fun parsePingStyleLine() {
        val line = IrcMessageParser.parseLine(":tmi.twitch.tv 001 justinfan123 :Welcome")
        assertEquals("001", line!!.command)
        assertEquals("Welcome", line.trailing)
    }

    @Test
    fun parseClearMsgMarksTargetIdAndOptionalModerator() {
        val raw = "@login=ronni;room-id=123;target-msg-id=abc-123-def;tmi-sent-ts=1642720582342 " +
            ":tmi.twitch.tv CLEARMSG #dallas :HeyGuys"
        val message = IrcMessageParser.parseClearMsg(raw)
        assertNotNull(message)
        assertEquals("abc-123-def", message!!.id)
        assertEquals("ronni", message.userLogin)
        assertEquals(ChatEventKind.MessageDeleted, message.eventKind)
        assertTrue(message.deleted)
        assertNull(message.deletedBy)
        assertEquals("HeyGuys", message.rawText)

        val withMod = IrcMessageParser.parseClearMsg(
            "@login=ronni;moderator-login=CoolMod;target-msg-id=abc-123-def " +
                ":tmi.twitch.tv CLEARMSG #dallas :HeyGuys",
        )
        assertEquals("CoolMod", withMod!!.deletedBy)
    }

    @Test
    fun parseClearChatTargetsUserAndIgnoresFullClear() {
        val raw = "@ban-duration=600;room-id=123;target-user-id=456;tmi-sent-ts=1642720582342 " +
            ":tmi.twitch.tv CLEARCHAT #dallas :ronni"
        val message = IrcMessageParser.parseClearChat(raw)
        assertNotNull(message)
        assertEquals("ronni", message!!.userLogin)
        assertEquals(ChatEventKind.UserMessagesDeleted, message.eventKind)
        assertTrue(message.deleted)
        assertEquals(600L, message.timeoutSeconds)
        assertFalse(message.banned)

        val ban = IrcMessageParser.parseClearChat(
            "@room-id=123;tmi-sent-ts=1642720582343 :tmi.twitch.tv CLEARCHAT #dallas :ronni",
        )
        assertTrue(ban!!.banned)
        assertNull(ban.timeoutSeconds)

        assertNull(
            IrcMessageParser.parseClearChat(
                "@room-id=123;tmi-sent-ts=1642720582342 :tmi.twitch.tv CLEARCHAT #dallas",
            ),
        )
    }

    @Test
    fun parseWatchStreakUserNotice() {
        val raw = "@badge-info=;badges=;color=#FF4500;display-name=CoolUser;emotes=;id=streak-1;" +
            "login=cooluser;msg-id=viewermilestone;msg-param-category=watch-streak;" +
            "msg-param-copoReward=450;msg-param-value=10;system-msg=CoolUser\\swatched\\s10\\s" +
            "consecutive\\sstreams\\sand\\ssparked\\sa\\swatch\\sstreak!;tmi-sent-ts=1700000000000 " +
            ":tmi.twitch.tv USERNOTICE #channel"
        val message = IrcMessageParser.parseUserNotice(raw)
        assertNotNull(message)
        assertEquals(ChatEventKind.WatchStreak, message!!.eventKind)
        assertEquals("CoolUser watched 10 consecutive streams", message.systemText)
        assertEquals(10, message.watchStreak?.consecutiveStreams)
        assertEquals(450, message.watchStreak?.points)
        assertTrue(message.parts.isEmpty())
        assertFalse(message.systemText!!.contains("sparked"))
    }

    @Test
    fun parseWatchStreakUserNoticeWithMessageAndSingularCount() {
        val raw = "@display-name=CoolUser;emotes=;id=streak-2;login=cooluser;msg-id=viewermilestone;" +
            "msg-param-category=watch-streak;msg-param-value=1;system-msg=CoolUser\\ssparked\\sa\\s" +
            "watch\\sstreak!;tmi-sent-ts=1700000000000 " +
            ":tmi.twitch.tv USERNOTICE #channel :hello"
        val message = IrcMessageParser.parseUserNotice(raw)
        assertEquals(ChatEventKind.WatchStreak, message!!.eventKind)
        assertEquals("CoolUser watched 1 consecutive stream", message.systemText)
        assertEquals(1, message.watchStreak?.consecutiveStreams)
        assertEquals(null, message.watchStreak?.points)
        assertEquals(listOf(ChatPart.Text("hello")), message.parts)
    }

    @Test
    fun parseActionPrivMsgStripsWrapperAndKeepsTwitchEmote() {
        val raw = "@badge-info=;badges=;color=#FF4500;display-name=CoolUser;emotes=25:8-12;id=act-1;" +
            "tmi-sent-ts=1700000000000 " +
            ":cooluser!cooluser@cooluser.tmi.twitch.tv PRIVMSG #channel :\u0001ACTION Kappa\u0001"
        val message = IrcMessageParser.parsePrivMsg(raw)
        assertNotNull(message)
        assertTrue(message!!.isAction)
        assertEquals("Kappa", message.rawText)
        assertEquals(1, message.parts.size)
        assertEquals("Kappa", (message.parts[0] as ChatPart.Emote).name)
        assertFalse(
            IrcMessageParser.parsePrivMsg(
                "@display-name=CoolUser;emotes=;id=n;tmi-sent-ts=1 " +
                    ":cooluser!cooluser@cooluser.tmi.twitch.tv PRIVMSG #channel :hello",
            )!!.isAction,
        )
    }

    @Test
    fun parseActionWithoutTrailingSohStillStripsPrefix() {
        val raw = "@display-name=CoolUser;emotes=;id=act-2;tmi-sent-ts=1700000000000 " +
            ":cooluser!cooluser@cooluser.tmi.twitch.tv PRIVMSG #channel :\u0001ACTION waves"
        val message = IrcMessageParser.parsePrivMsg(raw)
        assertTrue(message!!.isAction)
        assertEquals("waves", message.rawText)
        assertEquals(listOf(ChatPart.Text("waves")), message.parts)
    }

    @Test
    fun parseActionReplyStripsMentionThenKeepsEmote() {
        val raw = "@badge-info=;badges=;color=#FF4500;display-name=Replier;emotes=25:18-22;id=act-reply;" +
            "reply-parent-display-name=CoolUser;reply-parent-msg-body=Hi;reply-parent-msg-id=abc-123;" +
            "reply-parent-user-login=cooluser;tmi-sent-ts=1700000000000 " +
            ":replier!replier@replier.tmi.twitch.tv PRIVMSG #channel :\u0001ACTION @CoolUser Kappa\u0001"
        val message = IrcMessageParser.parsePrivMsg(raw)
        assertNotNull(message)
        assertTrue(message!!.isAction)
        assertEquals("Kappa", message.rawText)
        assertEquals(1, message.parts.size)
        assertEquals("Kappa", (message.parts[0] as ChatPart.Emote).name)
    }

    @Test
    fun parseReplyParentActionStripsWrapperAndOmitsColon() {
        val raw = "@badge-info=;badges=;color=#FF4500;display-name=Replier;emotes=;id=reply-act;" +
            "reply-parent-display-name=CoolUser;reply-parent-msg-body=\u0001ACTION\\swaves\u0001;" +
            "reply-parent-msg-id=abc-123;reply-parent-user-login=cooluser;tmi-sent-ts=1700000000000 " +
            ":replier!replier@replier.tmi.twitch.tv PRIVMSG #channel :@CoolUser nice"
        val message = IrcMessageParser.parsePrivMsg(raw)
        val reply = message!!.reply
        assertNotNull(reply)
        assertTrue(reply!!.parentIsAction)
        assertEquals("waves", reply.parentBody)
        assertFalse(message.isAction)
        assertEquals("nice", message.rawText)
    }

    @Test
    fun actionMessageThirdPartyEmotesMatchAfterStrip() {
        val raw = "@display-name=CoolUser;emotes=;id=act-7tv;tmi-sent-ts=1700000000000 " +
            ":cooluser!cooluser@cooluser.tmi.twitch.tv PRIVMSG #channel :\u0001ACTION hello Sadge\u0001"
        val message = IrcMessageParser.parsePrivMsg(raw)
        assertTrue(message!!.isAction)
        assertEquals("hello Sadge", message.rawText)
        val result = ThirdPartyEmoteMatcher.overlay(
            message.parts,
            mapOf("Sadge" to SevenTvEmote("https://cdn.7tv.app/emote/sadge/2x.webp")),
        )
        assertEquals(ChatPart.Text("hello "), result[0])
        assertEquals("Sadge", (result[1] as ChatPart.Emote).name)
    }

    @Test
    fun doesNotTreatActionLookalikeAsCtcp() {
        val plain = IrcMessageParser.stripCtcpAction("ACTION waves")
        assertFalse(plain.isAction)
        assertEquals("ACTION waves", plain.text)
        val glued = IrcMessageParser.stripCtcpAction("\u0001ACTIONFOO")
        assertFalse(glued.isAction)
    }

    @Test
    fun parseGifPrivMsgUsesExactUrlFromTag() {
        val url = "https://media4.giphy.com/media/joSNxeswxuc74Juo8X/giphy.gif" +
            "?cid=095d7a5dzizsiwgabonagkmigggv8v1spfai91ac3x0dsiy0&ep=v1_gifs_trending&rid=giphy.gif&ct=g"
        val title = "[Y A Y Yes GIF by Djemilah Birnie]"
        val raw = "@badge-info=subscriber/30;badges=broadcaster/1,subscriber/0;color=#033700;" +
            "display-name=TwitchDev;emotes=;gifs=0-33|joSNxeswxuc74Juo8X|$url;" +
            "id=401abf17-7e99-45d6-9bdf-43934e839327;tmi-sent-ts=1783632907018 " +
            ":twitchdev!twitchdev@twitchdev.tmi.twitch.tv PRIVMSG #twitch :$title"
        val message = IrcMessageParser.parsePrivMsg(raw)
        assertNotNull(message)
        assertEquals(1, message!!.parts.size)
        val gif = message.parts[0] as ChatPart.Gif
        assertEquals(title, gif.name)
        assertEquals(url, gif.url)
    }

    @Test
    fun parseGifAfterReplyMentionShiftsRange() {
        val url = "https://media.giphy.com/media/abc123/giphy.gif"
        val title = "[Wow GIF]"
        val prefix = "@CoolUser "
        val start = prefix.length
        val end = start + title.length - 1
        val raw = "@display-name=Replier;emotes=;gifs=$start-$end|abc123|$url;id=gif-reply;" +
            "reply-parent-display-name=CoolUser;reply-parent-msg-body=Hi;reply-parent-msg-id=abc;" +
            "reply-parent-user-login=cooluser;tmi-sent-ts=1700000000000 " +
            ":replier!replier@replier.tmi.twitch.tv PRIVMSG #channel :$prefix$title"
        val message = IrcMessageParser.parsePrivMsg(raw)
        assertEquals(title, message!!.rawText)
        val gif = message.parts.single() as ChatPart.Gif
        assertEquals(title, gif.name)
        assertEquals(url, gif.url)
    }

    @Test
    fun parseGifTagKeepsCommaInsideUrl() {
        val spans = IrcEmoteParser.parseGifTag(
            "0-4|id1|https://example.com/a.gif?x=1,2,7-10|id2|https://example.com/b.gif",
        )
        assertEquals(2, spans.size)
        assertEquals("https://example.com/a.gif?x=1,2", spans[0].url)
        assertEquals(0, spans[0].start)
        assertEquals(4, spans[0].endInclusive)
        assertEquals("https://example.com/b.gif", spans[1].url)
        assertEquals(7, spans[1].start)
    }
}
