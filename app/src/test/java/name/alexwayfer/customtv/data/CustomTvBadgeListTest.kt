package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomTvBadgeListTest {
    private val list = parseCustomTvBadges("""{"premium":[42,7],"free":[100,5,42]}""")!!

    @Test
    fun premiumAndFreeUsersShowTheirEdition() {
        assertEquals(CustomTvBadgeEdition.Premium, list.editionOf("7"))
        assertEquals(CustomTvBadgeEdition.Free, list.editionOf("5"))
        assertEquals(CustomTvBadgeEdition.Free, list.editionOf("100"))
    }

    @Test
    fun userInBothListsShowsPremium() {
        assertEquals(CustomTvBadgeEdition.Premium, list.editionOf("42"))
    }

    @Test
    fun unknownOrMissingUserShowsNone() {
        assertNull(list.editionOf("8"))
        assertNull(list.editionOf(null))
        assertNull(list.editionOf("not-a-number"))
        assertNull(CustomTvBadgeList().editionOf("7"))
    }

    @Test
    fun ownChangeMovesTheUserBetweenEditionsOrTakesTheBadgeOff() {
        val upgraded = list.with("5", CustomTvBadgeEdition.Premium)
        assertEquals(CustomTvBadgeEdition.Premium, upgraded.editionOf("5"))

        val added = list.with("3", CustomTvBadgeEdition.Free)
        assertEquals(CustomTvBadgeEdition.Free, added.editionOf("3"))
        assertEquals(CustomTvBadgeEdition.Free, added.editionOf("5"))

        val removed = list.with("42", null)
        assertNull(removed.editionOf("42"))
        assertEquals(CustomTvBadgeEdition.Premium, removed.editionOf("7"))

        assertEquals(CustomTvBadgeEdition.Free, list.with("x", null).editionOf("5"))
    }

    @Test
    fun answerWithoutBothListsIsNotAList() {
        assertNull(parseCustomTvBadges("""{"premium":[1]}"""))
        assertNull(parseCustomTvBadges("not json"))
        assertNull(CustomTvBadgeList().with("1", null).editionOf("1"))
    }

    @Test
    fun registrationAnswerNamesItsEdition() {
        assertEquals(
            CustomTvBadgeEdition.Premium,
            parseCustomTvBadgeEdition("""{"twitch_id":42,"edition":"premium"}"""),
        )
        assertEquals(CustomTvBadgeEdition.Free, parseCustomTvBadgeEdition("""{"twitch_id":42,"edition":"free"}"""))
        assertNull(parseCustomTvBadgeEdition("""{"twitch_id":42,"edition":"gold"}"""))
        assertNull(parseCustomTvBadgeEdition("[]"))
    }

    @Test
    fun listLoadsAgainAnHourAfterTheLastLoad() {
        val hour = CUSTOM_TV_BADGES_RELOAD.inWholeMilliseconds
        assertTrue(customTvBadgesDue(loadedAtMillis = null, nowMillis = 0))
        assertFalse(customTvBadgesDue(loadedAtMillis = 1_000, nowMillis = 1_000 + hour - 1))
        assertTrue(customTvBadgesDue(loadedAtMillis = 1_000, nowMillis = 1_000 + hour))
    }
}
