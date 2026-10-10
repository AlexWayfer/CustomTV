package name.alexwayfer.customtv.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationBarVisibilityTest {
    @Test
    fun theBarStaysWhileAClosingKeyboardStillReportsAnInset() {
        assertTrue(
            showAppNavigationBar(
                inPictureInPicture = false,
                imeBottomPx = 0,
                imeTargetBottomPx = 0,
                keepVisibleWhileImeCloses = false,
                keyboardInHomeSearch = false,
            ),
        )
        assertFalse(
            showAppNavigationBar(
                inPictureInPicture = false,
                imeBottomPx = 400,
                imeTargetBottomPx = 400,
                keepVisibleWhileImeCloses = false,
                keyboardInHomeSearch = false,
            ),
        )
        assertTrue(
            showAppNavigationBar(
                inPictureInPicture = false,
                imeBottomPx = 400,
                imeTargetBottomPx = 0,
                keepVisibleWhileImeCloses = false,
                keyboardInHomeSearch = false,
            ),
        )
        assertTrue(
            showAppNavigationBar(
                inPictureInPicture = false,
                imeBottomPx = 400,
                imeTargetBottomPx = 400,
                keepVisibleWhileImeCloses = true,
                keyboardInHomeSearch = false,
            ),
        )
        assertFalse(
            showAppNavigationBar(
                inPictureInPicture = true,
                imeBottomPx = 0,
                imeTargetBottomPx = 0,
                keepVisibleWhileImeCloses = true,
                keyboardInHomeSearch = false,
            ),
        )
    }

    @Test
    fun theBarStaysUnderTheKeyboardOfTheOpenHomeSearch() {
        assertTrue(
            showAppNavigationBar(
                inPictureInPicture = false,
                imeBottomPx = 400,
                imeTargetBottomPx = 400,
                keepVisibleWhileImeCloses = false,
                keyboardInHomeSearch = true,
            ),
        )
    }

    @Test
    fun theHomeListDoesNotKeepTheClosingKeyboardInset() {
        assertFalse(homeUsesImePadding(keepNavigationBarWhileImeCloses = true, keyboardInHomeSearch = false))
        assertTrue(homeUsesImePadding(keepNavigationBarWhileImeCloses = false, keyboardInHomeSearch = false))
    }

    @Test
    fun theHomeListDoesNotPadForTheKeyboardOfTheOpenHomeSearch() {
        assertFalse(homeUsesImePadding(keepNavigationBarWhileImeCloses = false, keyboardInHomeSearch = true))
    }

    @Test
    fun aLargePhoneHeldUprightKeepsTheBottomBar() {
        assertFalse(appNavigationUsesRail(windowWidthDp = 448f))
    }

    @Test
    fun aPhoneOnItsSideUsesTheRail() {
        assertTrue(appNavigationUsesRail(windowWidthDp = 915f))
    }

    @Test
    fun theRailStartsAtMaterialsMediumWidth() {
        assertFalse(appNavigationUsesRail(windowWidthDp = 599.9f))
        assertTrue(appNavigationUsesRail(windowWidthDp = 600f))
    }
}
