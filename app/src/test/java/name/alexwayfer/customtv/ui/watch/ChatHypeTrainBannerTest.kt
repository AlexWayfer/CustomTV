package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatHypeTrainBannerTest {
    @Test
    fun theCountdownShowsMinutesAndSecondsRoundedUp() {
        assertEquals("2:58", countdownLabel(178_000L))
        assertEquals("0:01", countdownLabel(200L))
        assertEquals("0:00", countdownLabel(-5_000L))
        assertEquals("10:00", countdownLabel(600_000L))
    }

    @Test
    fun aLevelUpNamesTheCompletedLevelAndTheFirstLevelOrAStepBackDoesNot() {
        assertEquals(1, completedHypeTrainLevel(previous = 1, current = 2))
        assertEquals(3, completedHypeTrainLevel(previous = 1, current = 4))
        assertNull(completedHypeTrainLevel(previous = 0, current = 1))
        assertNull(completedHypeTrainLevel(previous = 2, current = 2))
    }

    @Test
    fun theTrainsColorComesFromTwitchsHexAndAMissingOneLeavesTheDefault() {
        assertEquals(Color(0xFF00CCFF), hypeTrainColor("00CCFF")!!)
        assertEquals(Color(0xFFEB0400), hypeTrainColor("#EB0400")!!)
        assertNull(hypeTrainColor(null))
        assertNull(hypeTrainColor("zz"))
    }
}
