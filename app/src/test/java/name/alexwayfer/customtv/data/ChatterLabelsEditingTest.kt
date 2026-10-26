package name.alexwayfer.customtv.data

import java.time.Instant
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatterLabelsEditingTest {
    @Test
    fun readsEveryLabelWithAnIdHiddenOnesTooAndEnabledByDefault() {
        val labels = JSONArray(
            """[
                {"id":"a","name":"A","icon":"data:a"},
                {"id":"b","name":"B","icon":"data:b","enabled":false},
                {"name":"No id","icon":"data:c"},
                "not a label"
            ]""",
        )

        assertEquals(
            listOf(
                EditableChatterLabel("a", "A", "data:a", enabled = true),
                EditableChatterLabel("b", "B", "data:b", enabled = false),
            ),
            editableChatterLabels(labels),
        )
    }

    @Test
    fun listsEachChatterOncePerLabelAndSkipsEntriesWithoutIds() {
        val assignments = JSONArray(
            """[
                {"userId":"1","login":"one","username":"One","labelId":"a","assignedAt":"2026-10-01T10:00:00.000Z"},
                {"userId":"1","login":"one","username":"One","labelId":"a","assignedAt":"2026-10-05T10:00:00.000Z"},
                {"userId":"2","login":"two","username":"Two","labelId":"a"},
                {"userId":"1","login":"one","username":"One","labelId":"b"},
                {"userId":"","login":"nobody","labelId":"b"},
                {"userId":"3","login":"three"},
                "not an assignment"
            ]""",
        )

        val assignees = chatterLabelAssignees(assignments)

        assertEquals(setOf("a", "b"), assignees.keys)
        assertEquals(listOf("1", "2"), assignees.getValue("a").map { it.userId })
        assertEquals(listOf("1"), assignees.getValue("b").map { it.userId })
        // The first entry keeps its time.
        assertEquals(
            Instant.parse("2026-10-01T10:00:00Z").toEpochMilli(),
            assignees.getValue("a").first().assignedAtMillis,
        )
    }

    @Test
    fun sortsChattersByLoginIgnoringCaseAndFallsBackToTheName() {
        val assignments = JSONArray(
            """[
                {"userId":"1","login":"zed","username":"Zed","labelId":"a"},
                {"userId":"2","login":"","username":"Mia","labelId":"a"},
                {"userId":"3","login":"Bob","username":"Bob","labelId":"a"},
                {"userId":"4","login":"anna","username":"Anna","labelId":"a"}
            ]""",
        )

        assertEquals(
            listOf("4", "3", "2", "1"),
            chatterLabelAssignees(assignments).getValue("a").map { it.userId },
        )
    }

    @Test
    fun aChatterShowsTheLoginOnlyWhenItDiffersFromTheName() {
        assertEquals("Anna", ChatterLabelAssignee("1", login = "anna", displayName = "Anna", null).shownName)
        assertEquals("Анна (anna)", ChatterLabelAssignee("1", login = "anna", displayName = "Анна", null).shownName)
        assertEquals("Anna", ChatterLabelAssignee("1", login = "", displayName = "Anna", null).shownName)
        assertEquals("anna", ChatterLabelAssignee("1", login = "anna", displayName = "", null).shownName)
        assertEquals("1", ChatterLabelAssignee("1", login = "", displayName = "", null).shownName)
    }

    @Test
    fun aNullLoginIsNoLogin() {
        val assignments = JSONArray("""[{"userId":"1","username":"Anna","login":null,"labelId":"a"}]""")

        assertEquals("", chatterLabelAssignees(assignments).getValue("a").single().login)
    }

    @Test
    fun aMissingOrBrokenTimeLeavesTheDateOut() {
        val assignments = JSONArray(
            """[
                {"userId":"1","login":"a","labelId":"a"},
                {"userId":"2","login":"b","labelId":"a","assignedAt":"yesterday"}
            ]""",
        )

        assertEquals(listOf(null, null), chatterLabelAssignees(assignments).getValue("a").map { it.assignedAtMillis })
    }

    @Test
    fun aLabelWithoutIconOrNameCannotBeSaved() {
        val problems = chatterLabelProblems(
            listOf(
                EditableChatterLabel("a", "  ", "", enabled = true),
                EditableChatterLabel("b", "B", "data:b", enabled = true),
            ),
        )

        assertEquals(
            mapOf("a" to setOf(ChatterLabelProblem.IconMissing, ChatterLabelProblem.NameMissing)),
            problems,
        )
    }

    @Test
    fun aTrimmedNameAnEarlierLabelHasIsTakenOnlyOnTheLaterLabel() {
        val problems = chatterLabelProblems(
            listOf(
                EditableChatterLabel("a", "Friends", "data:a", enabled = true),
                EditableChatterLabel("b", " Friends ", "data:b", enabled = true),
                EditableChatterLabel("c", "friends", "data:c", enabled = true),
            ),
        )

        assertEquals(mapOf("b" to setOf(ChatterLabelProblem.NameTaken)), problems)
    }

    @Test
    fun savingWithoutDeletingWritesTheLabelsInTheNewOrderAndLeavesTheAssignments() {
        val gist = ChatterLabelsGist(
            labels = JSONArray(
                """[
                    {"id":"a","name":"A","icon":"data:a","enabled":true,"color":"red"},
                    {"id":"b","name":"B","icon":"data:b","enabled":true}
                ]""",
            ),
            assignments = JSONArray("""[{"userId":"1","labelId":"a"}]"""),
        )

        val update = chatterLabelsUpdate(
            gist,
            listOf(
                EditableChatterLabel("b", "B", "data:b", enabled = false),
                EditableChatterLabel("a", " Renamed ", "data:new", enabled = true),
            ),
        )

        assertNull(update.assignments)
        assertEquals(2, update.labels.length())
        val first = update.labels.getJSONObject(0)
        assertEquals("b", first.getString("id"))
        assertEquals(false, first.getBoolean("enabled"))
        val second = update.labels.getJSONObject(1)
        assertEquals("Renamed", second.getString("name"))
        assertEquals("data:new", second.getString("icon"))
        // A field the app does not know stays.
        assertEquals("red", second.getString("color"))
    }

    @Test
    fun aNewLabelGetsEveryFieldTheExtensionReads() {
        val gist = ChatterLabelsGist(labels = JSONArray(), assignments = JSONArray())

        val update = chatterLabelsUpdate(gist, listOf(EditableChatterLabel("n", "New", "data:n", enabled = true)))

        val label = update.labels.getJSONObject(0)
        assertEquals("n", label.getString("id"))
        assertEquals("New", label.getString("name"))
        assertEquals("data:n", label.getString("icon"))
        assertEquals(true, label.getBoolean("enabled"))
    }

    @Test
    fun deletingALabelDropsOnlyItsAssignmentsInTheSameSave() {
        val gist = ChatterLabelsGist(
            labels = JSONArray(
                """[
                    {"id":"a","name":"A","icon":"data:a"},
                    {"id":"b","name":"B","icon":"data:b"}
                ]""",
            ),
            assignments = JSONArray(
                """[
                    {"userId":"1","labelId":"a"},
                    {"userId":"1","labelId":"b"},
                    {"userId":"2","labelId":"gone"},
                    "kept as is"
                ]""",
            ),
        )

        val update = chatterLabelsUpdate(gist, listOf(EditableChatterLabel("b", "B", "data:b", enabled = true)))

        val assignments = requireNotNull(update.assignments)
        assertEquals(3, assignments.length())
        assertEquals("b", assignments.getJSONObject(0).getString("labelId"))
        // An entry of a label deleted earlier is not this save's to drop.
        assertEquals("gone", assignments.getJSONObject(1).getString("labelId"))
        assertEquals("kept as is", assignments.getString(2))
        assertEquals(1, update.labels.length())
    }

    @Test
    fun aTallImageShrinksToTheIconHeightKeepingItsShape() {
        assertEquals(ChatterLabelIconSize(width = 18, height = 36), chatterLabelIconSize(width = 100, height = 200))
    }

    @Test
    fun aSmallImageIsNotEnlarged() {
        assertEquals(ChatterLabelIconSize(width = 20, height = 30), chatterLabelIconSize(width = 20, height = 30))
    }

    @Test
    fun eachSideKeepsAtLeastOnePixel() {
        assertEquals(ChatterLabelIconSize(width = 1000, height = 1), chatterLabelIconSize(width = 1000, height = 0))
        assertEquals(ChatterLabelIconSize(width = 1, height = 36), chatterLabelIconSize(width = 1, height = 1_000))
    }

    @Test
    fun anIconIsStoredAsAPngDataUrl() {
        val url = chatterLabelIconDataUrl(byteArrayOf(1, 2, 3))

        assertEquals("data:image/png;base64,AQID", url)
        assertTrue(checkNotNull(decodeChatterLabelIcon(url)).contentEquals(byteArrayOf(1, 2, 3)))
    }
}
