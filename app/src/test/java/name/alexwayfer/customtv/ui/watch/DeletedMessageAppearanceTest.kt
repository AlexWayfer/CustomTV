package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Test

class DeletedMessageAppearanceTest {
    @Test
    fun deletedMessageDimsContentButNotItsLinkPreview() {
        val appearance = deletedMessageAppearance(deleted = true)

        assertEquals(0.42f, appearance.contentAlpha)
        assertEquals(1f, appearance.linkPreviewAlpha)
    }

    @Test
    fun activeMessageKeepsContentAndLinkPreviewOpaque() {
        val appearance = deletedMessageAppearance(deleted = false)

        assertEquals(1f, appearance.contentAlpha)
        assertEquals(1f, appearance.linkPreviewAlpha)
    }
}
