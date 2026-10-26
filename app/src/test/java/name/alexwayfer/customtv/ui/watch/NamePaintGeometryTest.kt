package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import name.alexwayfer.customtv.chat.PaintStop
import org.junit.Assert.assertEquals
import org.junit.Test

class NamePaintGeometryTest {
    private val box = Rect(left = 10f, top = 0f, right = 110f, bottom = 20f)

    @Test
    fun zeroDegreesRunsFromTheBottomUp() {
        val (from, to) = linearGradientEnds(0f, box)

        assertOffset(Offset(60f, 20f), from)
        assertOffset(Offset(60f, 0f), to)
    }

    @Test
    fun ninetyDegreesRunsFromLeftToRight() {
        val (from, to) = linearGradientEnds(90f, box)

        assertOffset(Offset(10f, 10f), from)
        assertOffset(Offset(110f, 10f), to)
    }

    @Test
    fun diagonalLineIsLongEnoughToReachTheCorners() {
        val square = Rect(0f, 0f, 10f, 10f)
        val (from, to) = linearGradientEnds(45f, square)

        // CSS makes the line as long as the box's sides seen along it: 10·sin45° + 10·cos45°.
        assertOffset(Offset(0f, 10f), from)
        assertOffset(Offset(10f, 0f), to)
    }

    @Test
    fun repeatingStopsSpanFromTheFirstToTheLast() {
        val span = repeatingSpan(
            listOf(PaintStop(0.2f, Color.Red), PaintStop(0.4f, Color.Green), PaintStop(0.6f, Color.Blue)),
        )

        assertEquals(0.2f, span.from, DELTA)
        assertEquals(0.6f, span.to, DELTA)
        assertEquals(3, span.stops.size)
        listOf(0f, 0.5f, 1f).zip(span.stops).forEach { (expected, stop) -> assertEquals(expected, stop.at, DELTA) }
    }

    @Test
    fun repeatingStopsAtOnePlaceKeepTheWholeLine() {
        val stops = listOf(PaintStop(0.5f, Color.Red), PaintStop(0.5f, Color.Blue))
        val span = repeatingSpan(stops)

        assertEquals(GradientSpan(0f, 1f, stops), span)
    }

    @Test
    fun stopsGoForwardWithinTheLine() {
        assertEquals(
            listOf(0f, 0.5f, 0.5f, 1f),
            orderedStopPositions(listOf(-0.5f, 0.5f, 0.3f, 1.5f)),
        )
    }

    @Test
    fun radialGradientReachesTheFarthestCorner() {
        assertEquals(5f, farthestCornerRadius(Rect(0f, 0f, 6f, 8f)), DELTA)
    }

    private fun assertOffset(expected: Offset, actual: Offset) {
        assertEquals(expected.x, actual.x, DELTA)
        assertEquals(expected.y, actual.y, DELTA)
    }

    private companion object {
        const val DELTA = 0.001f
    }
}
