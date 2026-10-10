package name.alexwayfer.customtv.ui.settings

import org.junit.Assert.assertEquals
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
}
