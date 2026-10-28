package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.data.CustomTvBadgeEdition
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class CustomTvBadgeRowTapTest {
    @Test
    fun premiumBadgeRowHasTheEditionsTap() {
        assertSame(customTvPremiumBadgeTap, customTvBadgeRowTap(CustomTvBadgeEdition.Premium))
    }

    @Test
    fun freeBadgeRowIsNotAButton() {
        assertNull(customTvBadgeRowTap(CustomTvBadgeEdition.Free))
    }
}
