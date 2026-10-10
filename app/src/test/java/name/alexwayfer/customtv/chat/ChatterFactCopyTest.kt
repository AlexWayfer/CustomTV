package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatterFactCopyTest {
    @Test
    fun copiesTheTitleThenTheDateAndAge() {
        assertEquals(
            "Following since Jan 10, 2021 (5 years 8 months)",
            chatterFactCopyText("Following since", "Jan 10, 2021 (5 years 8 months)"),
        )
    }

    @Test
    fun factWithoutALineUnderItCopiesTheTitle() {
        assertEquals("Not following", chatterFactCopyText("Not following", null))
    }
}
