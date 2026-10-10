package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

internal enum class PredictionOutcomeColor {
    Blue,
    Pink,
}

/** As on Twitch: of two outcomes the first is blue and the second pink; more outcomes are all blue. */
internal fun predictionOutcomeColor(index: Int, count: Int): PredictionOutcomeColor =
    if (count == 2 && index == 1) PredictionOutcomeColor.Pink else PredictionOutcomeColor.Blue

internal fun PredictionOutcomeColor.tint(): Color = when (this) {
    PredictionOutcomeColor.Blue -> PredictionBlue
    PredictionOutcomeColor.Pink -> PredictionPink
}

/** The outcome's number in a circle of its color, as on Twitch. TalkBack reads the outcome's title instead. */
@Composable
internal fun PredictionOutcomeBadge(number: Int, color: PredictionOutcomeColor, size: Dp, textSize: TextUnit) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color.tint())
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        // The line keeps only the glyph's height, centered, so the digit sits in the middle of the circle.
        Text(
            text = number.toString(),
            color = Color.White,
            style = TextStyle(
                fontSize = textSize,
                fontWeight = FontWeight.Bold,
                lineHeight = textSize,
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
            ),
        )
    }
}

/** Twitch's outcome colors. */
private val PredictionBlue = Color(0xFF387AFF)
private val PredictionPink = Color(0xFFF5009B)
