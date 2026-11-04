package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt

/** Label fields may cover the player; other card pages still stop beneath it. */
internal fun chatterCardSheetMaxHeightPx(
    page: ChatterCardPage,
    screenHeightPx: Int,
    chatMaxHeightPx: Int?,
): Int? = if (page == ChatterCardPage.Labels) {
    (screenHeightPx.coerceAtLeast(0) * LABELS_SHEET_HEIGHT_FRACTION).roundToInt()
} else {
    chatMaxHeightPx
}

/**
 * Keep the activity's system bars while a browser covers the card, but leave keyboard padding to the sheet's own
 * window. Its IME animation must not compete with an activity inset also subtracted from the content height limit.
 */
@Composable
internal fun chatterCardSheetInsets(): WindowInsets =
    chatSheetWindowInsets(WindowInsets.systemBars.union(WindowInsets.displayCutout))

/** Animate the height cap together with the page, so returning to the profile cannot clip the outgoing editor. */
@Composable
internal fun Transition<ChatterCardPage>.animateChatterCardContentMaxHeight(sheetInsets: WindowInsets): State<Dp> =
    animateDp(
        // The same spring as AnimatedContent's default size transform.
        transitionSpec = { spring(stiffness = Spring.StiffnessMediumLow) },
        label = "chatterCardHeightLimit",
    ) { page ->
        chatterCardContentMaxHeight(page, sheetInsets)
    }

@Composable
private fun chatterCardContentMaxHeight(page: ChatterCardPage, sheetInsets: WindowInsets): Dp {
    val screenHeightPx = LocalWindowInfo.current.containerSize.height
    return chatSheetContentMaxHeight(
        sheetInsets = sheetInsets,
        sheetMaxHeightPx = chatterCardSheetMaxHeightPx(
            page = page,
            screenHeightPx = screenHeightPx,
            chatMaxHeightPx = LocalChatSheetMaxHeightPx.current(),
        ) ?: screenHeightPx,
    )
}

/** The keyboard takes the system bar's place; the content budget already leaves room for that bar. */
internal fun chatterCardImeContentMaxHeightPx(contentMaxHeightPx: Int, imeBottomPx: Int, systemBottomPx: Int): Int =
    (contentMaxHeightPx - (imeBottomPx - systemBottomPx).coerceAtLeast(0)).coerceAtLeast(0)

/**
 * Read the sheet window's keyboard inset during measure, just as Material's outer imePadding does. A composition
 * read would trail that padding by a frame and briefly let the card fill the smaller window up to its top edge.
 */
@Composable
internal fun Modifier.chatterCardHeight(
    contentMaxHeight: State<Dp>,
    sheetInsets: WindowInsets,
    imeInsets: WindowInsets = WindowInsets.ime,
): Modifier = layout { measurable, constraints ->
    val maxHeight = chatterCardImeContentMaxHeightPx(
        contentMaxHeightPx = contentMaxHeight.value.roundToPx(),
        imeBottomPx = imeInsets.getBottom(this),
        systemBottomPx = sheetInsets.getBottom(this),
    ).coerceIn(constraints.minHeight, constraints.maxHeight)
    val placeable = measurable.measure(constraints.copy(maxHeight = maxHeight))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

private const val LABELS_SHEET_HEIGHT_FRACTION = 0.85f
