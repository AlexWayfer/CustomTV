package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelAssetRulesTest {
    private val all = EmoteSources(sevenTv = true, ffz = true, bttv = true)

    @Test
    fun resultForTheOpenChannelApplies() {
        assertTrue(channelResultApplies(requested = "alpha", current = "alpha"))
    }

    @Test
    fun resultForThePreviousChannelIsDropped() {
        assertFalse(channelResultApplies(requested = "alpha", current = "beta"))
    }

    @Test
    fun resultAfterTheChatClosedIsDropped() {
        assertFalse(channelResultApplies(requested = "alpha", current = null))
    }

    @Test
    fun unknownChannelIdIsNeverReady() {
        assertFalse(emoteSourcesReady(null, all, sevenTvLoaded = true, ffzLoaded = true, bttvLoaded = true))
    }

    @Test
    fun oneMissingEnabledSourceKeepsLoading() {
        assertFalse(emoteSourcesReady("1", all, sevenTvLoaded = true, ffzLoaded = false, bttvLoaded = true))
    }

    @Test
    fun aSourceThatIsOffIsNotWaitedFor() {
        val noFfz = all.copy(ffz = false)

        assertTrue(emoteSourcesReady("1", noFfz, sevenTvLoaded = true, ffzLoaded = false, bttvLoaded = true))
    }

    @Test
    fun allSourcesOffIsReadyOnceTheChannelIsKnown() {
        val none = EmoteSources(sevenTv = false, ffz = false, bttv = false)

        assertTrue(emoteSourcesReady("1", none, sevenTvLoaded = false, ffzLoaded = false, bttvLoaded = false))
    }

    @Test
    fun emptyLoadBeforeTheSourceConfirmsKeepsTheShownEmotes() {
        assertFalse(emoteLoadPublishes(emoteCount = 0, sourceLoaded = false))
    }

    @Test
    fun emptyLoadTheSourceConfirmedReplacesTheShownEmotes() {
        assertTrue(emoteLoadPublishes(emoteCount = 0, sourceLoaded = true))
    }

    @Test
    fun loadedEmotesAreShown() {
        assertTrue(emoteLoadPublishes(emoteCount = 3, sourceLoaded = false))
    }

    @Test
    fun turningASourceOnLoadsIt() {
        assertTrue(emoteSourceTurnedOn(all.copy(bttv = false), all))
    }

    @Test
    fun turningASourceOffLoadsNothing() {
        assertFalse(emoteSourceTurnedOn(all, all.copy(bttv = false)))
    }

    @Test
    fun unchangedSourcesLoadNothing() {
        assertFalse(emoteSourceTurnedOn(all, all))
    }

    @Test
    fun sourcesFollowTheSettingsAndDefaultToAllOn() {
        assertEquals(all, emoteSources(null))
        assertEquals(
            EmoteSources(sevenTv = false, ffz = true, bttv = true),
            emoteSources(AppSettings(sevenTvEmotes = false)),
        )
    }
}
