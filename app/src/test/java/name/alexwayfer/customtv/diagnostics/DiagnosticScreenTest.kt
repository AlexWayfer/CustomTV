package name.alexwayfer.customtv.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticScreenTest {
    @Test
    fun landscapeWindowNamesItsOrientationAndSize() {
        val screen = DiagnosticScreen(landscape = true, widthDp = 891, heightDp = 411, pictureInPicture = false)

        assertEquals("Screen: landscape 891x411 dp, PiP: no", diagnosticScreenLine(screen))
    }

    @Test
    fun pictureInPictureWindowIsMarked() {
        val screen = DiagnosticScreen(landscape = false, widthDp = 240, heightDp = 135, pictureInPicture = true)

        assertEquals("Screen: portrait 240x135 dp, PiP: yes", diagnosticScreenLine(screen))
    }

    @Test
    fun noActivityGivesADash() {
        assertEquals("Screen: -", diagnosticScreenLine(null))
    }
}
