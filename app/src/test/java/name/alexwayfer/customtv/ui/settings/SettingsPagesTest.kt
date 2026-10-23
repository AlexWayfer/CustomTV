package name.alexwayfer.customtv.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsPagesTest {
    @Test
    fun updatesOpenOnTheMainList() {
        assertEquals(SettingsPage.Main, settingsPageFor(SettingsScrollTarget.Updates))
    }

    @Test
    fun streamNotificationsOpenTheirOwnPage() {
        assertEquals(SettingsPage.Notifications, settingsPageFor(SettingsScrollTarget.Notifications))
    }

    @Test
    fun aiPortraitsOpenTheirOwnPage() {
        assertEquals(SettingsPage.ChatterPortraits, settingsPageFor(SettingsScrollTarget.ChatterPortraits))
    }

    @Test
    fun whisperSettingsOpenTheNotificationsPageAtTheirBlock() {
        assertEquals(SettingsPage.Notifications, settingsPageFor(SettingsScrollTarget.Whispers))
        assertTrue(settingsScrollsToBlock(SettingsScrollTarget.Whispers))
    }

    @Test
    fun updatesScrollTheMainListToTheirBlock() {
        assertTrue(settingsScrollsToBlock(SettingsScrollTarget.Updates))
    }

    @Test
    fun aTargetThatIsAWholePageOpensItAtTheTop() {
        assertFalse(settingsScrollsToBlock(SettingsScrollTarget.Notifications))
        assertFalse(settingsScrollsToBlock(SettingsScrollTarget.ChatterPortraits))
        assertFalse(settingsScrollsToBlock(SettingsScrollTarget.HiddenCategories))
    }

    @Test
    fun backFromAHiddenCategoryReturnsToTheirPage() {
        assertEquals(SettingsPage.HiddenCategories, settingsPageFor(SettingsScrollTarget.HiddenCategories))
    }

    @Test
    fun settingsTappedAgainOnASubpageGoesBackToTheMainList() {
        assertEquals(SettingsReselect.BackToMain, settingsReselect(SettingsPage.Chat))
        assertEquals(SettingsReselect.BackToMain, settingsReselect(SettingsPage.Licenses))
    }

    @Test
    fun settingsTappedAgainOnTheMainListScrollsItToTheTop() {
        assertEquals(SettingsReselect.ScrollToTop, settingsReselect(SettingsPage.Main))
    }
}
