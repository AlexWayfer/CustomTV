package name.alexwayfer.customtv.update

// scripts/release.ps1 writes the English bullets first and puts the translation under this heading.
private const val RUSSIAN_NOTES_HEADING = "### Russian"
private val noteBullet = Regex("""^[*+-]\s+(.+)$""")

internal data class AppReleaseNotes(
    private val english: List<String>,
    private val russian: List<String>,
) {
    fun forLanguage(language: String): List<String> = if (language == "ru" && russian.isNotEmpty()) russian else english
}

/** GitHub's release body and the notes Mecena returns use the same Markdown. */
internal fun parseAppReleaseNotes(markdown: String): AppReleaseNotes? {
    val lines = markdown.lines()
    val russianStart = lines.indexOfFirst { it.trim() == RUSSIAN_NOTES_HEADING }
    val english = releaseNoteBullets(if (russianStart < 0) lines else lines.take(russianStart))
    val russian = if (russianStart < 0) emptyList() else releaseNoteBullets(lines.drop(russianStart + 1))
    return if (english.isEmpty() && russian.isEmpty()) null else AppReleaseNotes(english, russian)
}

private fun releaseNoteBullets(lines: List<String>): List<String> =
    lines.mapNotNull { noteBullet.matchEntire(it.trim())?.groupValues?.get(1) }

/** Only the release the user opened is expanded. A second tap folds it back up. */
internal fun toggleAppReleaseNotes(expandedReleaseId: Long?, releaseId: Long): Long? =
    releaseId.takeUnless { it == expandedReleaseId }
