package name.alexwayfer.customtv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TwitchPlaybackTargetTest {
    @Test
    fun channelUsesTheLiveEmbedParameter() {
        assertEquals(
            "https://player.twitch.tv/?channel=alice&parent=player.twitch.tv&autoplay=true&muted=false",
            playerEmbedUrl(TwitchPlaybackTarget.Channel("alice")),
        )
    }

    @Test
    fun videoUsesOneRequiredVPrefix() {
        val expected =
            "https://player.twitch.tv/?video=v123&parent=player.twitch.tv&autoplay=true&muted=false"
        assertEquals(expected, playerEmbedUrl(video("123")))
        assertEquals(expected, playerEmbedUrl(video("v123")))
    }

    @Test
    fun clipUsesTheClipEmbedPage() {
        val clip = video("AwkwardClip-abc_1").copy(clipSlug = "AwkwardClip-abc_1")
        assertEquals(
            "https://clips.twitch.tv/embed?clip=AwkwardClip-abc_1&parent=player.twitch.tv&autoplay=true&muted=false",
            playerEmbedUrl(clip),
        )
    }

    @Test
    fun clipAndRecordingKeysDiffer() {
        assertEquals("video:123", video("v123").key)
        assertEquals("clip:Slug", video("Slug").copy(clipSlug = "Slug").key)
    }

    @Test
    fun clipPagesArePlayerHosts() {
        assertTrue(isPlayerHost("player.twitch.tv"))
        assertTrue(isPlayerHost("embed.player.twitch.tv"))
        assertTrue(isPlayerHost("Clips.Twitch.tv"))
        assertFalse(isPlayerHost("www.twitch.tv"))
        assertFalse(isPlayerHost("clips.twitch.tv.example.com"))
    }

    private fun video(id: String) = TwitchPlaybackTarget.Video(
        id = id,
        channel = "alice",
        title = "Title",
        thumbnailUrl = null,
        durationSeconds = 60L,
    )
}
