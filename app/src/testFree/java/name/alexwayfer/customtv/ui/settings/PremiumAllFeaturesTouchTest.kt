package name.alexwayfer.customtv.ui.settings

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import name.alexwayfer.customtv.ui.ComposeHost
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The link under the short Premium list, and the sheet with every feature that it opens. */
@RunWith(RobolectricTestRunner::class)
// The plain application: the app's own starts its stores and workers, which the list does not need.
@Config(application = Application::class)
class PremiumAllFeaturesTouchTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Test
    fun theLinkCountsEveryFeatureAndOpensTheFullList() {
        var opened = 0
        compose.setContent { PremiumAllFeaturesLink(onClick = { opened++ }) }

        compose.onNodeWithText("See all 36 features").performClick()

        assertEquals(1, opened)
    }

    @Test
    fun theFullListHasItsTitleAndFiveSections() {
        compose.setContent { PremiumAllFeaturesBody(onUpgrade = {}) }

        compose.onAllNodes(isHeading()).assertCountEquals(6)
    }

    @Test
    fun upgradeUnderTheFullListOpensTheUpgrade() {
        var upgrades = 0
        compose.setContent { PremiumAllFeaturesBody(onUpgrade = { upgrades++ }) }

        compose.onNodeWithText("Upgrade to Premium").performClick()

        assertEquals(1, upgrades)
    }
}
