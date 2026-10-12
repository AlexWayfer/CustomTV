package name.alexwayfer.customtv.player

import org.junit.Assert.assertEquals
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

    private fun video(id: String) = TwitchPlaybackTarget.Video(
        id = id,
        channel = "alice",
        title = "Title",
        thumbnailUrl = null,
        durationSeconds = 60L,
    )
}
