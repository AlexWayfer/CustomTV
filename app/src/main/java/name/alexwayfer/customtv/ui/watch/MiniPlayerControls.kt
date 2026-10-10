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
import androidx.compose.ui.graphics.graphicsLayer
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

private fun DrawScope.drawStrokedIcon(
    lines: List<Pair<Offset, Offset>>,
    strokeWidth: Float,
) {
    val halo = listOf(
        3.1f to 0.08f,
        2.5f to 0.12f,
        2.0f to 0.18f,
        1.6f to 0.28f,
        1.3f to 0.42f,
    )
    halo.forEach { (widthMul, alpha) ->
        val color = Color.Black.copy(alpha = alpha)
        lines.forEach { (start, end) ->
            drawLine(color, start, end, strokeWidth * widthMul, StrokeCap.Round)
        }
    }
    lines.forEach { (start, end) ->
        drawLine(Color.White, start, end, strokeWidth, StrokeCap.Round)
    }
}

@Composable
internal fun MinimizeChevronIcon(
    modifier: Modifier,
    contentDescription: String,
) {
    Canvas(
        modifier = modifier.semantics { this.contentDescription = contentDescription },
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
