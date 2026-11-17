package name.alexwayfer.customtv.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class ActivityHttpTransportTest {
    @Test
    fun premiumSecretAuthenticatesOnlyActivityRequests() {
        val access = ActivityAuthorization("premium", "secret")
        assertEquals("Bearer secret", activityHttpRequest("activity", JSONObject(), access).header("Authorization"))
        assertNull(activityHttpRequest("activity/challenge", JSONObject(), access).header("Authorization"))
        assertNull(activityHttpRequest("activity/attest", JSONObject(), access).header("Authorization"))
        assertFalse(access.toString().contains("secret"))
    }

    @Test
    fun freeReportDoesNotSendAnyAccountCredentials() {
        assertNull(activityHttpRequest("activity", JSONObject(), ActivityAuthorization("free", "secret"))
            .header("Authorization"))
    }

    @Test(expected = IOException::class)
    fun premiumWithoutLoginCannotSendAnUnauthenticatedReport() {
        activityHttpRequest("activity", JSONObject(), ActivityAuthorization("premium"))
    }
}
