package name.alexwayfer.customtv.ui.settings

import android.app.ActivityManager.RunningAppProcessInfo

internal const val MENTION_VIBRATION_MIN_MS = 100
internal const val MENTION_VIBRATION_MAX_MS = 800
internal const val MENTION_VIBRATION_STEP_MS = 100

internal const val MENTION_VIBRATION_MIN_PERCENT = 10
internal const val MENTION_VIBRATION_MAX_PERCENT = 100
internal const val MENTION_VIBRATION_PERCENT_STEP = 10

internal fun mentionVibrationMs(stored: Int): Int =
    discreteStep(stored, MENTION_VIBRATION_MIN_MS, MENTION_VIBRATION_MAX_MS, MENTION_VIBRATION_STEP_MS)

internal fun mentionVibrationSliderSteps(): Int =
    discreteSliderSteps(MENTION_VIBRATION_MIN_MS, MENTION_VIBRATION_MAX_MS, MENTION_VIBRATION_STEP_MS)

internal fun mentionVibrationPercent(stored: Int): Int =
    discreteStep(
        stored,
        MENTION_VIBRATION_MIN_PERCENT,
        MENTION_VIBRATION_MAX_PERCENT,
        MENTION_VIBRATION_PERCENT_STEP,
    )

internal fun mentionVibrationPercentSteps(): Int =
    discreteSliderSteps(
        MENTION_VIBRATION_MIN_PERCENT,
        MENTION_VIBRATION_MAX_PERCENT,
        MENTION_VIBRATION_PERCENT_STEP,
    )

internal fun mentionVibrationAmplitude(percent: Int): Int =
    mentionVibrationPercent(percent) * MAX_VIBRATION_AMPLITUDE / 100

/** The strongest amplitude `VibrationEffect.createOneShot` takes. */
private const val MAX_VIBRATION_AMPLITUDE = 255

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
