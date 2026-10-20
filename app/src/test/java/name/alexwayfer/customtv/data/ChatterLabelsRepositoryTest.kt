package name.alexwayfer.customtv.data

import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.UiText
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatterLabelsRepositoryTest {
    private val chatter = LabeledChatter(userId = "123", login = "chatter", displayName = "Chatter")

    @Test
    fun refreshesAtOnceWhenNeverChecked() {
        assertEquals(Duration.ZERO, chatterLabelsRefreshDelay(sinceLastCheck = null, interval = 1.minutes))
    }

    @Test
    fun waitsTheRestOfTheIntervalAfterARecentCheck() {
        assertEquals(40.seconds, chatterLabelsRefreshDelay(sinceLastCheck = 20.seconds, interval = 1.minutes))
    }

    @Test
    fun refreshesAtOnceAfterALongerPauseInTheBackground() {
        assertEquals(Duration.ZERO, chatterLabelsRefreshDelay(sinceLastCheck = 5.minutes, interval = 1.minutes))
    }

    @Test
    fun matchesAssignmentsByStableUserIdAndSkipsDisabledLabels() {
        val labels = JSONArray(
            """[
                {"id":"friend","name":"Friend","enabled":true,"icon":"data:image/png;base64,AQID"},
                {"id":"hidden","name":"Hidden","enabled":false,"icon":"data:image/png;base64,AQID"}
            ]""",
        )
        val assignments = JSONArray(
            """[
                {"userId":"123","login":"old_login","labelId":"friend"},
                {"userId":"123","login":"new_login","labelId":"hidden"},
                {"userId":"456","login":"old_login","labelId":"friend"}
            ]""",
        )

        val result = parseChatterLabels(labels, assignments).byUserId

        assertEquals(1, result["123"]?.size)
        assertEquals("Friend", result["123"]?.single()?.name)
        assertEquals(listOf<Byte>(1, 2, 3), result["123"]?.single()?.image?.toList())
        assertEquals(1, result["456"]?.size)
        assertFalse(result.containsKey("old_login"))
    }

    @Test
    fun acceptsSvgDataUrlFromExtension() {
        val labels = JSONArray(
            """[{"id":"star","name":"Star","icon":"data:image/svg+xml,%3Csvg%3E%3C%2Fsvg%3E"}]""",
        )
        val assignments = JSONArray("""[{"userId":"123","labelId":"star"}]""")

        val result = parseChatterLabels(labels, assignments).byUserId

        assertTrue(result.getValue("123").single().image.toString(Charsets.UTF_8).startsWith("<svg>"))
    }

    @Test
    fun listsEnabledLabelsInOrderWithoutAssignmentTime() {
        val labels = JSONArray(
            """[
                {"id":"b","name":"B","icon":"data:image/png;base64,AQID"},
                {"id":"off","name":"Off","enabled":false,"icon":"data:image/png;base64,AQID"},
                {"id":"a","name":"A","icon":"data:image/png;base64,AQID"}
            ]""",
        )

        val result = parseChatterLabels(labels, JSONArray()).labels

        assertEquals(listOf("b", "a"), result.map { it.id })
        assertTrue(result.all { it.assignedAtMillis == null })
    }

    @Test
    fun readsAssignmentTimeAndLeavesABrokenOneEmpty() {
        val labels = JSONArray("""[{"id":"friend","name":"Friend","icon":"data:image/png;base64,AQID"}]""")
        val assignments = JSONArray(
            """[
                {"userId":"123","labelId":"friend","assignedAt":"2026-10-05T11:19:00.000Z"},
                {"userId":"456","labelId":"friend","assignedAt":"yesterday"}
            ]""",
        )

        val result = parseChatterLabels(labels, assignments).byUserId

        assertEquals(
            Instant.parse("2026-10-05T11:19:00Z").toEpochMilli(),
            checkNotNull(result.getValue("123").single().assignedAtMillis),
        )
        assertNull(result.getValue("456").single().assignedAtMillis)
    }

    @Test
    fun savingRemovesUncheckedKeepsCheckedTimeAndAppendsNewOne() {
        val assignments = JSONArray(
            """[
                {"userId":"123","labelId":"gone","assignedAt":"2026-01-01T00:00:00.000Z"},
                {"userId":"456","labelId":"gone","assignedAt":"2026-01-02T00:00:00.000Z"},
                {"userId":"123","labelId":"kept","assignedAt":"2026-01-03T00:00:00.000Z"}
            ]""",
        )

        val result = updateChatterAssignments(
            assignments = assignments,
            chatter = chatter,
            shownLabelIds = setOf("gone", "kept", "new"),
            checkedLabelIds = setOf("kept", "new"),
            now = Instant.parse("2026-10-05T11:19:00.123456Z"),
        )

        assertEquals(3, result.length())
        assertEquals("456", result.getJSONObject(0).getString("userId"))
        assertEquals("2026-01-03T00:00:00.000Z", result.getJSONObject(1).getString("assignedAt"))
        val added = result.getJSONObject(2)
        assertEquals("123", added.getString("userId"))
        assertEquals("chatter", added.getString("login"))
        assertEquals("Chatter", added.getString("username"))
        assertEquals("new", added.getString("labelId"))
        assertEquals("2026-10-05T11:19:00.123Z", added.getString("assignedAt"))
    }

    @Test
    fun savingKeepsAssignmentsOfLabelsTheCardDoesNotShow() {
        val assignments = JSONArray(
            """[
                {"userId":"123","labelId":"disabled"},
                {"userId":"123","labelId":"shown"}
            ]""",
        )

        val result = updateChatterAssignments(
            assignments = assignments,
            chatter = chatter,
            shownLabelIds = setOf("shown"),
            checkedLabelIds = emptySet(),
            now = Instant.EPOCH,
        )

        assertEquals(1, result.length())
        assertEquals("disabled", result.getJSONObject(0).getString("labelId"))
    }

    @Test
    fun savingDropsADuplicateOfAKeptAssignment() {
        val assignments = JSONArray(
            """[
                {"userId":"123","labelId":"friend"},
                {"userId":"123","labelId":"friend"}
            ]""",
        )

        val result = updateChatterAssignments(
            assignments = assignments,
            chatter = chatter,
            shownLabelIds = setOf("friend"),
            checkedLabelIds = setOf("friend"),
            now = Instant.EPOCH,
        )

        assertEquals(1, result.length())
    }

    @Test
    fun reloadKeepsTheSameImageArrayForAnUnchangedIconAndTakesAChangedOne() {
        val labels = JSONArray(
            """[
                {"id":"same","name":"Same","icon":"data:image/png;base64,AQID"},
                {"id":"changed","name":"Changed","icon":"data:image/png;base64,AQID"}
            ]""",
        )
        val previous = parseChatterLabels(labels, JSONArray()).labels
        labels.getJSONObject(1).put("icon", "data:image/png;base64,BAUG")
        val reloaded = parseChatterLabels(labels, JSONArray("""[{"userId":"123","labelId":"same"}]"""))

        val result = reuseChatterLabelImages(reloaded, previous)

        assertSame(previous[0].image, result.labels[0].image)
        assertSame(previous[0].image, result.byUserId.getValue("123").single().image)
        assertEquals(listOf<Byte>(4, 5, 6), result.labels[1].image.toList())
    }

    @Test
    fun writesJsonIndentedWithTabsLikeTheExtension() {
        val json = formatChatterLabelsJson(
            JSONArray("""[{"userId":"123","labelId":"a"},{"userId":"456","labelId":"b"}]"""),
        )

        // Android and the test's org.json nest the brackets differently, so only the indent is checked.
        assertTrue(json.lines().none { it.startsWith(" ") })
        assertTrue("\t\"userId\": \"123\"" in json)
    }

    @Test
    fun aReadRefusalNamesWhatTheUserFixesAndAnyOtherCodeIsShown() {
        assertEquals(UiText.Resource(R.string.chatter_labels_error_token_invalid), chatterLabelsReadFailure(401).text)
        assertEquals(UiText.Resource(R.string.chatter_labels_error_read_denied), chatterLabelsReadFailure(403).text)
        assertEquals(UiText.Resource(R.string.chatter_labels_error_not_found), chatterLabelsReadFailure(404).text)
        assertEquals(
            UiText.Resource(R.string.chatter_labels_error_http, listOf(500)),
            chatterLabelsReadFailure(500).text,
        )
    }

    @Test
    fun aWriteRefusalOrAMissingGistIsADeniedWrite() {
        assertEquals(UiText.Resource(R.string.chatter_labels_error_token_invalid), chatterLabelsWriteFailure(401).text)
        assertEquals(UiText.Resource(R.string.chatter_labels_error_write_denied), chatterLabelsWriteFailure(403).text)
        assertEquals(UiText.Resource(R.string.chatter_labels_error_write_denied), chatterLabelsWriteFailure(404).text)
        assertEquals(
            UiText.Resource(R.string.chatter_labels_error_http, listOf(502)),
            chatterLabelsWriteFailure(502).text,
        )
    }
}
