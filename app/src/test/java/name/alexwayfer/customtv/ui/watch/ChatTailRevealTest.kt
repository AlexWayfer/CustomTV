package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatTailRevealTest {
    @Test
    fun growingQueueAcceleratesWithoutJumpingToTargetSpeed() {
        val next = chatTailVelocity(1f, 500f, 16f, 300)
        assertTrue(next > 1f)
        assertTrue(next < 5f)
    }

    @Test
    fun approachingTailSlowsDownWithoutResettingVelocity() {
        val next = chatTailVelocity(2f, 10f, 16f, 300)
        assertTrue(next > 0.1f)
        assertTrue(next < 2f)
    }

    @Test
    fun sameDistanceMaintainsSpeedAcrossRowBoundaries() {
        assertEquals(1f, chatTailVelocity(1f, 100f, 16f, 300), 0.0001f)
    }

    @Test
    fun emptyQueueStops() {
        assertEquals(0f, chatTailVelocity(2f, 0f, 16f, 300), 0f)
    }

    @Test
    fun equivalentElapsedTimeGivesEquivalentAcceleration() {
        val oneFrame = chatTailVelocity(1f, 500f, 32f, 300)
        val twoFrames = chatTailVelocity(chatTailVelocity(1f, 500f, 16f, 300), 500f, 16f, 300)
        assertEquals(oneFrame, twoFrames, 0.0001f)
    }

    @Test
    fun oneFrameAt120HzSkipsTheStep() {
        assertFalse(chatTailStepDue(8_333_333L))
    }

    @Test
    fun twoFramesAt120HzTakeTheStep() {
        assertTrue(chatTailStepDue(16_666_666L))
    }

    @Test
    fun earlyFrameAt60HzStillTakesTheStep() {
        assertTrue(chatTailStepDue(15_000_000L))
    }
}
