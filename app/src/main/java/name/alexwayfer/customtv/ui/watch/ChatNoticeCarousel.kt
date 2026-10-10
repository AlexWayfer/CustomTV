package name.alexwayfer.customtv.ui.watch

import android.view.ViewConfiguration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.components.rememberLastNonNull
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary
import kotlin.math.roundToInt

/**
 * The notices as pages side by side, one at a time. A single notice is one page without dots; a
 * second one makes room for the dots under the cards, which grows in as the dots fade in, and shrinks
 * back as they fade out when only one notice is left.
 */
@Composable
internal fun ChatNoticeCarousel(
    pages: List<ChatNoticePage>,
    firstArrivalOrder: Long?,
    anyExpanded: Boolean,
    content: @Composable (ChatNoticePage, Dp) -> Unit,
) {
    val multiPage = pages.size > 1
    val dotInset by animateDpAsState(if (multiPage) chatNoticeDotInset() else 0.dp, label = "notice dot inset")
    val dotsAlpha by animateFloatAsState(if (multiPage) 1f else 0f, label = "notice dots")
    // The dots fade out with the pages they stood for.
    val dotPages = rememberLastNonNull(pages.takeIf { multiPage })
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val pageWidth = maxWidth
        val pageWidthPx = with(LocalDensity.current) { pageWidth.roundToPx() }
        val scroll = rememberScrollState()
        val context = LocalContext.current
        val minFlingVelocity = remember(context) {
            ViewConfiguration.get(context).scaledMinimumFlingVelocity.toFloat()
        }
        val flingBehavior = remember(scroll, pageWidthPx, pages.size, minFlingVelocity) {
            ChatNoticeFlingBehavior(scroll, pageWidthPx, pages.size, minFlingVelocity)
        }
        val scope = rememberCoroutineScope()
        var selected by remember { mutableIntStateOf(0) }
        LaunchedEffect(pageWidthPx, pages.size) {
            snapshotFlow {
                if (pageWidthPx <= 0) {
                    0
                } else {
                    (scroll.value.toFloat() / pageWidthPx).roundToInt().coerceIn(0, pages.lastIndex)
                }
            }.collect { selected = it }
        }
        // The newest notice takes the first page; show it when it arrives or when the order thaws.
        val first = pages.first()
        LaunchedEffect(first, firstArrivalOrder, anyExpanded, pageWidthPx) {
            if (!anyExpanded && pageWidthPx > 0) scroll.animateScrollTo(0)
        }
        LaunchedEffect(scroll.isScrollInProgress, pages.size, pageWidthPx) {
            if (scroll.isScrollInProgress || pageWidthPx <= 0 || pages.size < 2) return@LaunchedEffect
            val page = (scroll.value.toFloat() / pageWidthPx).roundToInt().coerceIn(0, pages.lastIndex)
            val target = page * pageWidthPx
            if (scroll.value != target) scroll.animateScrollTo(target)
        }
        Row(Modifier.horizontalScroll(scroll, enabled = multiPage, flingBehavior = flingBehavior)) {
            pages.forEach { page ->
                Box(Modifier.width(pageWidth)) {
                    content(page, dotInset)
                    if (dotPages != null && (multiPage || dotsAlpha > 0f)) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = ChatNoticeDotBottomPadding)
                                .graphicsLayer { alpha = dotsAlpha }
                                .then(if (multiPage) Modifier else Modifier.clearAndSetSemantics {}),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            dotPages.forEachIndexed { index, notice ->
                                val label = stringResource(
                                    when (notice) {
                                        ChatNoticePage.Raid -> R.string.chat_notice_show_raid
                                        ChatNoticePage.HypeTrain -> R.string.chat_notice_show_hype_train
                                        ChatNoticePage.Poll -> R.string.chat_notice_show_poll
                                        ChatNoticePage.Prediction -> R.string.chat_notice_show_prediction
                                        ChatNoticePage.CommunityGift -> R.string.chat_notice_show_community_gift
                                        ChatNoticePage.Pinned -> R.string.chat_notice_show_pinned
                                    },
                                )
                                val dotColor by animateColorAsState(
                                    if (index == selected) TwitchPurple else TwitchTextSecondary,
                                    label = "notice dot",
                                )
                                Box(
                                    modifier = Modifier
                                        .size(ChatNoticeDotTouch)
                                        .clickable {
                                            scope.launch { scroll.animateScrollTo(index * pageWidthPx) }
                                        }
                                        .semantics {
                                            contentDescription = label
                                            this.selected = index == selected
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(ChatNoticeDotVisual)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                            .clearAndSetSemantics { },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
