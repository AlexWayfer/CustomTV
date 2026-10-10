package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmoteOriginTest {
    @Test
    fun readsTwitchEmoteIdFromAnimatedAndStaticUrls() {
        assertEquals(
            "emotesv2_3a5b59f49ec2433bb76a0071ec4997bb",
            twitchEmoteId(
                "https://static-cdn.jtvnw.net/emoticons/v2/emotesv2_3a5b59f49ec2433bb76a0071ec4997bb/animated/dark/2.0",
            ),
        )
        assertEquals("25", twitchEmoteId("https://static-cdn.jtvnw.net/emoticons/v2/25/default/dark/2.0"))
        assertNull(twitchEmoteId("https://cdn.betterttv.net/emote/abc/2x.webp"))
    }

    @Test
    fun describesTwitchEmoteKinds() {
        assertEquals(EmoteOrigin.TwitchGlobal, twitchEmoteOrigin("GLOBALS", null, null))
        assertEquals(
            EmoteOrigin.TwitchSubscription(1, "xQc"),
            twitchEmoteOrigin("SUBSCRIPTIONS", "TIER_1", "xQc"),
        )
        assertEquals(
            EmoteOrigin.TwitchSubscription(3, "Channel"),
            twitchEmoteOrigin("SUBSCRIPTIONS", "3000", "Channel"),
        )
        assertEquals(
            EmoteOrigin.TwitchSubscription(null, "Channel"),
            twitchEmoteOrigin("SUBSCRIPTIONS", null, "Channel"),
        )
        assertEquals(EmoteOrigin.TwitchFollower("Name"), twitchEmoteOrigin("FOLLOWER", null, "Name"))
        assertEquals(EmoteOrigin.TwitchBits("Name"), twitchEmoteOrigin("BITS_BADGE_TIERS", null, "Name"))
        assertEquals(EmoteOrigin.TwitchPrime("Name"), twitchEmoteOrigin("PRIME", null, "Name"))
        assertEquals(EmoteOrigin.TwitchChannel("Name"), twitchEmoteOrigin("LIMITED_TIME", null, "Name"))
        assertEquals(EmoteOrigin.Twitch, twitchEmoteOrigin(null, null, null))
    }

    @Test
    fun readsLibraryFromEmoteHost() {
        assertEquals(EmoteLibrary.SevenTv, emoteLibraryFromUrl("https://cdn.7tv.app/emote/abc/2x.webp"))
        assertEquals(EmoteLibrary.Bttv, emoteLibraryFromUrl("https://cdn.betterttv.net/emote/abc/2x.webp"))
        assertEquals(
            EmoteLibrary.Ffz,
            emoteLibraryFromUrl("https://cdn.frankerfacez.com/emote/abc/4"),
        )
        assertNull(emoteLibraryFromUrl("https://static-cdn.jtvnw.net/emoticons/v2/25/animated/dark/2.0"))
    }

    @Test
    fun giantRewardUrlStillIdentifiesTheEmoteSource() {
        val ffz = FfzGiantEmote.highResolutionUrl("https://cdn.frankerfacez.com/emote/omega/2")
        assertEquals(EmoteLibrary.Ffz, emoteLibraryFromUrl(ffz))
        assertNull(twitchEmoteId(ffz))

        val animatedFfz = FfzGiantEmote.highResolutionUrl(
            "https://cdn.frankerfacez.com/emote/ffzw/animated/2",
        )
        assertEquals(EmoteLibrary.Ffz, emoteLibraryFromUrl(animatedFfz))

        val sevenTv = FfzGiantEmote.highResolutionUrl("https://cdn.7tv.app/emote/abc/2x.webp")
        assertEquals(EmoteLibrary.SevenTv, emoteLibraryFromUrl(sevenTv))

        val bttv = FfzGiantEmote.highResolutionUrl(
            "https://cdn.betterttv.net/emote/abc/2x.webp",
        )
        assertEquals(EmoteLibrary.Bttv, emoteLibraryFromUrl(bttv))

        val twitch = FfzGiantEmote.highResolutionUrl(
            "https://static-cdn.jtvnw.net/emoticons/v2/25/default/dark/2.0",
        )
        assertEquals("25", twitchEmoteId(twitch))
        assertNull(emoteLibraryFromUrl(twitch))
    }

    @Test
    fun prefersTheCurrentChannelOverAGlobalLibraryEmote() {
        assertEquals(
            EmoteOrigin.LibraryChannel(EmoteLibrary.SevenTv, "xQc"),
            libraryEmoteOrigin(EmoteLibrary.SevenTv, onThisChannel = true, "xQc", global = true),
        )
        assertEquals(
            EmoteOrigin.LibraryGlobal(EmoteLibrary.Bttv),
            libraryEmoteOrigin(EmoteLibrary.Bttv, onThisChannel = false, "xQc", global = true),
        )
        assertEquals(
            EmoteOrigin.Library(EmoteLibrary.Ffz),
            libraryEmoteOrigin(EmoteLibrary.Ffz, onThisChannel = false, "", global = false),
        )
    }
}
