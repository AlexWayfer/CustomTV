package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints

/**
 * True while the content under the player stays composed behind a mini player: it keeps its
 * state and layout so expanding does not rebuild the chat, but nothing of it is shown.
 */
internal val LocalPlayerContentHidden = compositionLocalOf { false }

/**
 * True while the content is the chat beside or over a full screen player. A sideways swipe there moves or hides
 * the whole chat, so a row does not take it for a reply.
 */
internal val LocalPlayerContentFullscreen = compositionLocalOf { false }

/** Whether the content under the player is composed; it is laid out once at full size before it may hide. */
internal fun playerContentComposed(
    inPictureInPicture: Boolean,
    compactSettled: Boolean,
    measuredFullSize: Boolean,
): Boolean = !inPictureInPicture && (!compactSettled || measuredFullSize)

/** Closes a sheet, popup, or picker of the player content when the content hides behind the mini player. */
@Composable
internal fun DismissWhenPlayerContentHidden(onDismiss: () -> Unit) {
    val hidden = LocalPlayerContentHidden.current
    val dismiss = rememberUpdatedState(onDismiss)
    LaunchedEffect(hidden) {
        if (hidden) dismiss.value()
    }
}

/**
 * Measures hidden content at its last full size and does not place it, so it is neither drawn,
 * touched, nor announced, and keeps its measurements for when it shows again.
 */
internal fun Modifier.hiddenAtFullSize(hidden: Boolean, fullSize: Constraints): Modifier =
    if (!hidden) {
        this
    } else {
        clearAndSetSemantics { }
            .layout { measurable, constraints ->
                measurable.measure(fullSize)
                layout(constraints.minWidth, constraints.minHeight) { }
            }
    }
