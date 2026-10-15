package name.alexwayfer.customtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.theme.TwitchText

private val StreamTagShape = RoundedCornerShape(percent = 50)

/**
 * Tag [pills] in a single line that scrolls sideways, faded at an edge with more beyond it; a drag along it never
 * swipes the pager around it.
 */
@Composable
internal fun OneLineTagPills(modifier: Modifier = Modifier, pills: @Composable RowScope.() -> Unit) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .nestedScroll(KeepHorizontalDrag)
            .horizontalScrollFade(scrollState)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(TagPillSpacing),
        content = pills,
    )
}

/** Tag [pills], running on to more lines as they need. */
@Composable
internal fun WrappingTagPills(modifier: Modifier = Modifier, pills: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(TagPillSpacing),
        verticalArrangement = Arrangement.spacedBy(TagPillSpacing),
        content = pills,
    )
}

/** The room between tag pills, across and down. */
private val TagPillSpacing = 4.dp

/**
 * One of a stream's tags as a small pill; a [selected] tag is purple. [modifier] goes on the clipped pill, so a
 * click on it ripples inside the pill.
 */
@Composable
internal fun StreamTagPill(text: String, modifier: Modifier = Modifier, selected: Boolean = false) {
    Text(
        text = text,
        color = TwitchText,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 20.sp,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        style = TextStyle(
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None,
            ),
        ),
        modifier = Modifier
            .clip(StreamTagShape)
            .background(if (selected) TwitchPurple else Color.White.copy(alpha = 0.15f))
            .then(modifier)
            .padding(horizontal = 8.dp),
    )
}
