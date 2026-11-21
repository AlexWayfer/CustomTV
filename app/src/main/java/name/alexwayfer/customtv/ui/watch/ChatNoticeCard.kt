package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.chat.raidFractionRemaining
import name.alexwayfer.customtv.ui.components.CardFoldSpring
import name.alexwayfer.customtv.ui.components.collapsibleTextMaxLines
import name.alexwayfer.customtv.ui.components.revealLines
import name.alexwayfer.customtv.ui.theme.TwitchDivider
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.theme.TwitchSurface
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary
import kotlin.math.roundToInt

/** The summary keeps its full text while folding and clips it at the animated height. */
@Composable
internal fun ChatNoticeCard(
    expanded: Boolean,
    bottomInset: Dp,
    summary: @Composable (allLines: Boolean, fold: () -> Float, bottomInset: Dp) -> Unit,
    modifier: Modifier = Modifier,
    detailsModifier: Modifier = Modifier,
    detailsAnchor: Alignment.Vertical = Alignment.Bottom,
    footer: (@Composable () -> Unit)? = null,
    insetInCollapsedSummary: Boolean = false,
    details: @Composable ColumnScope.() -> Unit,
) {
    val transition = updateTransition(expanded, label = "notice expansion")
    val fold by transition.animateFloat({ CardFoldSpring }, label = "notice fold") { if (it) 1f else 0f }
    val allLines = transition.currentState || transition.targetState
    // When the inset moves out of the summary, the details open on the same transition.
    val detailsTransition = if (insetInCollapsedSummary) transition else updateTransition(expanded, label = "details")
    val summaryInset = if (insetInCollapsedSummary) {
        bottomInset.coerceAtLeast(0.dp) * (1f - fold.coerceIn(0f, 1f))
    } else {
        0.dp
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = ChatNoticeCardOuterPadding)
            .clip(RoundedCornerShape(8.dp))
            .background(TwitchSurface)
            .blockChatTouches()
            .then(modifier),
    ) {
        summary(allLines, { fold }, summaryInset)
        detailsTransition.AnimatedVisibility(
            visible = { it },
            enter = expandVertically(expandFrom = detailsAnchor),
            exit = shrinkVertically(shrinkTowards = detailsAnchor),
        ) {
            Column(modifier = detailsModifier, content = details)
        }
        footer?.invoke()
        if (bottomInset > 0.dp) Spacer(Modifier.height((bottomInset - summaryInset).coerceAtLeast(0.dp)))
    }
}

/** Keeps the actual deadline when a notice leaves the carousel and returns. */
@Composable
internal fun ChatNoticeCountdownBar(endsAtMillis: Long, durationMillis: Long) {
    val fraction = remember(endsAtMillis, durationMillis) {
        Animatable(raidFractionRemaining(endsAtMillis, System.currentTimeMillis(), durationMillis))
    }
    LaunchedEffect(fraction) {
        val left = (endsAtMillis - System.currentTimeMillis()).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        fraction.animateTo(0f, tween(durationMillis = left, easing = LinearEasing))
    }
    LinearProgressIndicator(
        progress = { fraction.value },
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { },
        color = TwitchPurple,
        trackColor = TwitchDivider,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

@Composable
internal fun ChatNoticeHeader(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    chatTextSize: Int,
    expandLabel: String,
    bottomPadding: Dp = 8.dp,
    arrowTint: Color = TwitchTextSecondary,
    content: @Composable RowScope.() -> Unit,
) {
    val arrowSize = chatDp(chatTextSize, 20, 22)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandedChange(!expanded) }
            .padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = bottomPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
        NoticeExpandArrow(
            expanded = expanded,
            contentDescription = expandLabel,
            tint = arrowTint,
            modifier = Modifier.size(arrowSize),
        )
    }
}

@Composable
internal fun RowScope.ChatNoticeTitle(
    label: String,
    title: String,
    chatTextSize: Int,
    allLines: Boolean,
    fold: () -> Float,
) {
    val headerSize = chatSp(chatTextSize, 12, 14)
    val titleSize = chatSp(chatTextSize, 14, 16)
    var titleFirstLineBottomPx by remember { mutableIntStateOf(0) }
    Column(Modifier.weight(1f)) {
        Text(
            text = label,
            color = TwitchTextSecondary,
            fontSize = headerSize,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = title,
            color = TwitchText,
            fontSize = titleSize,
            fontWeight = FontWeight.Bold,
            maxLines = collapsibleTextMaxLines(allLines),
            overflow = if (allLines) TextOverflow.Clip else TextOverflow.Ellipsis,
            onTextLayout = { layout -> titleFirstLineBottomPx = layout.getLineBottom(0).roundToInt() },
            modifier = Modifier.revealLines({ titleFirstLineBottomPx }, fold),
        )
    }
}
