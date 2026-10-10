package name.alexwayfer.customtv.channel

import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelLoginTest {
    @Test
    fun uppercaseSchemeAndWwwStillBecomeALogin() {
        assertEquals("foo", normalizeChannelInput("HTTPS://WWW.Twitch.tv/Foo"))
        assertEquals("foo", normalizeChannelInput("HTTP://www.twitch.tv/foo/videos"))
        assertEquals("xqc", normalizeChannelInput("https://twitch.tv/xQc"))
    }

    @Test
    fun labelKeepsDisplayNameWhenEqualIgnoringCase() {
        assertEquals("xQc", displayNameLabel("xQc", "xqc"))
        assertEquals("Pokimane", displayNameLabel("Pokimane", "pokimane"))
    }

    @Test
    fun nameAddsCollaboratorCountAfterASpace() {
        assertEquals("xQc +4", channelNameWithCollaborators("xQc", 4))
        assertEquals("あくたん (akirosenthal) +1", channelNameWithCollaborators("あくたん (akirosenthal)", 1))
        assertEquals("xQc", channelNameWithCollaborators("xQc", null))
        assertEquals("xQc", channelNameWithCollaborators("xQc", 0))
    }

    @Test
    fun labelAddsLoginWhenDisplayNameDiffers() {
        assertEquals("あくたん (akirosenthal)", displayNameLabel("あくたん", "akirosenthal"))
        assertEquals("xQc (xqcow)", displayNameLabel("xQc", "xqcow"))
    }
}
