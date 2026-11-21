package name.alexwayfer.customtv.player

import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import name.alexwayfer.customtv.data.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProximityScreenOffTest {
    @Test fun aSupportedPlayerUsesProximityScreenOffByDefault() {
        assertTrue(
            shouldBlankPlayerScreen(AppSettings().blankScreenOnProximity, inPictureInPicture = false, supported = true)
        )
    }

    @Test fun theWakeLockIsNotUsedBeforeTheStoredSettingArrivesOrWhenItIsOff() {
        assertFalse(shouldBlankPlayerScreen(null, inPictureInPicture = false, supported = true))
        assertFalse(shouldBlankPlayerScreen(false, inPictureInPicture = false, supported = true))
    }

    @Test fun theWakeLockIsNotUsedInPictureInPictureOrWithoutSupport() {
        assertFalse(shouldBlankPlayerScreen(true, inPictureInPicture = true, supported = true))
        assertFalse(shouldBlankPlayerScreen(true, inPictureInPicture = false, supported = false))
    }

    @Test fun aBriefCoverLeavesTheScreenOnAndTheNextCoverWaitsTheFullThreeSeconds() = runTest {
        val readings = Channel<Boolean>(Channel.UNLIMITED)
        val changes = mutableListOf<Boolean>()
        backgroundScope.launch { readings.receiveAsFlow().delayedProximityScreenOff().collect { changes.add(it) } }
        runCurrent()

        readings.send(true)
        advanceTimeBy(2_999.milliseconds)
        readings.send(false)
        runCurrent()
        assertEquals(listOf(false), changes)

        readings.send(true)
        advanceTimeBy(2_999.milliseconds)
        runCurrent()
        assertEquals(listOf(false), changes)
        advanceTimeBy(1.milliseconds)
        runCurrent()
        assertEquals(listOf(false, true), changes)
    }

    @Test fun repeatedNearReadingsKeepTheDeadlineAndUncoveringTurnsTheScreenOnImmediately() = runTest {
        val readings = Channel<Boolean>(Channel.UNLIMITED)
        val changes = mutableListOf<Boolean>()
        backgroundScope.launch { readings.receiveAsFlow().delayedProximityScreenOff().collect { changes.add(it) } }
        runCurrent()

        readings.send(true)
        advanceTimeBy(1_500.milliseconds)
        readings.send(true)
        advanceTimeBy(1_500.milliseconds)
        runCurrent()
        assertEquals(listOf(true), changes)

        readings.send(false)
        runCurrent()
        assertEquals(listOf(true, false), changes)
    }

    @Test fun theNearBoundaryMatchesTheDisplayThresholdAndTheSensorRange() {
        assertTrue(proximityCovered(distance = 0f, maximumRange = 5f))
        assertFalse(proximityCovered(distance = 5f, maximumRange = 5f))
        assertFalse(proximityCovered(distance = 5f, maximumRange = 20f))
        assertFalse(proximityCovered(distance = 3f, maximumRange = 3f))
    }
}
