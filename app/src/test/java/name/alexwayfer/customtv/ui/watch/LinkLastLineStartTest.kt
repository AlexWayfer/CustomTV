package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Test

class LinkLastLineStartTest {
    @Test
    fun oneLineLinkStartsAtItsOwnStart() {
        assertEquals(42, linkLastLineStart(linkStart = 42, lastLineStart = 30))
    }

    @Test
    fun twoLineLinkStartsAtTheStartOfItsLastLine() {
        assertEquals(60, linkLastLineStart(linkStart = 42, lastLineStart = 60))
    }
}
