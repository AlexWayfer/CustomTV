package name.alexwayfer.customtv.ui

import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset

/**
 * A hidden mini player stays composed, so video and chat keep running, but moves past the right
 * edge: it covers nothing, takes no touches, and TalkBack does not reach it.
 */
internal fun Modifier.offscreenWhen(hidden: Boolean, screenWidthPx: Int): Modifier =
    if (hidden) offset { IntOffset(screenWidthPx, 0) }.clearAndSetSemantics { } else this
