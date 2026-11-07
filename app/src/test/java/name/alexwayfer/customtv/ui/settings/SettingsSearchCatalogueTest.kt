package name.alexwayfer.customtv.ui.settings

import android.app.Application
import android.content.res.Configuration
import name.alexwayfer.customtv.BuildConfig
import name.alexwayfer.customtv.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SettingsSearchCatalogueTest {
    @Test
    fun catalogueHasUniqueIdsAndCompleteEnglishAndRussianResources() {
        val entries = settingsSearchEntries("viewer", sdkInt = 36)
        assertEquals(entries.size, entries.map { it.id }.toSet().size)
        val application = RuntimeEnvironment.getApplication()
        listOf("en", "ru").forEach { language ->
            val configuration = Configuration(application.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(language))
            }
            val resources = application.createConfigurationContext(configuration).resources
            entries.forEach { entry ->
                val ids = listOfNotNull(entry.title, entry.hint, entry.keywords, entry.anchorTitle, entry.anchorHint)
                ids.forEach { id ->
                    val value = resources.getString(id)
                    assertTrue("Empty search string: $id", value.isNotBlank())
                    assertFalse("Unformatted search string: $value", Regex("%\\d+\\$").containsMatchIn(value))
                }
            }
        }
    }

    @Test
    fun systemSettingsAreOfferedOnlyOnSupportedAndroidVersions() {
        val old = settingsSearchEntries(null, sdkInt = 30)
        val links = settingsSearchEntries(null, sdkInt = 31)
        val language = settingsSearchEntries(null, sdkInt = 33)
        assertFalse(old.any { it.title == R.string.app_links_setting })
        assertTrue(links.any { it.title == R.string.app_links_setting })
        assertFalse(links.any { it.title == R.string.settings_section_language })
        assertTrue(language.any { it.title == R.string.settings_section_language })
    }

    @Test
    fun freeSearchCannotOpenPremiumOnlyPages() {
        if (BuildConfig.PREMIUM) return
        val allowed = setOf(SettingsPage.Main, SettingsPage.Chat, SettingsPage.Mentions, SettingsPage.Playback,
            SettingsPage.ChatterLabels, SettingsPage.Gestures, SettingsPage.Licenses)
        assertTrue(settingsSearchEntries("viewer", sdkInt = 36).all { it.page in allowed })
    }

    @Test
    fun experimentalPageRequiresAnAccount() {
        assertFalse(settingsSearchEntries(null, sdkInt = 36).any { it.page == SettingsPage.Experimental })
        assertEquals(BuildConfig.PREMIUM,
            settingsSearchEntries("viewer", sdkInt = 36).any { it.page == SettingsPage.Experimental })
    }

    @Test
    fun dependentControlsLeadToTheirAlwaysVisibleParent() {
        val entries = settingsSearchEntries(null, sdkInt = 36)
        assertEquals(R.string.mention_vibration,
            entries.single { it.title == R.string.mention_vibration_length }.anchorTitle)
        assertEquals(R.string.mention_sound,
            entries.single { it.title == R.string.mention_sound_choice }.anchorTitle)
        val providers = entries.filter { it.title == R.string.third_party_emotes }
        assertEquals(3, providers.size)
        assertEquals(3, providers.map { it.anchorHint }.toSet().size)
    }
}
