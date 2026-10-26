package name.alexwayfer.customtv.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatterAssignmentUsersTest {
    @Test
    fun asksForEachChatterOnceAndSkipsEntriesWithoutAnId() {
        val assignments = JSONArray(
            """[
                {"userId":"1","labelId":"a"},
                {"userId":"2","labelId":"a"},
                {"userId":"1","labelId":"b"},
                {"userId":"","labelId":"b"},
                "not an assignment"
            ]""",
        )

        assertEquals(listOf("1", "2"), chatterAssignmentUserIds(assignments))
    }

    @Test
    fun bringsEveryEntryOfAChatterUpToDateAndKeepsTheRest() {
        val assignments = JSONArray(
            """[
                {"userId":"1","username":"Анна","labelId":"a","assignedAt":"2026-10-01T10:00:00.000Z"},
                {"userId":"1","username":"Анна","login":"old","labelId":"b"},
                {"userId":"2","username":"Bob","login":"bob","labelId":"a"},
                {"userId":"3","username":"Gone","login":"gone","labelId":"a"},
                "not an assignment"
            ]""",
        )

        val synced = syncChatterAssignmentUsers(
            assignments,
            mapOf(
                "1" to LabeledChatter("1", login = "anna", displayName = "Anna"),
                "2" to LabeledChatter("2", login = "bob", displayName = "Bob"),
            ),
        )

        assertEquals(
            JSONArray(
                """[
                    {"userId":"1","username":"Anna","labelId":"a","assignedAt":"2026-10-01T10:00:00.000Z","login":"anna"},
                    {"userId":"1","username":"Anna","login":"anna","labelId":"b"},
                    {"userId":"2","username":"Bob","login":"bob","labelId":"a"},
                    {"userId":"3","username":"Gone","login":"gone","labelId":"a"},
                    "not an assignment"
                ]""",
            ).toString(),
            synced?.toString(),
        )
    }

    @Test
    fun tellsActiveBannedAndUnknownChattersApart() {
        val data = JSONObject(
            """{
                "active0":{"login":"anna","displayName":"Anna"},
                "any0":{"login":"anna","displayName":"Anna","deletedAt":null},
                "active1":null,
                "any1":{"login":"banned","displayName":"Banned","deletedAt":null},
                "active2":null,
                "any2":{"login":"gone","displayName":"Gone","deletedAt":"2026-01-01T00:00:00Z"},
                "active3":null,
                "any3":null
            }""",
        )

        val names = parseChatterNames(listOf("1", "2", "3", "4"), data)

        assertEquals(
            mapOf(
                "1" to LabeledChatter("1", "anna", "Anna"),
                "2" to LabeledChatter("2", "banned", "Banned"),
            ),
            names.found,
        )
        assertEquals(setOf("2"), names.banned)
        assertEquals(setOf("3", "4"), names.notFound)
    }

    @Test
    fun entriesThatAreUpToDateNeedNoWrite() {
        val assignments = JSONArray("""[{"userId":"2","username":"Bob","login":"bob","labelId":"a"}]""")

        assertNull(
            syncChatterAssignmentUsers(assignments, mapOf("2" to LabeledChatter("2", "bob", "Bob"))),
        )
    }
}
