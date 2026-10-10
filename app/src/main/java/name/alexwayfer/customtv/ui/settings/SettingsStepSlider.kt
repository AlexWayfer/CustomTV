package name.alexwayfer.customtv.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import name.alexwayfer.customtv.ui.theme.TwitchDivider
import name.alexwayfer.customtv.ui.theme.TwitchPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsStepSlider(
    title: String,
    valueText: @Composable (Int) -> String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    resolve: (Int) -> Int,
    onChange: (Int) -> Unit,
    onPreview: (Int) -> Unit,
) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    var pending by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(value) { pending = null }
    val shown = stepSliderShownValue(dragging, pending, value, resolve)
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(
            valueText(shown),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    Slider(
        value = shown.toFloat(),
        onValueChange = { dragging = it },
        onValueChangeFinished = {
            val chosen = dragging?.roundToInt()?.let(resolve) ?: return@Slider
            dragging = null
            if (chosen != value) {
                pending = chosen
                onChange(chosen)
            }
            onPreview(chosen)
        },
        valueRange = valueRange,
        steps = steps,
        interactionSource = interactionSource,
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = interactionSource,
                thumbSize = DpSize(6.dp, 28.dp),
            )
        },
        track = { sliderState ->
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(16.dp),
            ) {
                val y = center.y
                val thumbX = size.width * sliderState.coercedValueAsFraction
                val gap = 8.dp.toPx()
                val stroke = 6.dp.toPx()
                fun drawSegment(from: Float, to: Float, active: Boolean) {
                    if (to - from < 1f) return
                    drawLine(
                        color = if (active) TwitchPurple else TwitchDivider,
                        start = Offset(from, y),
                        end = Offset(to, y),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
                drawSegment(0f, thumbX - gap, active = true)
                drawSegment(thumbX + gap, size.width, active = false)
                if (sliderState.steps > 0) {
                    val tickCount = sliderState.steps + 2
                    val tickRadius = stroke / 2f
                    for (index in 0 until tickCount) {
                        val x = size.width * index / (sliderState.steps + 1)
                        if (abs(x - thumbX) < gap) continue
                        drawCircle(
                            color = if (x < thumbX) TwitchDivider else TwitchPurple,
                            radius = tickRadius,
                            center = Offset(x, y),
                        )
                    }
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .semantics { contentDescription = title },
    )
}

/** The drag position while dragging, then the released value until the saved [value] catches up. */
internal fun stepSliderShownValue(dragging: Float?, pending: Int?, value: Int, resolve: (Int) -> Int): Int =
    dragging?.roundToInt()?.let(resolve) ?: pending ?: value
