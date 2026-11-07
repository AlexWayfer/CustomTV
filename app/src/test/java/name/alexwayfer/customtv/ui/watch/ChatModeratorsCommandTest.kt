package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatModeratorsCommandTest {
    @Test
    fun moderatorsCommandIgnoresCaseAndSurroundingWhitespace() {
        assertTrue(isModeratorsCommand("/mods"))
        assertTrue(isModeratorsCommand(" /MoDs \n"))
    }

    @Test
    fun aMentionOfTheCommandOrExtraArgumentsDoesNotOpenTheMenu() {
        assertFalse(isModeratorsCommand("hello /mods"))
        assertFalse(isModeratorsCommand("/mods alice"))
        assertFalse(isModeratorsCommand("/mod"))
        assertFalse(isModeratorsCommand(""))
    }
}
