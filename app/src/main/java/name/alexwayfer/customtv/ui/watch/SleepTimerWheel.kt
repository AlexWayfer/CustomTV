package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A looped wheel of [count] numbers from zero, [step] apart: a flick turns it with a light tick on each number, and
 * it settles with one number in the middle row. TalkBack reads it as an adjustable value.
 */
@Composable
internal fun SleepTimerWheel(
    count: Int,
    step: Int,
    initial: Int,
    label: String,
    // What TalkBack calls the wheel, where the short [label] would read poorly.
    description: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The middle row shows the first visible item's neighbor below.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = wheelStartIndex(initial / step, count) - 1)
    val centerIndex by remember(listState) {
        derivedStateOf { wheelCenterIndex(listState.layoutInfo) ?: (listState.firstVisibleItemIndex + 1) }
    }
    val value = wheelValue(centerIndex, count)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(listState, count, step) {
        var first = true
        snapshotFlow { wheelValue(centerIndex, count) }.collect {
            if (!first) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            first = false
            currentOnValueChange(it * step)
        }
    }
    val scope = rememberCoroutineScope()
    Box(
        modifier = modifier
            .width(WheelWidth)
            .height(WheelItemHeight * WHEEL_VISIBLE_ROWS)
            .semantics {
                contentDescription = description
                stateDescription = (value * step).toString()
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = (value * step).toFloat(),
                    range = 0f..((count - 1) * step).toFloat(),
                    steps = count - 2,
                )
                setProgress { target ->
                    val index = centerIndex - value + (target / step).roundToInt().coerceIn(0, count - 1)
                    scope.launch { listState.scrollToItem(index - 1) }
                    true
                }
            },
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            modifier = Modifier
                .fillMaxSize()
                .clearAndSetSemantics { },
        ) {
            items(WHEEL_ITEM_COUNT) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WheelItemHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = (wheelValue(index, count) * step).toString(),
                        color = if (index == centerIndex) TwitchPurple else TwitchTextSecondary,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -WheelItemHeight / 2),
            color = TwitchTextSecondary,
        )
        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = WheelItemHeight / 2),
            color = TwitchTextSecondary,
        )
        // The unit stays beside the chosen number, so it does not read as one more row of the wheel.
        Text(
            text = label,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .clearAndSetSemantics { },
            color = TwitchTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** The visible item nearest the middle of the wheel; null before the first layout. */
private fun wheelCenterIndex(layoutInfo: LazyListLayoutInfo): Int? {
    val middle = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    return layoutInfo.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - middle) }?.index
}

/** The item that shows [value] near the middle of the looped list, so the wheel turns far both ways. */
internal fun wheelStartIndex(value: Int, count: Int): Int {
    val middle = WHEEL_ITEM_COUNT / 2
    return middle - middle % count + value
}

internal fun wheelValue(index: Int, count: Int): Int = index.mod(count)

// Long enough that no flick reaches an end; the list composes only the visible rows.
private const val WHEEL_ITEM_COUNT = 100_000
private const val WHEEL_VISIBLE_ROWS = 3
private val WheelItemHeight = 48.dp
private val WheelWidth = 96.dp
