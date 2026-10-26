package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.chat.EmoteEffects
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmoteEffectColorTest {
    @Test
    fun rainbowAndPartyColorsChangeOverTime() {
        assertTrue(emoteEffectColorMoves(EmoteEffects(rainbow = true)))
        assertTrue(emoteEffectColorMoves(EmoteEffects(party = true)))
    }

    @Test
    fun cursedAndHyperRedColorsStayTheSame() {
        assertFalse(emoteEffectColorMoves(EmoteEffects(cursed = true)))
        assertFalse(emoteEffectColorMoves(EmoteEffects(hyperRed = true)))
    }

    @Test
    fun aStillColorThatWinsOverRainbowKeepsTheColorStill() {
        assertFalse(emoteEffectColorMoves(EmoteEffects(cursed = true, rainbow = true)))
        assertFalse(emoteEffectColorMoves(EmoteEffects(hyperRed = true, party = true)))
    }

    @Test
    fun motionWithoutColorDoesNotChangeTheColor() {
        assertFalse(emoteEffectColorMoves(EmoteEffects(shake = true)))
        assertFalse(emoteEffectColorMoves(EmoteEffects(bounce = true, jam = true)))
    }
}
