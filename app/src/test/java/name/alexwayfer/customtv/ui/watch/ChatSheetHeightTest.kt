package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatSheetHeightTest {
    @Test
    fun sheetReachesUpToThePlayersBottomEdge() = assertEquals(1_400, chatSheetMaxHeightPx(containerHeightPx = 2_200, playerBottomPx = 800))

    @Test
    fun contentLeavesRoomForTheDragHandleAndTheSystemBar() =
        assertEquals(1_200, chatSheetContentMaxHeightPx(sheetMaxPx = 1_400, dragHandlePx = 132, bottomInsetPx = 68))

    @Test
    fun contentLimitNeverGoesBelowZero() =
        assertEquals(0, chatSheetContentMaxHeightPx(sheetMaxPx = 100, dragHandlePx = 132, bottomInsetPx = 68))

    @Test
    fun playerReachingTheBottomLeavesTheSheetUnlimited() {
        assertNull(chatSheetMaxHeightPx(containerHeightPx = 1_080, playerBottomPx = 1_080))
        assertNull(chatSheetMaxHeightPx(containerHeightPx = 1_080, playerBottomPx = 1_200))
    }
}
