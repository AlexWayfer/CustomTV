package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PersonalEmotesTest {
    private val none = emptyMap<String, SevenTvEmote>()

    @Test
    fun aSevenTvSetReachesEveryChatterItIsGivenTo() {
        val emotes = PersonalEmotes()
            .apply(PersonalEmoteEvent.SevenTvSetChanged("set", added = mapOf("nnysBop" to emote("bop"))))
            .apply(PersonalEmoteEvent.SevenTvSetAssigned("set", listOf("1", "2")))

        assertEquals(mapOf("nnysBop" to emote("bop")), emotes.forUser("1"))
        assertEquals(mapOf("nnysBop" to emote("bop")), emotes.forUser("2"))
        assertEquals(none, emotes.forUser("3"))
    }

    @Test
    fun aChatterGivenTheSetBeforeItsEmotesGetsThemOnceTheyArrive() {
        val assigned = PersonalEmotes().apply(PersonalEmoteEvent.SevenTvSetAssigned("set", listOf("1")))
        assertEquals(none, assigned.forUser("1"))

        val filled = assigned.apply(PersonalEmoteEvent.SevenTvSetChanged("set", added = mapOf("bop" to emote("bop"))))
        assertEquals(mapOf("bop" to emote("bop")), filled.forUser("1"))
    }

    @Test
    fun aSetChangeRemovesBeforeItAddsSoARenameKeepsTheNewName() {
        val emotes = PersonalEmotes()
            .apply(
                PersonalEmoteEvent.SevenTvSetChanged("set", added = mapOf("old" to emote("e"), "kept" to emote("k"))),
            )
            .apply(PersonalEmoteEvent.SevenTvSetAssigned("set", listOf("1")))
            .apply(
                PersonalEmoteEvent.SevenTvSetChanged("set", added = mapOf("new" to emote("e")), removed = setOf("old")),
            )

        assertEquals(mapOf("kept" to emote("k"), "new" to emote("e")), emotes.forUser("1"))
    }

    @Test
    fun theSameSetGivenTwiceChangesNothing() {
        val once = PersonalEmotes().apply(PersonalEmoteEvent.SevenTvSetAssigned("set", listOf("1")))
        val twice = once.apply(PersonalEmoteEvent.SevenTvSetAssigned("set", listOf("1")))

        assertEquals(once, twice)
        assertEquals(mapOf("1" to listOf("set")), twice.sevenTvUsers)
    }

    @Test
    fun sevenTvWinsOverBttvAndAnEarlierSetOverALaterOne() {
        val emotes = PersonalEmotes()
            .apply(PersonalEmoteEvent.SevenTvSetChanged("first", added = mapOf("Same" to emote("first"))))
            .apply(PersonalEmoteEvent.SevenTvSetChanged("second", added = mapOf("Same" to emote("second"))))
            .apply(PersonalEmoteEvent.SevenTvSetAssigned("first", listOf("1")))
            .apply(PersonalEmoteEvent.SevenTvSetAssigned("second", listOf("1")))
            .apply(PersonalEmoteEvent.BttvUser("1", mapOf("Same" to emote("bttv"), "Pro" to emote("pro"))))

        assertEquals(mapOf("Same" to emote("first"), "Pro" to emote("pro")), emotes.forUser("1"))
        assertEquals(mapOf("Same" to emote("first")), emotes.sevenTvFor("1"))
        assertEquals(mapOf("Same" to emote("bttv"), "Pro" to emote("pro")), emotes.bttvFor("1"))
    }

    @Test
    fun bttvReplacesTheChattersEmotesAndAnEmptyListDropsThem() {
        val first = PersonalEmotes().apply(PersonalEmoteEvent.BttvUser("1", mapOf("a" to emote("a"))))
        val replaced = first.apply(PersonalEmoteEvent.BttvUser("1", mapOf("b" to emote("b"))))
        assertEquals(mapOf("b" to emote("b")), replaced.forUser("1"))

        val dropped = replaced.apply(PersonalEmoteEvent.BttvUser("1", emptyMap()))
        assertEquals(none, dropped.forUser("1"))
        assertEquals(emptyMap<String, Map<String, SevenTvEmote>>(), dropped.bttv)
    }

    @Test
    fun aTurnedOffPlatformLeavesItsEmotesOut() {
        val emotes = PersonalEmotes()
            .apply(PersonalEmoteEvent.SevenTvSetChanged("set", added = mapOf("seven" to emote("7"))))
            .apply(PersonalEmoteEvent.SevenTvSetAssigned("set", listOf("1")))
            .apply(PersonalEmoteEvent.BttvUser("1", mapOf("bttv" to emote("b"))))

        assertEquals(
            mapOf("bttv" to emote("b")),
            emotes.enabled(EmoteSources(sevenTv = false, ffz = true, bttv = true)).forUser("1"),
        )
        assertEquals(
            mapOf("seven" to emote("7")),
            emotes.enabled(EmoteSources(sevenTv = true, ffz = true, bttv = false)).forUser("1"),
        )
        val all = EmoteSources(sevenTv = true, ffz = false, bttv = true)
        assertSame(emotes, emotes.enabled(all))
    }

    @Test
    fun noUserHasNoEmotes() {
        val emotes = PersonalEmotes().apply(PersonalEmoteEvent.BttvUser("1", mapOf("a" to emote("a"))))

        assertEquals(none, emotes.forUser(null))
        assertEquals(none, emotes.forUser(""))
    }

    private fun emote(id: String) = SevenTvEmote(url = "https://cdn.7tv.app/emote/$id/2x.webp", id = id)
}
