package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.data.ChatterLabelIcon
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatterLabelCommentsTest {
    private val assigned = listOf(ChatterLabelIcon("a", "A", byteArrayOf(1), comment = "saved"))

    @Test
    fun `a comment-only edit changes no assignments`() {
        assertEquals(emptySet<String>(), changedChatterAssignmentIds(setOf("a"), setOf("a")))
    }

    @Test
    fun `only checked and unchecked assignments are replaced`() {
        assertEquals(setOf("a", "new"), changedChatterAssignmentIds(setOf("a", "kept"), setOf("new", "kept")))
    }

    @Test
    fun `only edited comments of checked labels are saved`() {
        assertEquals(
            mapOf("a" to "edited", "new" to "new comment"),
            changedChatterComments(
                assigned,
                setOf("a", "new"),
                mapOf("a" to "edited", "new" to "new comment", "unchecked" to "ignored"),
            ),
        )
    }

    @Test
    fun `unchanged comments and an empty new comment need no update`() {
        assertEquals(
            emptyMap<String, String>(),
            changedChatterComments(assigned, setOf("a", "new"), mapOf("a" to "saved", "new" to "")),
        )
    }

    @Test
    fun `clearing a saved comment is an edit and line breaks are normalized`() {
        assertEquals(mapOf("a" to ""), changedChatterComments(assigned, setOf("a"), mapOf("a" to "")))
        assertEquals(
            mapOf("a" to "one two"),
            changedChatterComments(assigned, setOf("a"), mapOf("a" to "one\r\ntwo")),
        )
    }
}
