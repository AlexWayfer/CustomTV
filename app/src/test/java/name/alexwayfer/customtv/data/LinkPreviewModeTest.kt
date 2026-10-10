package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LinkPreviewModeTest {
    @Test
    fun aStoredModeNameRestoresThatMode() {
        assertEquals(LinkPreviewMode.Full, storedLinkPreviewMode("Full"))
        assertEquals(LinkPreviewMode.Compact, storedLinkPreviewMode("Compact"))
        assertEquals(LinkPreviewMode.None, storedLinkPreviewMode("None"))
        assertEquals(LinkPreviewMode.None, storedLinkPreviewMode(null))
        assertEquals(LinkPreviewMode.None, storedLinkPreviewMode("full"))
    }
}
