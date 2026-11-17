package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.UiText
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatterLabelCheckTest {
    @Test
    fun recognizesExtensionTwitchHelpersAndLegacyCookieAccess() {
        val sessionScripts = listOf(
            "return await twitch.gql(query, variables, { authenticated: true })",
            "return await twitch?.getUser(user.id, 'login')",
            "return await twitch['gql'](query)",
        )
        for (script in sessionScripts) {
            assertEquals(ChatterCheckTwitchRequirement.Session, chatterCheckTwitchRequirement(script))
        }
        assertEquals(ChatterCheckTwitchRequirement.Browser, chatterCheckTwitchRequirement("return document.cookie"))
        assertEquals(
            ChatterCheckTwitchRequirement.Browser,
            chatterCheckTwitchRequirement("return fetch('https://gql.twitch.tv/gql')"),
        )
        assertEquals(
            ChatterCheckTwitchRequirement.None,
            chatterCheckTwitchRequirement("return bestLogs.countMessages(['twitch'])"),
        )
    }

    @Test
    fun archiveRequestKeepsTheCurrentUserAndChannelAndEncodesTheKeyword() {
        val query = JSONObject("""{"query":"слово","limit":50,"offset":100,"host":"other.example"}""")
        val url = checkNotNull(chatterBestLogsUrl("789", "123", query))

        assertEquals("bestlogs.lurkology.com", url.host)
        assertEquals("/channelid/789/userid/123/search", url.encodedPath)
        assertEquals("слово", url.queryParameter("q"))
        assertEquals("100", url.queryParameter("offset"))
        assertNull(chatterBestLogsUrl("789/..", "123", query))
        assertNull(chatterBestLogsUrl("789", "123", query.put("limit", 501)))
    }

    @Test
    fun reportsArchiveValidationAndOversizeSeparatelyFromConnectionFailures() {
        fun archiveError(reason: String) = parseChatterCheckResult(
            JSONObject().put("kind", "check-error").put("reason", reason),
        )

        val reasons = listOf("archive-rejected", "archive-too-large", "archive-request", "archive-timeout")
        assertEquals(reasons.size, reasons.map(::archiveError).toSet().size)
    }

    @Test
    fun keepsScriptResultsAsTextAndPreservesMessageReferences() {
        val result = parseChatterCheckResult(JSONObject("""
            {"kind":"result","value":{"text":"1 match","messages":[
                {"text":"<message>","timestamp":"2026-11-12T00:01:00Z","id":"m1","channelId":"789"}
            ]}}
        """.trimIndent()))

        assertEquals(
            ChatterCheckResult.Done(
                UiText.Raw("1 match"), listOf(ChatterCheckMessage("<message>", "2026-11-12T00:01:00Z", "m1", "789")),
            ),
            result,
        )
        assertEquals(
            ChatterCheckResult.Done(UiText.Resource(R.string.chatter_check_no)),
            parseChatterCheckResult(JSONObject("""{"kind":"result","value":false}""")),
        )
    }
}
