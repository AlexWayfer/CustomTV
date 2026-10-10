package name.alexwayfer.customtv.ui.home

import name.alexwayfer.customtv.data.ChannelProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class RecentProfilesTest {
    @Test
    fun aCachedDisplayNameReplacesTheStoredLogin() {
        val current = mapOf("xqc" to profile("xqc"))
        val merged = recentProfilesWithCache(current, listOf("xqc")) { profile("xQc") }
        assertEquals("xQc", merged.getValue("xqc").displayName)
    }

    @Test
    fun aMissingCacheEntryKeepsTheRememberedProfile() {
        val remembered = profile("xQc")
        val current = mapOf("xqc" to remembered)
        val merged = recentProfilesWithCache(current, listOf("xqc")) { null }
        assertSame(remembered, merged.getValue("xqc"))
    }

    @Test
    fun anEmptyHistoryKeepsTheCurrentProfiles() {
        val current = mapOf("xqc" to profile("xQc"))
        assertSame(current, recentProfilesWithCache(current, emptyList()) { null })
    }

    private fun profile(displayName: String): ChannelProfile {
        return ChannelProfile(login = "xqc", displayName = displayName, avatarUrl = null)
    }
}
