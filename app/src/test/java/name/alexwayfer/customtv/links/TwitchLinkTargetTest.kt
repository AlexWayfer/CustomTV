package name.alexwayfer.customtv.links

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TwitchLinkTargetTest {
    @Test
    fun aChannelPageOpensTheChannelOnEveryTwitchHost() {
        val channel = TwitchLinkTarget.Channel("lirik")
        assertEquals(channel, twitchLinkTarget("https://www.twitch.tv/lirik"))
        assertEquals(channel, twitchLinkTarget("https://twitch.tv/lirik"))
        assertEquals(channel, twitchLinkTarget("https://m.twitch.tv/lirik"))
        assertEquals(channel, twitchLinkTarget("http://www.twitch.tv/lirik"))
    }

    @Test
    fun aChannelLinkKeepsItsLoginWithoutTheSlashQueryOrCase() {
        val channel = TwitchLinkTarget.Channel("lirik")
        assertEquals(channel, twitchLinkTarget("https://www.twitch.tv/lirik/"))
        assertEquals(channel, twitchLinkTarget("https://www.twitch.tv/lirik?sr=a#chat"))
        assertEquals(channel, twitchLinkTarget("  https://WWW.Twitch.tv/LIRIK  "))
    }

    @Test
    fun aTabOfTheChannelPageOpensTheChannel() {
        assertEquals(TwitchLinkTarget.Channel("lirik"), twitchLinkTarget("https://www.twitch.tv/lirik/about"))
        assertEquals(TwitchLinkTarget.Channel("lirik"), twitchLinkTarget("https://www.twitch.tv/lirik/videos"))
        assertEquals(TwitchLinkTarget.Channel("lirik"), twitchLinkTarget("https://www.twitch.tv/lirik/schedule"))
    }

    @Test
    fun aRecordingLinkOpensTheRecording() {
        assertEquals(TwitchLinkTarget.Video("2251837463"), twitchLinkTarget("https://www.twitch.tv/videos/2251837463"))
        assertEquals(
            TwitchLinkTarget.Video("2251837463"),
            twitchLinkTarget("https://m.twitch.tv/videos/2251837463?t=1h2m3s"),
        )
    }

    @Test
    fun aRecordingLinkWithoutANumericIdOpensNothing() {
        assertNull(twitchLinkTarget("https://www.twitch.tv/videos/v123"))
        assertNull(twitchLinkTarget("https://www.twitch.tv/videos"))
        assertNull(twitchLinkTarget("https://www.twitch.tv/videos/123/extra"))
    }

    @Test
    fun aClipKeepsTheCaseOfItsSlug() {
        val slug = "NeighborlyShinyZucchiniKAPOW-T-72XOdjOYPJjckN"
        val clip = TwitchLinkTarget.Clip(slug)
        assertEquals(clip, twitchLinkTarget("https://www.twitch.tv/praden/clip/$slug"))
        assertEquals(clip, twitchLinkTarget("https://m.twitch.tv/Praden/clip/$slug/"))
        assertEquals(clip, twitchLinkTarget("https://twitch.tv/praden/clip/$slug?featured=false"))
    }

    @Test
    fun aClipOnTheClipsHostOpensBySlug() {
        val slug = "GracefulSlickDragonflyDancingBaby-bGcDCc38sctjlZYj"
        val clip = TwitchLinkTarget.Clip(slug)
        assertEquals(clip, twitchLinkTarget("https://clips.twitch.tv/$slug"))
        assertEquals(clip, twitchLinkTarget("http://Clips.Twitch.tv/$slug/"))
    }

    @Test
    fun aClipLinkWithoutAClipOpensNothing() {
        assertNull(twitchLinkTarget("https://clips.twitch.tv/"))
        assertNull(twitchLinkTarget("https://clips.twitch.tv/embed?clip=FunnyClipSlug-abc123"))
        assertNull(twitchLinkTarget("https://clips.twitch.tv/FunnyClipSlug-abc123/extra"))
        assertNull(twitchLinkTarget("https://clips.twitch.tv/Funny.Clip"))
        assertNull(twitchLinkTarget("https://www.twitch.tv/lirik/clip"))
        assertNull(twitchLinkTarget("https://www.twitch.tv/lirik/clip/Funny.Clip"))
        assertNull(twitchLinkTarget("https://www.twitch.tv/lirik/clips"))
        assertNull(twitchLinkTarget("https://www.twitch.tv/directory/clip/FunnyClipSlug-abc123"))
    }

    @Test
    fun pagesThatAreNotChannelsOpenInTheBrowser() {
        assertNull(twitchLinkTarget("https://www.twitch.tv/directory"))
        assertNull(twitchLinkTarget("https://www.twitch.tv/directory/category/just-chatting"))
        assertNull(twitchLinkTarget("https://www.twitch.tv/settings/profile"))
        assertNull(twitchLinkTarget("https://www.twitch.tv/p/en/legal/terms-of-service/"))
        assertNull(twitchLinkTarget("https://www.twitch.tv/search?term=lirik"))
        assertNull(twitchLinkTarget("https://www.twitch.tv/activate"))
        assertNull(twitchLinkTarget("https://www.twitch.tv/subs/lirik"))
        assertNull(twitchLinkTarget("https://www.twitch.tv/popout/lirik/chat"))
        assertNull(twitchLinkTarget("https://www.twitch.tv/"))
    }

    @Test
    fun anotherPageOfTheChannelOpensInTheBrowser() {
        assertNull(twitchLinkTarget("https://www.twitch.tv/lirik/report"))
        assertNull(twitchLinkTarget("https://www.twitch.tv/lirik/about/extra"))
    }

    @Test
    fun aPathThatIsNotALoginOpensNothing() {
        assertNull(twitchLinkTarget("https://www.twitch.tv/creator-camp"))
        assertNull(twitchLinkTarget("https://www.twitch.tv/this_login_is_far_too_long_for_twitch"))
    }

    @Test
    fun anotherHostOrSchemeOpensNothing() {
        assertNull(twitchLinkTarget("https://player.twitch.tv/?channel=lirik"))
        assertNull(twitchLinkTarget("https://twitch.tv.example.com/lirik"))
        assertNull(twitchLinkTarget("ftp://www.twitch.tv/lirik"))
        assertNull(twitchLinkTarget("www.twitch.tv/lirik"))
        assertNull(twitchLinkTarget("https://www.twitch.tv/li rik"))
        assertNull(twitchLinkTarget(""))
    }
}
