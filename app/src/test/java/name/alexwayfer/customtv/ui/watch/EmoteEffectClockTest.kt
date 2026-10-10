package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Test

class EmoteEffectClockTest {
    @Test
    fun emotesDrawnOnDifferentDisplayFramesOfOneEffectFrameShareTheTime() {
        val start = 1_000L * EMOTE_EFFECT_FRAME_NANOS
        assertEquals(start, emoteEffectTime(start))
        assertEquals(start, emoteEffectTime(start + 8_333_333L))
        assertEquals(start, emoteEffectTime(start + EMOTE_EFFECT_FRAME_NANOS - 1))
    }

    @Test
    fun theNextEffectFrameStartsAtItsBoundary() {
        val start = 1_000L * EMOTE_EFFECT_FRAME_NANOS
        assertEquals(start + EMOTE_EFFECT_FRAME_NANOS, emoteEffectTime(start + EMOTE_EFFECT_FRAME_NANOS))
    }

    @Test
    fun theSleepEndsAtTheNextEffectFrame() {
        val start = 1_000L * EMOTE_EFFECT_FRAME_NANOS
        assertEquals(EMOTE_EFFECT_FRAME_NANOS, nanosUntilNextEmoteEffectFrame(start))
        assertEquals(1L, nanosUntilNextEmoteEffectFrame(start + EMOTE_EFFECT_FRAME_NANOS - 1))
    }
}
