package name.alexwayfer.customtv.ui.account

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.ComposeHost
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ProfileHomeNoticeRenderTest {
    @get:Rule(order = 0)
    val host = ComposeHost()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    /** Where the text of [to] stands on each frame after the spinner gives way to it, and once all settles. */
    private fun topsAfterLoading(to: ProfileHomeNotice, text: Int): List<Float> {
        var notice by mutableStateOf(ProfileHomeNotice.Loading)
        var shown = ""
        compose.setContent {
            shown = stringResource(text)
            Column { ProfileHomeNoticeSlot(notice, onRetry = {}) }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        notice = to
        val tops = mutableListOf<Float>()
        fun sample() {
            compose.onAllNodesWithText(shown).fetchSemanticsNodes().firstOrNull()?.let { tops += it.positionInRoot.y }
        }
        repeat(FRAMES) {
            compose.mainClock.advanceTimeByFrame()
            sample()
        }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        sample()
        return tops
    }

    @Test
    fun theEmptyNoteStaysInPlaceAsTheSpinnerFades() {
        val tops = topsAfterLoading(ProfileHomeNotice.Empty, R.string.profile_home_empty_title)

        assertEquals(listOf(tops.last()), tops.distinct())
    }

    @Test
    fun theFailureStaysInPlaceAsTheSpinnerFades() {
        val tops = topsAfterLoading(ProfileHomeNotice.Failed, R.string.profile_videos_failed)

        assertEquals(listOf(tops.last()), tops.distinct())
    }

    private companion object {
        /** Past the fade of the spinner, at 60 frames a second. */
        const val FRAMES = 60
    }
}
