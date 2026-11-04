package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatterCardSheetHeightTest {
    @Test
    fun `labels may cover the player up to eighty five percent of the screen`() {
        assertEquals(1_870, chatterCardSheetMaxHeightPx(ChatterCardPage.Labels, 2_200, chatMaxHeightPx = 1_400))
    }

    @Test
    fun `the label editor keeps the same limit when the keyboard changes the chat room`() {
        val closed = chatterCardSheetMaxHeightPx(ChatterCardPage.Labels, 2_200, chatMaxHeightPx = 1_400)
        val open = chatterCardSheetMaxHeightPx(ChatterCardPage.Labels, 2_200, chatMaxHeightPx = 600)

        assertEquals(closed, open)
    }

    @Test
    fun `other card pages keep the limit under the player`() {
        for (page in ChatterCardPage.entries.filterNot { it == ChatterCardPage.Labels }) {
            assertEquals(1_400, chatterCardSheetMaxHeightPx(page, 2_200, chatMaxHeightPx = 1_400))
        }
    }

    @Test
    fun `a card outside the player keeps other pages unlimited`() {
        assertNull(chatterCardSheetMaxHeightPx(ChatterCardPage.Profile, 2_200, chatMaxHeightPx = null))
    }

    @Test
    fun `the label limit stays bounded on a small or empty screen`() {
        assertEquals(85, chatterCardSheetMaxHeightPx(ChatterCardPage.Labels, 100, chatMaxHeightPx = null))
        assertEquals(0, chatterCardSheetMaxHeightPx(ChatterCardPage.Labels, 0, chatMaxHeightPx = null))
        assertEquals(0, chatterCardSheetMaxHeightPx(ChatterCardPage.Labels, -100, chatMaxHeightPx = null))
    }

    @Test
    fun `keyboard room replaces the system bar room rather than adding it twice`() {
        assertEquals(1_100, chatterCardImeContentMaxHeightPx(1_700, imeBottomPx = 680, systemBottomPx = 80))
        assertEquals(1_700, chatterCardImeContentMaxHeightPx(1_700, imeBottomPx = 40, systemBottomPx = 80))
        assertEquals(1_700, chatterCardImeContentMaxHeightPx(1_700, imeBottomPx = 0, systemBottomPx = 80))
    }

    @Test
    fun `the card top stays in place throughout keyboard opening and closing`() {
        val screenHeight = 2_200
        val contentLimit = 1_700
        val handle = 120
        val systemBar = 80
        val closedTop = screenHeight - contentLimit - handle - systemBar
        for (ime in listOf(0, 40, 80, 200, 680, 420, 80, 0)) {
            val content = chatterCardImeContentMaxHeightPx(contentLimit, ime, systemBar)
            val top = screenHeight - content - handle - maxOf(systemBar, ime)

            assertEquals(closedTop, top)
        }
    }

    @Test
    fun `a keyboard taller than the content limit never makes the size negative`() {
        assertEquals(0, chatterCardImeContentMaxHeightPx(100, imeBottomPx = 680, systemBottomPx = 80))
        assertEquals(0, chatterCardImeContentMaxHeightPx(0, imeBottomPx = 680, systemBottomPx = 80))
    }
}
