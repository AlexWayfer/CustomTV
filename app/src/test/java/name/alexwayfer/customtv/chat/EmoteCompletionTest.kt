package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmoteCompletionTest {
    @Test
    fun aColonAloneAsksWithAnEmptyQuery() {
        assertEquals(EmoteQuery(0, 1, ""), emoteQueryAtCursor(":", cursor = 1))
        assertEquals(EmoteQuery(3, 4, ""), emoteQueryAtCursor("hi :", cursor = 4))
    }

    @Test
    fun aColonFollowedByASpaceIsNotAQuery() {
        assertNull(emoteQueryAtCursor("hi : ", cursor = 5))
    }

    @Test
    fun aColonRightAfterTextIsNotAQuery() {
        assertNull(emoteQueryAtCursor("hello:", cursor = 6))
    }

    @Test
    fun anEmptyQueryOffersFrequentlyUsedEmotesOnly() {
        val emotes = listOf("Kappa", "LUL", "PogChamp", "unused").map { PickerEmote(it, "https://$it") }
        val usage = listOf(
            EmoteUse("LUL", count = 2, usedAtMillis = 5),
            EmoteUse("Kappa", count = 7, usedAtMillis = 1),
            EmoteUse("PogChamp", count = 2, usedAtMillis = 9),
        )
        assertEquals(listOf("Kappa", "PogChamp", "LUL"), matchingPickerEmotes(emotes, "", usage).map { it.name })
        assertEquals(emptyList<String>(), matchingPickerEmotes(emotes, "").map { it.name })
    }

    @Test
    fun choosingAnEmoteAfterABareColonReplacesTheColon() {
        assertEquals(
            EmoteInsertion("hi Kappa ", 9),
            textWithCompletedEmote("hi :", 4, "Kappa", maxLength = 500),
        )
    }

    @Test
    fun charactersAfterAColonAreTheQuery() {
        val query = emoteQueryAtCursor("hi :Kap", cursor = 7)
        assertEquals("Kap", query?.text)
        assertEquals(3, query?.start)
        assertEquals(7, query?.end)
        assertEquals(")", emoteQueryAtCursor(":)", cursor = 2)?.text)
    }

    @Test
    fun aColonInsideAWordIsNotAQuery() {
        assertNull(emoteQueryAtCursor("http://kappa", cursor = 12))
        assertNull(emoteQueryAtCursor("a:K", cursor = 3))
    }

    @Test
    fun suggestionsUsePickerEmotesAndPreferTheEarlierDuplicate() {
        val sections = listOf(
            EmotePickerSection(
                EmotePickerPlace.Channel,
                EmotePickerKind.SevenTv,
                "Quin",
                listOf(PickerEmote("Kappa", "https://channel/kappa"), PickerEmote("Zed", "https://zed")),
            ),
            EmotePickerSection(
                EmotePickerPlace.Global,
                EmotePickerKind.Twitch,
                "",
                listOf(
                    PickerEmote("Kappa", "https://global/kappa"),
                    PickerEmote("KappaPride", "https://pride"),
                    PickerEmote(":)", "https://smile"),
                    PickerEmote("HeyGuys", "https://hey"),
                ),
            ),
        )
        val emotes = pickerEmotesForCompletion(sections)
        assertEquals("https://channel/kappa", emotes.first { it.name == "Kappa" }.url)
        assertEquals(
            listOf("Kappa", "KappaPride"),
            matchingPickerEmotes(emotes, "kap").map { it.name },
        )
        assertEquals(listOf(":)"), matchingPickerEmotes(emotes, ")").map { it.name })
        assertEquals(
            listOf("Kappa", "KappaPride"),
            matchingPickerEmotes(emotes, "appa").map { it.name },
        )
        assertEquals(listOf("KappaPride"), matchingPickerEmotes(emotes, "pride").map { it.name })
        assertEquals(listOf("HeyGuys"), matchingPickerEmotes(emotes, "eyG").map { it.name })
        assertEquals(
            listOf("Kappa", "aKappa"),
            matchingPickerEmotes(
                listOf(PickerEmote("aKappa", "https://a"), PickerEmote("Kappa", "https://k")),
                "kap",
            ).map { it.name },
        )
        assertEquals(8, matchingPickerEmotes((0 until 8).map { PickerEmote("a$it", "https://$it") }, "a").size)
    }

    @Test
    fun usedEmotesLeadAndEachGroupPutsNamesStartingWithTheQueryFirst() {
        val emotes = listOf("weirdPepe", "pepeW", "pepeBad", "pepeHands", "sadPepe", "pepeLaugh", "angryPepe")
            .map { PickerEmote(it, "https://$it") }
        val usage = listOf(
            EmoteUse("weirdPepe", count = 50, usedAtMillis = 1),
            EmoteUse("sadPepe", count = 2, usedAtMillis = 1),
            EmoteUse("pepeBad", count = 3, usedAtMillis = 1),
            EmoteUse("pepeW", count = 7, usedAtMillis = 1),
        )
        assertEquals(
            listOf("pepeW", "pepeBad", "weirdPepe", "sadPepe", "pepeHands", "pepeLaugh", "angryPepe"),
            matchingPickerEmotes(emotes, "pepe", usage).map { it.name },
        )
    }

    @Test
    fun aTypedWordAsksForEmotesOnlyWhenThatSettingIsOn() {
        val text = "привет Ka"
        assertNull(emoteQueryAtCursor(text, cursor = text.length))
        val query = emoteQueryAtCursor(text, cursor = text.length, withoutColon = true)
        assertEquals("Ka", query?.text)
        assertEquals(
            EmoteInsertion("привет Kappa ", text.length - 2 + "Kappa ".length),
            textWithCompletedEmote(text, text.length, "Kappa", maxLength = 500, withoutColon = true),
        )
        assertEquals(
            EmoteInsertion("привет Kappa there", "привет Kappa ".length),
            textWithCompletedEmote(
                "привет Ka there",
                cursor = "привет Ka".length,
                "Kappa",
                maxLength = 500,
                withoutColon = true,
            ),
        )
        assertEquals("Kap", emoteQueryAtCursor("привет :Kap", cursor = "привет :Kap".length, withoutColon = true)?.text)
    }

    @Test
    fun aCommandWordAtTheStartAsksForNoEmotesButLaterWordsDo() {
        assertNull(emoteQueryAtCursor("/us", cursor = 3, withoutColon = true))
        assertEquals("Ka", emoteQueryAtCursor("/user Ka", cursor = 8, withoutColon = true)?.text)
        assertEquals("/D", emoteQueryAtCursor("hi /D", cursor = 5, withoutColon = true)?.text)
    }

    @Test
    fun choosingAnEmoteRemovesTheColonAndLeavesOneSpace() {
        assertEquals(
            EmoteInsertion("hello Kappa ", 12),
            textWithCompletedEmote("hello :Kap", cursor = 10, "Kappa", maxLength = 500),
        )
        assertEquals(
            EmoteInsertion("Kappa ", 6),
            textWithCompletedEmote(":Kap", cursor = 4, "Kappa", maxLength = 500),
        )
        assertNull(textWithCompletedEmote("hi:Kap", cursor = 6, "Kappa", maxLength = 500))
        assertEquals(
            EmoteInsertion("Kappa world", 6),
            textWithCompletedEmote(":Kap world", cursor = 4, "Kappa", maxLength = 500),
        )
        assertEquals(
            EmoteInsertion(":) ", 3),
            textWithCompletedEmote(":)", cursor = 2, ":)", maxLength = 500),
        )
        assertNull(textWithCompletedEmote("hello :Kap", cursor = 10, "Kappa", maxLength = 8))
    }
}
