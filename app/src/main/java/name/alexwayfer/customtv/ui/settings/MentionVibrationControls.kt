package name.alexwayfer.customtv.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import name.alexwayfer.customtv.R

@Composable
internal fun MentionVibrationControls(
    durationMs: Int,
    strengthPercent: Int,
    onDurationChange: (Int) -> Unit,
    onStrengthChange: (Int) -> Unit,
) {
    val context = LocalContext.current
    val resolvedMs = mentionVibrationMs(durationMs)
    val resolvedPercent = mentionVibrationPercent(strengthPercent)
    SettingsStepSlider(
        title = stringResource(R.string.mention_vibration_length),
        valueText = { stringResource(R.string.mention_vibration_length_value, it) },
        value = resolvedMs,
        valueRange = MentionVibrationMinMs.toFloat()..MentionVibrationMaxMs.toFloat(),
        steps = mentionVibrationSliderSteps(),
        resolve = ::mentionVibrationMs,
        onChange = onDurationChange,
        onPreview = { chosen -> playMentionVibration(context, chosen, resolvedPercent) },
    )
    if (remember(context) { vibrator(context)?.hasAmplitudeControl() == true }) {
        SettingsStepSlider(
            title = stringResource(R.string.mention_vibration_strength),
            valueText = { stringResource(R.string.mention_vibration_strength_value, it) },
            value = resolvedPercent,
            valueRange = MentionVibrationMinPercent.toFloat()..MentionVibrationMaxPercent.toFloat(),
            steps = mentionVibrationPercentSteps(),
            resolve = ::mentionVibrationPercent,
            onChange = onStrengthChange,
            onPreview = { chosen -> playMentionVibration(context, resolvedMs, chosen) },
        )
    }
}
