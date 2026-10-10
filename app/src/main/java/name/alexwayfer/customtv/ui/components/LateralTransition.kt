package name.alexwayfer.customtv.ui.components

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

/**
 * Moves between pages that sit side by side, such as a list and an item opened from it, or tabs:
 * forward brings the new page in from the end, back from the start. Uses the default springs, the
 * same ones the cards and the composer move with.
 */
internal fun lateralTransform(forward: Boolean): ContentTransform {
    val direction = if (forward) 1 else -1
    return (slideInHorizontally { width -> direction * width } + fadeIn())
        .togetherWith(slideOutHorizontally { width -> -direction * width } + fadeOut())
}
