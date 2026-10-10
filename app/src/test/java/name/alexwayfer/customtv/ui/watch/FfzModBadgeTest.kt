package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FfzModBadgeTest {
    @Test
    fun ffzModeratorBadgeGetsTheGreenBackground() {
        assertTrue(isFfzModBadge("https://cdn.frankerfacez.com/room-badge/mod/id/1/v/a/2"))
    }

    @Test
    fun ffzVipBadgeAndTwitchBadgesKeepTheirOwnBackground() {
        assertFalse(isFfzModBadge("https://cdn.frankerfacez.com/room-badge/vip/id/1/v/b/2"))
        assertFalse(isFfzModBadge("https://static-cdn.jtvnw.net/badges/v1/3267646d/2"))
        assertFalse(isFfzModBadge(null))
    }
}
