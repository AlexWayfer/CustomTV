package name.alexwayfer.customtv.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CacheMemoryLineTest {
    @Test
    fun `empty cache reports zero entries and key characters`() {
        assertEquals("empty=0 emptyKeyChars=0", cacheMemoryLine("empty", emptySet()))
    }

    @Test
    fun `cache report counts key references without exposing them`() {
        val keys = listOf("private-url", "private-token", "private-url")
        val line = cacheMemoryLine("cache", keys)
        assertEquals("cache=3 cacheKeyChars=35", line)
        assertFalse(line.contains("private"))
    }

    @Test
    fun `key character metric counts UTF-16 units rather than bytes`() {
        assertEquals("unicode=2 unicodeKeyChars=3", cacheMemoryLine("unicode", listOf("ё", "😞")))
    }
}
