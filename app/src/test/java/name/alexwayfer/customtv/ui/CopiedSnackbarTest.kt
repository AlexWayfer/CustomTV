package name.alexwayfer.customtv.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CopiedSnackbarTest {
    @Test
    fun `android 12L and older copy silently, so the app tells about the copy`() {
        assertTrue(copyNeedsOwnNotice(sdkInt = 28))
        assertTrue(copyNeedsOwnNotice(sdkInt = 32))
    }

    @Test
    fun `android 13 and newer show their own note, so the app stays quiet`() {
        assertFalse(copyNeedsOwnNotice(sdkInt = 33))
        assertFalse(copyNeedsOwnNotice(sdkInt = 36))
    }
}
