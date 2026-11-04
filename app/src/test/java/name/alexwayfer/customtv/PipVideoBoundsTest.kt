package name.alexwayfer.customtv

import androidx.compose.ui.unit.IntRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PipVideoBoundsTest {
    @Test
    fun portraitPlayerExcludesTheLetterboxingAboveAndBelowVideo() {
        assertEquals(IntRect(0, 908, 1080, 1515), pipVideoBounds(IntRect(0, 0, 1080, 2424)))
    }

    @Test
    fun widePlayerExcludesTheLetterboxingBesideVideo() {
        assertEquals(IntRect(252, 0, 2172, 1080), pipVideoBounds(IntRect(0, 0, 2424, 1080)))
    }

    @Test
    fun movedMiniPlayerKeepsItsWindowCoordinates() {
        assertEquals(IntRect(700, 1700, 1020, 1880), pipVideoBounds(IntRect(700, 1700, 1020, 1880)))
    }

    @Test
    fun resizingThePlayerUpdatesTheVideoBounds() {
        assertEquals(IntRect(40, 100, 680, 460), pipVideoBounds(IntRect(40, 100, 680, 460)))
        assertEquals(IntRect(40, 100, 360, 280), pipVideoBounds(IntRect(40, 100, 360, 280)))
    }

    @Test
    fun emptyOrInvertedBoundsHaveNoVideo() {
        assertNull(pipVideoBounds(IntRect(0, 0, 0, 100)))
        assertNull(pipVideoBounds(IntRect(0, 0, 100, 0)))
        assertNull(pipVideoBounds(IntRect(100, 100, 0, 0)))
    }

    @Test
    fun aSliverTooSmallForAVideoHasNoBounds() {
        assertNull(pipVideoBounds(IntRect(0, 0, 1, 100)))
        assertNull(pipVideoBounds(IntRect(0, 0, 100, 1)))
    }

    @Test
    fun boundsMeasuredBeforeBindingAreReportedAndClearedOnClose() {
        var updates = 0
        val sources = PipVideoSources { updates += 1 }
        val source = PipVideoSource().apply { bounds = IntRect(40, 100, 680, 460) }
        sources.bind(source)
        assertEquals(source.bounds, sources.bounds)
        assertEquals(1, updates)
        sources.clear(source)
        assertNull(sources.bounds)
        assertEquals(2, updates)
    }

    @Test
    fun unchangedBoundsDoNotKeepSendingPipTransactions() {
        var updates = 0
        val sources = PipVideoSources { updates += 1 }
        val source = PipVideoSource().apply { bounds = IntRect(40, 100, 680, 460) }
        sources.bind(source)
        sources.update(source)
        sources.update(source)
        assertEquals(1, updates)
        source.bounds = IntRect(700, 1700, 1020, 1880)
        sources.update(source)
        assertEquals(source.bounds, sources.bounds)
        assertEquals(2, updates)
    }

    @Test
    fun oldChannelCannotClearOrOverwriteTheNewPlayersBounds() {
        var updates = 0
        val sources = PipVideoSources { updates += 1 }
        val previous = PipVideoSource().apply { bounds = IntRect(40, 100, 680, 460) }
        val next = PipVideoSource().apply { bounds = IntRect(700, 1700, 1020, 1880) }
        sources.bind(previous)
        sources.bind(next)
        previous.bounds = IntRect(0, 0, 320, 180)
        sources.update(previous)
        sources.clear(previous)
        assertEquals(next.bounds, sources.bounds)
        assertEquals(2, updates)
    }

    @Test
    fun newPlayerWithoutBoundsClearsThePreviousHintUntilItIsMeasured() {
        var updates = 0
        val sources = PipVideoSources { updates += 1 }
        sources.bind(PipVideoSource().apply { bounds = IntRect(40, 100, 680, 460) })
        val next = PipVideoSource()
        sources.bind(next)
        assertNull(sources.bounds)
        next.bounds = IntRect(700, 1700, 1020, 1880)
        sources.update(next)
        assertEquals(next.bounds, sources.bounds)
        assertEquals(3, updates)
    }
}
