package name.alexwayfer.customtv.ui.settings

import android.app.ActivityManager.RunningAppProcessInfo
import name.alexwayfer.customtv.data.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MentionVibrationTest {
    @Test
    fun theDefaultLengthIsFourHundredMilliseconds() {
        assertEquals(400, AppSettings().mentionVibrationMs)
    }

    @Test
    fun aLengthOutsideTheRangeIsBroughtInsideIt() {
        assertEquals(MentionVibrationMinMs, mentionVibrationMs(0))
        assertEquals(MentionVibrationMaxMs, mentionVibrationMs(5_000))
    }

    @Test
    fun aLengthBetweenStepsUsesTheLowerStep() {
        assertEquals(400, mentionVibrationMs(400))
        assertEquals(400, mentionVibrationMs(450))
    }

    @Test
    fun theSliderHasAStepBetweenEachHundredMilliseconds() {
        assertEquals(6, mentionVibrationSliderSteps())
    }

    @Test
    fun theDefaultStrengthIsEightyPercent() {
        assertEquals(80, AppSettings().mentionVibrationPercent)
        assertEquals(204, mentionVibrationAmplitude(80))
    }

    @Test
    fun strengthStepsMapOntoTheVibratorRange() {
        assertEquals(10, mentionVibrationPercent(0))
        assertEquals(25, mentionVibrationAmplitude(10))
        assertEquals(50, mentionVibrationPercent(55))
        assertEquals(127, mentionVibrationAmplitude(50))
        assertEquals(8, mentionVibrationPercentSteps())
    }

    @Test
    fun anOpenScreenVibratesAsBefore() {
        assertFalse(mentionVibrationAsNotification(RunningAppProcessInfo.IMPORTANCE_FOREGROUND))
    }

    @Test
    fun aPlayingStreamBehindTheLockScreenVibratesAsBefore() {
        assertFalse(mentionVibrationAsNotification(RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE))
    }

    @Test
    fun aBackgroundProcessVibratesAsANotification() {
        assertTrue(mentionVibrationAsNotification(RunningAppProcessInfo.IMPORTANCE_VISIBLE))
        assertTrue(mentionVibrationAsNotification(RunningAppProcessInfo.IMPORTANCE_SERVICE))
        assertTrue(mentionVibrationAsNotification(RunningAppProcessInfo.IMPORTANCE_CACHED))
    }
}
