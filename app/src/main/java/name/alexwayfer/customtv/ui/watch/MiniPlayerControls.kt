package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R

@Composable
internal fun BoxScope.MiniPlayerControls(
    expandAlpha: Float,
    onExpand: () -> Unit,
    onClose: () -> Unit,
) {
    IconButton(
        onClick = onExpand,
        modifier = Modifier
            .align(Alignment.TopStart)
            .size(44.dp)
            .graphicsLayer { alpha = expandAlpha },
    ) {
        ExpandArrowsIcon(
            modifier = Modifier.size(26.dp),
            contentDescription = stringResource(R.string.expand),
        )
    }
    IconButton(
        onClick = onClose,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .size(44.dp)
            .graphicsLayer { alpha = expandAlpha },
    ) {
        OutlinedCloseIcon(
            modifier = Modifier.size(24.dp),
            contentDescription = stringResource(R.string.close),
        )
    }
}

private val PlayerIconHalo = listOf(
    3.1f to 0.08f,
    2.5f to 0.12f,
    2.0f to 0.18f,
    1.6f to 0.28f,
    1.3f to 0.42f,
)

private fun DrawScope.drawStrokedIcon(
    lines: List<Pair<Offset, Offset>>,
    strokeWidth: Float,
) {
    PlayerIconHalo.forEach { (widthMul, alpha) ->
        val color = Color.Black.copy(alpha = alpha)
        lines.forEach { (start, end) ->
            drawLine(color, start, end, strokeWidth * widthMul, StrokeCap.Round)
        }
    }
    lines.forEach { (start, end) ->
        drawLine(Color.White, start, end, strokeWidth, StrokeCap.Round)
    }
}

private const val SLEEP_TIMER_VIEWPORT = 24f
private const val SLEEP_TIMER_RING_WIDTH = 2f

// Material Icons Outlined "schedule".
private val SleepTimerPath = PathParser().parsePathString(
    "M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2z " +
        "M12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z",
).toPath()

@Composable
internal fun SleepTimerIcon(tint: Color, contentDescription: String) {
    Canvas(Modifier.size(24.dp).semantics { this.contentDescription = contentDescription }) {
        scale(size.minDimension / SLEEP_TIMER_VIEWPORT, pivot = Offset.Zero) {
            PlayerIconHalo.forEach { (widthMul, alpha) ->
                drawPath(
                    path = SleepTimerPath,
                    color = Color.Black.copy(alpha = alpha),
                    // The path already includes the ring width, so only add the part outside its edges.
                    style = Stroke(width = SLEEP_TIMER_RING_WIDTH * (widthMul - 1f)),
                )
            }
            drawPath(SleepTimerPath, tint)
        }
    }
}

@Composable
internal fun MinimizeChevronIcon() {
    val description = stringResource(R.string.minimize)
    Canvas(
        modifier = Modifier.size(22.dp).semantics { contentDescription = description },
    ) {
        val strokeWidth = size.minDimension * 0.14f
        val padX = size.minDimension * 0.14f
        val top = size.height * 0.32f
        val bottom = size.height * 0.68f
        val mid = Offset(size.width / 2f, bottom)
        drawStrokedIcon(
            lines = listOf(
                Offset(padX, top) to mid,
                Offset(size.width - padX, top) to mid,
            ),
            strokeWidth = strokeWidth,
        )
    }
}

@Composable
private fun ExpandArrowsIcon(
    modifier: Modifier,
    contentDescription: String,
) {
    Canvas(
        modifier = modifier.semantics { this.contentDescription = contentDescription },
    ) {
        val strokeWidth = size.minDimension * 0.14f
        val pad = size.minDimension * 0.08f
        val len = size.minDimension * 0.26f
        val head = size.minDimension * 0.2f
        val tr = Offset(size.width - pad, pad)
        val bl = Offset(pad, size.height - pad)
        drawStrokedIcon(
            lines = listOf(
                Offset(tr.x - len, tr.y + len) to tr,
                Offset(tr.x - head, tr.y) to tr,
                Offset(tr.x, tr.y + head) to tr,
                Offset(bl.x + len, bl.y - len) to bl,
                Offset(bl.x + head, bl.y) to bl,
                Offset(bl.x, bl.y - head) to bl,
            ),
            strokeWidth = strokeWidth,
        )
    }
}

@Composable
private fun OutlinedCloseIcon(
    modifier: Modifier,
    contentDescription: String,
) {
    Canvas(
        modifier = modifier.semantics { this.contentDescription = contentDescription },
    ) {
        val strokeWidth = size.minDimension * 0.15f
        val pad = size.minDimension * 0.18f
        drawStrokedIcon(
            lines = listOf(
                Offset(pad, pad) to Offset(size.width - pad, size.height - pad),
                Offset(size.width - pad, pad) to Offset(pad, size.height - pad),
            ),
            strokeWidth = strokeWidth,
        )
    }
}
