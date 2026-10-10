package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatThreadPanelTest {
    @Test
    fun aJustOpenedPanelTakesNoHeight() = assertEquals(0, threadPanelShownHeightPx(300, dragPx = 0f, reveal = 0f))

    @Test
    fun halfwayOpenThePanelTakesHalfItsHeight() = assertEquals(150, threadPanelShownHeightPx(300, dragPx = 0f, reveal = 0.5f))

    @Test
    fun anOpenPanelDraggedDownLosesTheDraggedHeight() = assertEquals(200, threadPanelShownHeightPx(300, dragPx = 100f, reveal = 1f))

    @Test
    fun aDragPastThePanelLeavesNothing() = assertEquals(0, threadPanelShownHeightPx(300, dragPx = 500f, reveal = 1f))

    @Test
    fun anUnmeasuredPanelOrAnOvershootNeverGoesBelowZeroOrPastItsHeight() {
        assertEquals(0, threadPanelShownHeightPx(0, dragPx = 40f, reveal = 1f))
        assertEquals(0, threadPanelShownHeightPx(-20, dragPx = 0f, reveal = 1f))
        assertEquals(300, threadPanelShownHeightPx(300, dragPx = -50f, reveal = 1.3f))
    }
}
