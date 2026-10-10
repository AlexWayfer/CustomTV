package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Test

class KeywordPhrasesTest {
    private val raid = KeywordPhrase("raid", wholeWord = true)
    private val gg = KeywordPhrase("gg", wholeWord = false)

    @Test
    fun aBlankEntryIsNotAdded() {
        assertEquals(listOf(raid), keywordPhrasesAfterAdd(listOf(raid), "  ", wholeWord = false))
    }

    @Test
    fun aPhraseKeepsItsSpacesAndModeAndDropsACaseDuplicate() {
        val added = keywordPhrasesAfterAdd(emptyList(), "  go live  ", wholeWord = false)
        assertEquals(listOf(KeywordPhrase("go live", wholeWord = false)), added)
        assertEquals(added, keywordPhrasesAfterAdd(added, "Go Live", wholeWord = true))
    }

    @Test
    fun removingMatchesIgnoringCase() {
        assertEquals(listOf(gg), keywordPhrasesAfterRemove(listOf(KeywordPhrase("Raid", wholeWord = true), gg), "raid"))
    }

    @Test
    fun changingTheModeKeepsThePhraseInItsPlace() {
        assertEquals(
            listOf(KeywordPhrase("raid", wholeWord = false), gg),
            keywordPhrasesAfterWholeWordChange(listOf(raid, gg), "RAID", wholeWord = false),
        )
    }

    @Test
    fun storedPhrasesRoundTripWithTheirModeAndDropBlanks() {
        val stored = encodeKeywordPhrases(listOf(raid, gg))
        assertEquals(listOf(raid, gg), decodeKeywordPhrases(stored))
        assertEquals(
            listOf(gg),
            decodeKeywordPhrases("""[{"text":"gg","wholeWord":false},{"text":"","wholeWord":true},{"text":"GG","wholeWord":true}]"""),
        )
        assertEquals(emptyList<KeywordPhrase>(), decodeKeywordPhrases(null))
        assertEquals(emptyList<KeywordPhrase>(), decodeKeywordPhrases("not json"))
    }

    @Test
    fun aPlainStringSavedBeforeTheModeMatchesWholeWords() {
        assertEquals(listOf(raid), decodeKeywordPhrases("""["raid","","Raid"]"""))
    }
}
