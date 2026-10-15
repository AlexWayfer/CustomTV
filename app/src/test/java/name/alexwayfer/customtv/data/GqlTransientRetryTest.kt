package name.alexwayfer.customtv.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GqlTransientRetryTest {
    @Test
    fun serverFailuresAnywhereInTheAnswerAreTransient() {
        assertTrue(hasTransientGqlError("""{"errors":[{"message":"service error","path":["user","videos"]}]}"""))
        assertTrue(hasTransientGqlError("""{"errors":[{"message":"failed"},{"message":"Service Timeout"}]}"""))
    }

    @Test
    fun otherErrorsDataAndUnreadableBodiesAreNotTransient() {
        assertFalse(hasTransientGqlError("""{"errors":[{"message":"failed integrity check"}],"data":null}"""))
        assertFalse(hasTransientGqlError("""{"data":{"user":null}}"""))
        assertFalse(hasTransientGqlError("<html>"))
    }

    @Test
    fun theLoggedPlaceNamesTheFirstServerFailureAndItsField() {
        val body = """
            {"errors":[
              {"message":"failed integrity check","path":["user","channel","socialMedias"]},
              {"message":"service error","path":["user","chapters","edges",3,"node","moments"]}
            ]}
        """.trimIndent()

        assertEquals("service error at user.chapters.edges.3.node.moments", transientGqlErrorPlace(body))
        assertEquals("service timeout", transientGqlErrorPlace("""{"errors":[{"message":"service timeout"}]}"""))
    }

    @Test
    fun anErrorIsWithinAFieldWhenItsPathStartsThere() {
        val json = JSONObject(
            """{"errors":[{"message":"service error","path":["user","chapters","edges",1,"node","moments"]}]}""",
        )

        assertTrue(gqlErrorWithin(json, listOf("user", "chapters")))
        assertTrue(gqlErrorWithin(json, listOf("user", "chapters", "edges", 1)))
        assertFalse(gqlErrorWithin(json, listOf("user", "chapters", "edges", 0)))
        assertFalse(gqlErrorWithin(json, listOf("user", "videos")))
        assertFalse(gqlErrorWithin(json, listOf("user", "chapters", "edges", 1, "node", "moments", "edges")))
    }

    @Test
    fun anErrorWithoutAPathIsWithinNoField() {
        assertFalse(gqlErrorWithin(JSONObject("""{"errors":[{"message":"service error"}]}"""), listOf("v0")))
    }

    @Test
    fun onlyMutationsAreSentOnce() {
        assertTrue(isGqlMutation(JSONObject().put("query", $$"  mutation($input:X!){x(input:$input){id}}")))
        assertFalse(isGqlMutation(JSONObject().put("query", "query{user(id:1){login}}")))
        assertFalse(isGqlMutation(JSONObject().put("query", "{user(id:1){login}}")))
        assertFalse(isGqlMutation(JSONObject().put("operationName", "Pinned").put("extensions", JSONObject())))
    }
}
