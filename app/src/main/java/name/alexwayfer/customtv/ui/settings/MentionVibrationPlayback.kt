package name.alexwayfer.customtv.ui.settings

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

internal fun playMentionVibration(context: Context, storedMs: Int, storedPercent: Int) {
    val vibrator = vibrator(context) ?: return
    if (!vibrator.hasVibrator()) return
    val amplitude = if (vibrator.hasAmplitudeControl()) {
        mentionVibrationAmplitude(storedPercent)
    } else {
        VibrationEffect.DEFAULT_AMPLITUDE
    }
    val effect = VibrationEffect.createOneShot(mentionVibrationMs(storedMs).toLong(), amplitude)
    if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        mentionVibrationAsNotification(processImportance())
    ) {
        vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_NOTIFICATION))
    } else {
        vibrator.vibrate(effect)
    }
}

private fun processImportance(): Int {
    val info = ActivityManager.RunningAppProcessInfo()
    ActivityManager.getMyMemoryState(info)
    return info.importance
}

internal fun vibrator(context: Context): Vibrator? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        context.getSystemService(Vibrator::class.java)
    }
}
