package name.alexwayfer.customtv.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SectionVisitTest {
    private fun visitAfter(from: AppSection, to: AppSection, profileOpenedFrom: AppSection?): Int =
        SectionVisit(from, settingsVisit = 3).moveTo(to, profileOpenedFrom).settingsVisit

    @Test
    fun `back from a profile the settings opened returns to the same visit`() {
        assertEquals(3, visitAfter(AppSection.ChannelProfile, AppSection.Settings, AppSection.Settings))
    }

    @Test
    fun `back from a profile opened elsewhere starts a new visit`() {
        assertEquals(4, visitAfter(AppSection.ChannelProfile, AppSection.Settings, AppSection.Home))
        assertEquals(4, visitAfter(AppSection.ChannelProfile, AppSection.Settings, null))
    }

    @Test
    fun `the settings opened from another section start a new visit`() {
        assertEquals(4, visitAfter(AppSection.Home, AppSection.Settings, null))
        assertEquals(4, visitAfter(AppSection.Whispers, AppSection.Settings, null))
        assertEquals(4, visitAfter(AppSection.Explore, AppSection.Settings, AppSection.Settings))
    }

    @Test
    fun `staying in the settings or leaving them keeps the visit`() {
        assertEquals(3, visitAfter(AppSection.Settings, AppSection.Settings, null))
        assertEquals(3, visitAfter(AppSection.Settings, AppSection.ChannelProfile, null))
        assertEquals(3, visitAfter(AppSection.Settings, AppSection.Home, null))
    }

    @Test
    fun `the move takes the new section`() {
        assertEquals(
            AppSection.ChannelProfile,
            SectionVisit(AppSection.Settings).moveTo(AppSection.ChannelProfile, null).section,
        )
    }
}
