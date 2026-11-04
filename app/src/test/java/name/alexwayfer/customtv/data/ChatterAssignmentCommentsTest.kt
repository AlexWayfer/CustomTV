package name.alexwayfer.customtv.data

import java.time.Instant
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ChatterAssignmentCommentsTest {
    private val labels = JSONArray(
        """[{"id":"a","name":"A","icon":"data:image/png;base64,AQID"}]""",
    )

    @Test
    fun `a changed comment changes the assignment shown on the profile`() {
        val before = JSONArray("""[{"userId":"1","labelId":"a","comment":"First"}]""")
        val after = JSONArray("""[{"userId":"1","labelId":"a","comment":"Second"}]""")

        assertNotEquals(parseChatterLabels(labels, before).byUserId, parseChatterLabels(labels, after).byUserId)
    }

    @Test
    fun `a changed comment changes the assignee shown in settings`() {
        val before = JSONArray("""[{"userId":"1","labelId":"a","comment":"First"}]""")
        val after = JSONArray("""[{"userId":"1","labelId":"a","comment":"Second"}]""")

        assertNotEquals(chatterLabelAssignees(before), chatterLabelAssignees(after))
    }

    @Test
    fun `a new assignment writes an empty comment for the extension`() {
        val result = updateChatterAssignments(
            assignments = JSONArray(),
            chatter = LabeledChatter("1", "anna", "Anna"),
            shownLabelIds = setOf("a"),
            checkedLabelIds = setOf("a"),
            now = Instant.EPOCH,
        )

        assertEquals("", result.getJSONObject(0).optString("comment", "missing"))
    }

    @Test
    fun `missing and null comments are empty and line breaks follow the extension`() {
        val assignments = JSONArray(
            """[
                {"userId":"1","labelId":"a"},
                {"userId":"2","labelId":"a","comment":null},
                {"userId":"3","labelId":"a","comment":" one\r\ntwo\nthree\rfour "}
            ]""",
        )

        val parsed = parseChatterLabels(labels, assignments).byUserId
        assertEquals(listOf("", "", " one two three four "), parsed.values.map { it.single().comment })
        assertEquals(
            listOf("", "", " one two three four "),
            chatterLabelAssignees(assignments).getValue("a").map { it.comment },
        )
    }

    @Test
    fun `editing changes only the targeted comment and never mutates the source`() {
        val assignments = JSONArray(
            """[
                {"userId":"1","labelId":"a","comment":"old","assignedAt":"kept","extra":true},
                {"userId":"1","labelId":"b","comment":"changed elsewhere"},
                {"userId":"2","labelId":"a","comment":"other chatter"},
                "unknown entry"
            ]""",
        )

        val result = updateChatterAssignmentComments(assignments, "1", mapOf("a" to "new\r\ncomment"))

        assertEquals("new comment", result.getJSONObject(0).getString("comment"))
        assertEquals("kept", result.getJSONObject(0).getString("assignedAt"))
        assertEquals(true, result.getJSONObject(0).getBoolean("extra"))
        assertEquals("changed elsewhere", result.getJSONObject(1).getString("comment"))
        assertEquals("other chatter", result.getJSONObject(2).getString("comment"))
        assertEquals("unknown entry", result.getString(3))
        assertEquals("old", assignments.getJSONObject(0).getString("comment"))
    }

    @Test
    fun `clearing a comment keeps the assignment and a deleted assignment stays deleted`() {
        val assignments = JSONArray("""[{"userId":"1","labelId":"a","comment":"old"}]""")

        val result = updateChatterAssignmentComments(assignments, "1", mapOf("a" to "", "deleted" to "new"))

        assertEquals(1, result.length())
        assertEquals("", result.getJSONObject(0).getString("comment"))
    }

    @Test
    fun `a comment-only card save keeps concurrent assignments and does not recreate a deleted one`() {
        val assignments = JSONArray("""[{"userId":"1","labelId":"concurrent","comment":"kept"}]""")
        val selected = updateChatterAssignments(
            assignments,
            LabeledChatter("1", "anna", "Anna"),
            shownLabelIds = emptySet(),
            checkedLabelIds = setOf("deleted"),
            now = Instant.EPOCH,
        )

        val result = updateChatterAssignmentComments(selected, "1", mapOf("deleted" to "new"))

        assertEquals(assignments.toString(), result.toString())
    }

    @Test
    fun `image reuse retains the refreshed comment`() {
        val previous = parseChatterLabels(labels, JSONArray()).labels
        val changed = parseChatterLabels(
            labels,
            JSONArray("""[{"userId":"1","labelId":"a","comment":"new"}]"""),
        )
        // Use a different array with the same bytes to exercise the replacement path.
        val refreshed = reuseChatterLabelImages(changed, previous)

        assertEquals("new", refreshed.byUserId.getValue("1").single().comment)
    }

    @Test
    fun `refreshing the chatter name retains the current comment`() {
        val assignments = JSONArray("""[{"userId":"1","labelId":"a","username":"Old","comment":"current"}]""")

        val result = checkNotNull(
            syncChatterAssignmentUsers(assignments, mapOf("1" to LabeledChatter("1", "anna", "Anna"))),
        )

        assertEquals("current", result.getJSONObject(0).getString("comment"))
        assertEquals("Anna", result.getJSONObject(0).getString("username"))
    }
}
