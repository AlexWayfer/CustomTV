package name.alexwayfer.customtv.data

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
    fun countsEachChatterOncePerLabel() {
        val assignments = JSONArray(
            """[
                {"userId":"1","labelId":"a"},
                {"userId":"1","labelId":"a"},
                {"userId":"2","labelId":"a"},
                {"userId":"1","labelId":"b"},
                {"userId":"","labelId":"b"}
            ]""",
        )

        assertEquals(mapOf("a" to 2, "b" to 1), chatterLabelChatterCounts(assignments))
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
