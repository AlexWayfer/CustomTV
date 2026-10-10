package name.alexwayfer.customtv.ui.account

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileTabRowBleedTest {
    @Test
    fun aRowWiderByBothBleedsReportsTheParentsWidth() {
        assertEquals(300, bledElementReportedWidth(elementWidth = 348, bleedPx = 24))
    }

    @Test
    fun aRowNarrowerThanBothBleedsReportsZero() {
        assertEquals(0, bledElementReportedWidth(elementWidth = 30, bleedPx = 24))
    }

    @Test
    fun aZeroWidthRowReportsZero() {
        assertEquals(0, bledElementReportedWidth(elementWidth = 0, bleedPx = 24))
    }
}
