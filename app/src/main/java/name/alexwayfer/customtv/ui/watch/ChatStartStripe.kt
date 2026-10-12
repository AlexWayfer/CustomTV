package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** The colored stripe along a chat event's start edge, which marks it apart from the messages. */
internal fun Modifier.chatStartStripe(color: Color): Modifier = drawBehind {
    val stripe = 3.dp.toPx()
    val x = if (layoutDirection == LayoutDirection.Rtl) size.width - stripe else 0f
    drawRect(color = color, topLeft = Offset(x, 0f), size = Size(stripe, size.height))
}
