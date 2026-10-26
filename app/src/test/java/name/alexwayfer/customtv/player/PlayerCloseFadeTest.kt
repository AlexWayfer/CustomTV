package name.alexwayfer.customtv.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerCloseFadeTest {
    @Test
    fun withSmoothSoundThePageFadesTheSoundOverTheFadeLength() {
        assertEquals(PLAYER_SOUND_FADE_MS, playerSoundFadeMs(smoothSound = true))
    }

    @Test
    fun withoutSmoothSoundThePageDoesNotFade() {
        assertEquals(0L, playerSoundFadeMs(smoothSound = false))
    }

    @Test
    fun aClosedPlayerWithSmoothSoundKeepsItsPageLongerThanTheFade() {
        assertEquals(PLAYER_SOUND_FADE_MS + 50L, playerCloseDelayMs(smoothSound = true))
    }

    @Test
    fun aClosedPlayerWithoutSmoothSoundDropsItsPageAtOnce() {
        assertEquals(0L, playerCloseDelayMs(smoothSound = false))
    }
}
