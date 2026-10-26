package name.alexwayfer.customtv.ui.components

import name.alexwayfer.customtv.data.ChannelProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChannelPreviewTest {
    @Test
    fun `a known name and avatar show until the profile loads`() {
        val profile = ChannelPreview("anna", "Анна", "https://example.com/anna.png").fallbackProfile()

        assertEquals("anna", profile.login)
        assertEquals("Анна", profile.displayName)
        assertEquals("https://example.com/anna.png", profile.avatarUrl)
    }

    @Test
    fun `without a name the login shows as typed and keys the profile in lower case`() {
        val profile = ChannelPreview("Anna").fallbackProfile()

        assertEquals("anna", profile.login)
        assertEquals("Anna", profile.displayName)
        assertNull(profile.avatarUrl)
    }

    @Test
    fun `a blank name or avatar is none`() {
        val profile = ChannelPreview("anna", " ", "").fallbackProfile()

        assertEquals("anna", profile.displayName)
        assertNull(profile.avatarUrl)
    }

    @Test
    fun `a profile on screen previews its name and avatar`() {
        val profile = ChannelProfile(login = "anna", displayName = "Анна", avatarUrl = "https://example.com/a.png")

        assertEquals(ChannelPreview("anna", "Анна", "https://example.com/a.png"), profile.preview())
    }
}
