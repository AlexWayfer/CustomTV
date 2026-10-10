package name.alexwayfer.customtv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerAudioOnlyTest {
    @Test
    fun backgroundWithoutPictureInPicturePlaysSoundOnly() {
        assertTrue(playerAudioOnly(enabled = true, background = true, inPictureInPicture = false, screenUnlocked = true))
    }

    @Test
    fun turnedOffSettingKeepsTheVideoInTheBackground() {
        assertFalse(playerAudioOnly(enabled = false, background = true, inPictureInPicture = false, screenUnlocked = true))
    }

    @Test
    fun pictureInPictureKeepsTheVideo() {
        assertFalse(playerAudioOnly(enabled = true, background = true, inPictureInPicture = true, screenUnlocked = true))
    }

    @Test
    fun lockedScreenHidesPictureInPictureAndPlaysSoundOnly() {
        assertTrue(playerAudioOnly(enabled = true, background = true, inPictureInPicture = true, screenUnlocked = false))
    }

    @Test
    fun lockedScreenPlaysSoundOnlyBeforeTheActivityStops() {
        assertTrue(playerAudioOnly(enabled = true, background = false, inPictureInPicture = true, screenUnlocked = false))
    }

    @Test
    fun litLockScreenKeepsPictureInPictureOnSoundOnly() {
        // Picking the phone up lights the lock screen; the picture-in-picture window stays hidden behind it.
        assertTrue(playerAudioOnly(enabled = true, background = true, inPictureInPicture = true, screenUnlocked = false))
    }

    @Test
    fun turnedOffSettingKeepsTheVideoWithTheScreenOff() {
        assertFalse(playerAudioOnly(enabled = false, background = true, inPictureInPicture = true, screenUnlocked = false))
    }

    @Test
    fun foregroundKeepsTheVideo() {
        assertFalse(playerAudioOnly(enabled = true, background = false, inPictureInPicture = false, screenUnlocked = true))
    }

    @Test
    fun livePlaylistGetsTheAudioOnlyParameter() {
        assertEquals(
            "https://usher.ttvnw.net/api/channel/hls/quin69.m3u8?sig=a&token=b%7D&allow_source=true&allow_audio_only=true",
            usherUrlWithAudioOnly("https://usher.ttvnw.net/api/channel/hls/quin69.m3u8?sig=a&token=b%7D&allow_source=true"),
        )
    }

    @Test
    fun recordingPlaylistGetsTheAudioOnlyParameter() {
        assertEquals(
            "https://usher.ttvnw.net/vod/123.m3u8?sig=a&allow_audio_only=true",
            usherUrlWithAudioOnly("https://usher.ttvnw.net/vod/123.m3u8?sig=a"),
        )
    }

    @Test
    fun aRefusedAudioOnlyParameterIsTurnedOn() {
        assertEquals(
            "https://usher.ttvnw.net/api/channel/hls/x.m3u8?allow_audio_only=true&p=1",
            usherUrlWithAudioOnly("https://usher.ttvnw.net/api/channel/hls/x.m3u8?allow_audio_only=false&p=1"),
        )
    }

    @Test
    fun playlistWithoutQueryGetsOne() {
        assertEquals(
            "https://usher.ttvnw.net/api/channel/hls/x.m3u8?allow_audio_only=true",
            usherUrlWithAudioOnly("https://usher.ttvnw.net/api/channel/hls/x.m3u8"),
        )
    }

    @Test
    fun playlistThatAlreadyAllowsItIsLeftAlone() {
        assertNull(usherUrlWithAudioOnly("https://usher.ttvnw.net/api/channel/hls/x.m3u8?allow_audio_only=true"))
    }

    @Test
    fun otherHostsAreLeftAlone() {
        assertNull(usherUrlWithAudioOnly("https://video-weaver.example.ttvnw.net/v1/playlist/abc.m3u8"))
        assertNull(usherUrlWithAudioOnly("https://player.twitch.tv/?channel=x"))
    }

    @Test
    fun otherUsherPathsAreLeftAlone() {
        assertNull(usherUrlWithAudioOnly("https://usher.ttvnw.net/api/other/x.json"))
    }
}
