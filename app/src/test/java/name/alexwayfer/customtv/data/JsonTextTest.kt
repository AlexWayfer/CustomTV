package name.alexwayfer.customtv.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JsonTextTest {
    @Test
    fun objectTextReadsAsAnObject() {
        assertEquals("b", """{"a":"b"}""".toJsonObjectOrNull()?.optString("a"))
    }

    @Test
    fun anErrorPageIsNoObject() {
        assertNull("<html>Bad Gateway</html>".toJsonObjectOrNull())
        assertNull("".toJsonObjectOrNull())
    }

    @Test
    fun anArrayIsNoObjectAndTheOtherWayRound() {
        assertNull("[1]".toJsonObjectOrNull())
        assertNull("{}".toJsonArrayOrNull())
    }

    @Test
    fun arrayTextReadsAsAnArray() {
        assertEquals(2, "[1,2]".toJsonArrayOrNull()?.length())
    }

    private val fields = JSONObject(
        """{"name":" Kappa ","blank":"  ","word":"null","padded":" null ","none":null,"count":7,"zero":0,"empty":""}""",
    )

    @Test
    fun textIsTheValueAsSentAndNothingForMissingNullBlankOrTheWordNull() {
        assertEquals(" Kappa ", fields.text("name"))
        assertNull(fields.text("missing"))
        assertNull(fields.text("none"))
        assertNull(fields.text("blank"))
        assertNull(fields.text("word"))
        assertEquals(" null ", fields.text("padded"))
    }

    @Test
    fun trimmedTextDropsTheSpacesAroundAndThenWhatIsLeftEmptyOrTheWordNull() {
        assertEquals("Kappa", fields.trimmedText("name"))
        assertNull(fields.trimmedText("blank"))
        assertNull(fields.trimmedText("padded"))
        assertNull(fields.trimmedText("none"))
    }

    @Test
    fun aStringTextKeepsAValueAndDropsBlankNullOrTheWordNull() {
        assertEquals("Kappa", "Kappa".textOrNull())
        assertNull(null.textOrNull())
        assertNull(" ".textOrNull())
        assertNull("null".textOrNull())
    }

    @Test
    fun numbersAndStringsOrNullTellAMissingOrNullFieldFromAZeroOrAnEmptyOne() {
        assertEquals(7, fields.intOrNull("count"))
        assertEquals(0, fields.intOrNull("zero"))
        assertNull(fields.intOrNull("none"))
        assertNull(fields.intOrNull("missing"))
        assertEquals(7L, fields.longOrNull("count"))
        assertNull(fields.longOrNull("none"))
        assertEquals("", fields.stringOrNull("empty"))
        assertNull(fields.stringOrNull("none"))
        assertNull(fields.stringOrNull("missing"))
    }
}
