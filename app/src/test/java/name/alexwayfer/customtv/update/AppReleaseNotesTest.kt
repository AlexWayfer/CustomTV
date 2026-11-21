package name.alexwayfer.customtv.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppReleaseNotesTest {
    @Test
    fun theAppLanguageSelectsItsOwnList() {
        val notes = parseAppReleaseNotes("*   Restore playback\r\n\r\n### Russian\r\n- Восстановлено воспроизведение")

        assertEquals(listOf("Restore playback"), notes?.forLanguage("en"))
        assertEquals(listOf("Восстановлено воспроизведение"), notes?.forLanguage("ru"))
    }

    @Test
    fun anOlderReleaseFallsBackToEnglish() {
        val notes = parseAppReleaseNotes("* Fix chat\n* Fix playback")

        assertEquals(listOf("Fix chat", "Fix playback"), notes?.forLanguage("ru"))
    }

    @Test
    fun emptyListsHaveNoNotes() {
        assertNull(parseAppReleaseNotes("### Russian\n*   "))
    }

    @Test
    fun anotherReleaseStartsCollapsedAndCanBeOpened() {
        assertEquals(43L, toggleAppReleaseNotes(expandedReleaseId = 42, releaseId = 43))
        assertNull(toggleAppReleaseNotes(expandedReleaseId = 43, releaseId = 43))
    }
}
