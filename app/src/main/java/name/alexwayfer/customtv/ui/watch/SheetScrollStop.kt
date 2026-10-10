package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/**
 * Whether a sheet's scrolling content keeps the rest of a downward drag or fling from the sheet. A gesture that
 * began inside the content, or [contentMoved] it on the way, stops at its top; only a new one from the top closes
 * the sheet. The move covers a start the content misjudged, such as a fling that runs up a whole list.
 */
internal fun sheetContentKeepsDrag(startedAtTop: Boolean, contentMoved: Boolean, availableY: Float): Boolean =
    (!startedAtTop || contentMoved) && availableY > 0f

/**
 * Whether the content keeps the rest of a fling from the sheet: downward as a drag, and always upward. An open sheet
 * has nothing to do with an upward rest but run a settle animation, and while that runs it takes the next touch
 * itself, so a quick drag down at the bottom of a list would close the sheet instead of scrolling.
 */
internal fun sheetContentKeepsFling(startedAtTop: Boolean, contentMoved: Boolean, availableY: Float): Boolean =
    availableY < 0f || sheetContentKeepsDrag(startedAtTop, contentMoved, availableY)

/**
 * For `Modifier.nestedScroll` on a sheet's content that scrolls with [scrollState]: scrolling back up stops at the
 * top instead of carrying on into closing the sheet, which waits for the next gesture. [atTop] says when the content
 * is at its top, such as a header that also has to be expanded. Pass the content's [overscroll] to its
 * `verticalScroll` too, so what the stop keeps from the sheet stretches the content's edge.
 */
@Composable
internal fun rememberStopAtScrollTop(
    scrollState: ScrollState,
    overscroll: OverscrollEffect?,
    atTop: () -> Boolean = { scrollState.value == 0 },
): NestedScrollConnection = rememberStopAtTop(scrollState, overscroll = { overscroll }, atTop = atTop)

/**
 * The same stop for content that scrolls some other way, such as a lazy list; [key] names that content. What the
 * stop keeps from the sheet goes to [overscroll], the content's own edge effect, so the content stretches at its top
 * edge as it does at the bottom.
 */
@Composable
internal fun rememberStopAtTop(
    key: Any,
    overscroll: () -> OverscrollEffect?,
    atTop: () -> Boolean,
): NestedScrollConnection {
    val readAtTop = rememberUpdatedState(atTop)
    val readOverscroll = rememberUpdatedState(overscroll)
    return remember(key) {
        object : NestedScrollConnection {
            private var gestureStarting = true
            private var startedAtTop = true
            private var contentMoved = false

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (gestureStarting && source == NestedScrollSource.UserInput) {
                    startedAtTop = readAtTop.value()
                    contentMoved = false
                    gestureStarting = false
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (consumed.y != 0f) contentMoved = true
                if (!sheetContentKeepsDrag(startedAtTop, contentMoved, available.y)) return Offset.Zero
                val kept = Offset(0f, available.y)
                readOverscroll.value()?.applyToScroll(kept, source) { Offset.Zero }
                return kept
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                gestureStarting = true
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (!sheetContentKeepsFling(startedAtTop, contentMoved, available.y)) return Velocity.Zero
                val kept = Velocity(0f, available.y)
                readOverscroll.value()?.applyToFling(kept) { Velocity.Zero }
                return kept
            }
        }
    }
}
