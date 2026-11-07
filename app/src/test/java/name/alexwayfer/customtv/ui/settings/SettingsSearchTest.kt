package name.alexwayfer.customtv.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchTest {
    private val sound = document(1, "Звук упоминаний", "При ответе", "Упоминания", "аудио громкость")
    private val playback = document(2, "Плавный звук", "Плавное изменение громкости", "Воспроизведение")
    private val emotes = document(3, "Эмоуты", "Эмоуты от 7TV", "Чат", "смайлы смайлики")

    @Test
    fun blankQueryShowsNoResults() {
        assertTrue(searchSettings(listOf(sound), " \n ").isEmpty())
    }

    @Test
    fun searchIgnoresCaseAndWhitespace() {
        assertEquals(listOf(sound), searchSettings(listOf(sound, playback), "  ЗВУК   упомин  "))
    }

    @Test
    fun searchTreatsYoAndYeAsTheSameLetter() {
        val item = document(4, "Шёпот", "Непрочитанные сообщения", "Уведомления")
        assertEquals(listOf(item), searchSettings(listOf(item), "шепот"))
    }

    @Test
    fun wordsCanMatchDifferentFieldsAndAppearInAnyOrder() {
        assertEquals(listOf(sound), searchSettings(listOf(sound, playback), "ответе звук"))
    }

    @Test
    fun synonymsAndSectionNamesAreSearchable() {
        assertEquals(listOf(sound), searchSettings(listOf(sound, playback), "аудио"))
        assertEquals(listOf(emotes), searchSettings(listOf(emotes, playback), "чат смайлики"))
    }

    @Test
    fun everyWordMustMatch() {
        assertTrue(searchSettings(listOf(sound, playback), "звук клавиатура").isEmpty())
    }

    @Test
    fun exactTitleComesBeforeTitleFragmentsAndDescriptionMatches() {
        val description = document(4, "Оповещения", "Звук", "Уведомления")
        val exact = document(5, "Звук", "", "Упоминания")
        assertEquals(listOf(exact, sound, description), searchSettings(listOf(description, sound, exact), "звук"))
    }

    @Test
    fun selectingAResultKeepsTheQueryAndBackRestoresResults() {
        val results = SettingsSearchState(query = "звук", active = true)
        val selected = selectSettingsSearchResult(results, sound.entry.id)
        assertEquals(SettingsSearchState("звук", false, sound.entry.id), selected)
        assertEquals(results, settingsSearchBack(selected))
    }

    @Test
    fun theSearchWindowHandlesItsOwnBackWithoutInterceptingPageNavigation() {
        assertNull(settingsSearchBack(SettingsSearchState("звук", true)))
        assertNull(settingsSearchBack(SettingsSearchState("звук")))
    }

    @Test
    fun reopeningSearchRetainsTheQueryAndDropsThePreviousTarget() {
        assertEquals(
            SettingsSearchState("звук", true),
            openSettingsSearch(SettingsSearchState("звук", selectedId = sound.entry.id)),
        )
    }

    private fun document(
        titleId: Int,
        title: String,
        hint: String,
        path: String,
        keywords: String = "",
    ) = SettingsSearchDocument(SettingsSearchEntry(SettingsPage.Chat, titleId), title, hint, path, keywords)
}
