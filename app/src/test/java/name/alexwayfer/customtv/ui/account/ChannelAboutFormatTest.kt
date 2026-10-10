package name.alexwayfer.customtv.ui.account

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelAboutFormatTest {
    @Test
    fun followerCountSeparatesThousandsForTheCurrentLocale() {
        assertEquals("12,345", formatFollowerCount(12_345, Locale.US))
    }
}
