package name.alexwayfer.customtv.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsStepSliderTest {
    private val resolve: (Int) -> Int = { it.coerceIn(10, 20) }

    @Test
    fun draggingShowsTheRoundedDragPosition() {
        assertEquals(15, stepSliderShownValue(dragging = 14.6f, pending = null, value = 13, resolve = resolve))
    }

    @Test
    fun releasedValueStaysUntilTheSavedValueCatchesUp() {
        assertEquals(15, stepSliderShownValue(dragging = null, pending = 15, value = 13, resolve = resolve))
    }

    @Test
    fun withoutDragOrPendingValueTheSavedValueIsShown() {
        assertEquals(13, stepSliderShownValue(dragging = null, pending = null, value = 13, resolve = resolve))
    }
}
