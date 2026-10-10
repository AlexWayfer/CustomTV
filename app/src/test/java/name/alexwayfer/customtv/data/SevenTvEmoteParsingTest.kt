package name.alexwayfer.customtv.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SevenTvEmoteParsingTest {
    @Test
    fun `a set item keeps the keywords and the original name of a renamed emote`() {
        val item = JSONObject(
            """
                {"id":"e1","name":"a","flags":0,"data":{"id":"e1","name":"Aware","tags":["aware","pepe",""],
                "host":{"url":"//cdn.7tv.app/emote/e1","files":[]}}}
            """.trimIndent(),
        )

        val (name, emote) = SevenTvRepository.parseSetItem(item)!!

        assertEquals("a", name)
        assertEquals(listOf("aware", "pepe"), emote.tags)
        assertEquals("Aware", emote.originalName)
    }

    @Test
    fun `an emote under its own name has no original name, and one without tags has none`() {
        val item = JSONObject(
            """
                {"id":"e2","name":"KEKW","data":{"id":"e2","name":"KEKW","host":{"url":"//cdn.7tv.app/emote/e2"}}}
            """.trimIndent(),
        )

        val emote = SevenTvRepository.parseSetItem(item)!!.second

        assertNull(emote.originalName)
        assertEquals(emptyList<String>(), emote.tags)
    }
}
