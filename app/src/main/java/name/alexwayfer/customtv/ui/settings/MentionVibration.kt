package name.alexwayfer.customtv.ui.settings

import android.app.ActivityManager.RunningAppProcessInfo

internal const val MentionVibrationMinMs = 100
internal const val MentionVibrationMaxMs = 800
internal const val MentionVibrationStepMs = 100

internal const val MentionVibrationMinPercent = 10
internal const val MentionVibrationMaxPercent = 100
internal const val MentionVibrationPercentStep = 10

internal fun mentionVibrationMs(stored: Int): Int =
    discreteStep(stored, MentionVibrationMinMs, MentionVibrationMaxMs, MentionVibrationStepMs)

internal fun mentionVibrationSliderSteps(): Int =
    discreteSliderSteps(MentionVibrationMinMs, MentionVibrationMaxMs, MentionVibrationStepMs)

internal fun mentionVibrationPercent(stored: Int): Int =
    discreteStep(
        stored,
        MentionVibrationMinPercent,
        MentionVibrationMaxPercent,
        MentionVibrationPercentStep,
    )

internal fun mentionVibrationPercentSteps(): Int =
    discreteSliderSteps(
        MentionVibrationMinPercent,
        MentionVibrationMaxPercent,
        MentionVibrationPercentStep,
    )

internal fun mentionVibrationAmplitude(percent: Int): Int =
    mentionVibrationPercent(percent) * 255 / 100

/**
 * Android 13+ drops a vibration without a usage from a process below foreground-service importance, the
 * same threshold the vibrator service applies. Such a process vibrates as a notification instead; a visible
 * screen or a playing stream keeps the plain vibration, which notification settings do not affect.
 */
internal fun mentionVibrationAsNotification(processImportance: Int): Boolean =
    processImportance > RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE

private fun discreteStep(stored: Int, min: Int, max: Int, step: Int): Int {
    val clamped = stored.coerceIn(min, max)
    return min + (clamped - min) / step * step
}

private fun discreteSliderSteps(min: Int, max: Int, step: Int): Int =
    (max - min) / step - 1
