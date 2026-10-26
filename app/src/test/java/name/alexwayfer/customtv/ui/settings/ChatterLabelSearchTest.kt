package name.alexwayfer.customtv.ui.settings

import name.alexwayfer.customtv.data.ChatterLabelAssignee
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatterLabelSearchTest {
    private val anna = ChatterLabelAssignee("1", login = "anna_k", displayName = "Анна", assignedAtMillis = null)
    private val bob = ChatterLabelAssignee("2", login = "bobby", displayName = "Bobby", assignedAtMillis = null)
    private val both = listOf(anna, bob)

    @Test
    fun `nothing typed searches for nothing`() {
        assertNull(chatterSearchTerm(""))
        assertNull(chatterSearchTerm("  "))
        assertNull(chatterSearchTerm(" @ "))
    }

    @Test
    fun `the term drops the spaces around it and an at sign before a nick`() {
        assertEquals("ann", chatterSearchTerm(" @ann "))
    }

    @Test
    fun `without a term every chatter is shown`() {
        assertEquals(both, assigneesMatching(both, null))
    }

    @Test
    fun `a piece from the middle of a login finds the chatter, in any case`() {
        assertEquals(listOf(anna), assigneesMatching(both, "NA_"))
    }

    @Test
    fun `a piece of the display name finds the chatter too`() {
        assertEquals(listOf(anna), assigneesMatching(both, "нн"))
    }

    @Test
    fun `a term no chatter has finds none`() {
        assertEquals(emptyList<ChatterLabelAssignee>(), assigneesMatching(both, "zed"))
    }
}
