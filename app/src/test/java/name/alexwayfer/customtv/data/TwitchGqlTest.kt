package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TwitchGqlTest {
    @Test
    fun aliasBatchesHoldAtMostFifteenInOrder() {
        val batches = gqlAliasBatches((1..18).toList())

        assertEquals(listOf((1..15).toList(), listOf(16, 17, 18)), batches)
    }

    @Test
    fun fifteenItemsFitOneBatch() {
        assertEquals(1, gqlAliasBatches((1..15).toList()).size)
    }

    @Test
    fun errorsToReportLeaveOutServerFailuresAndRepeats() {
        val body = """
            {"errors":[
              {"message":"Cannot query field \"foo\" on type \"User\"."},
              {"message":"service timeout"},
              {"message":"Service Unavailable"},
              {"message":"failed integrity check"},
              {"message":"failed integrity check"}
            ],"data":null}
        """.trimIndent()

        assertEquals(
            listOf("Cannot query field \"foo\" on type \"User\".", "failed integrity check"),
            gqlErrorsToReport(body),
        )
    }

    @Test
    fun errorsToReportLeaveOutWithheldChannelLinksButKeepOtherIntegrityFailures() {
        val body = """
            {"errors":[
              {"message":"failed integrity check","path":["user","channel","socialMedias"],
               "extensions":{"code":"IntegrityCheckFailed"}},
              {"message":"failed integrity check","path":["user","followers"],
               "extensions":{"code":"IntegrityCheckFailed"}}
            ],"data":{"user":{"channel":{"socialMedias":null},"followers":null}}}
        """.trimIndent()

        assertEquals(listOf("failed integrity check"), gqlErrorsToReport(body))
    }

    @Test
    fun errorsToReportAreEmptyForDataOrAnUnreadableBody() {
        assertEquals(emptyList<String>(), gqlErrorsToReport("""{"data":{"user":null}}"""))
        assertEquals(emptyList<String>(), gqlErrorsToReport("<html>"))
    }

    @Test
    fun failureCauseNamesTwitchErrorsOrWhatIsMissing() {
        assertEquals(
            "service timeout; failed",
            gqlFailureCause("""{"errors":[{"message":"service timeout"},{"message":"failed"}],"data":{"user":null}}"""),
        )
        assertEquals("not JSON", gqlFailureCause("<html>"))
        assertEquals("no data", gqlFailureCause("""{}"""))
        assertEquals("missing fields", gqlFailureCause("""{"data":{"user":null}}"""))
    }

    @Test
    fun failureCauseNamesTheIntegrityCheckThatLeftOutAField() {
        assertEquals(
            "failed integrity check",
            gqlFailureCause(
                """
                {"errors":[{"message":"failed integrity check","path":["user","channel","socialMedias"],
                  "extensions":{"code":"IntegrityCheckFailed"}}],
                 "data":{"user":{"followers":{"totalCount":255},"channel":{"socialMedias":null}}}}
                """.trimIndent(),
            ),
        )
    }
}
