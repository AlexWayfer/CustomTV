package name.alexwayfer.customtv.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import kotlinx.coroutines.launch

/**
 * Slides a child from where it was placed last to its new place, so rows that change order in a plain Column move
 * instead of jumping. The child keeps it across the move when it is composed under a `key`.
 */
internal fun Modifier.placementAnimation(durationMillis: Int): Modifier =
    this then PlacementAnimationElement(durationMillis)

private data class PlacementAnimationElement(val durationMillis: Int) : ModifierNodeElement<PlacementAnimationNode>() {
    override fun create() = PlacementAnimationNode(durationMillis)

    override fun update(node: PlacementAnimationNode) {
        node.durationMillis = durationMillis
    }
}

private class PlacementAnimationNode(var durationMillis: Int) : Modifier.Node(), LayoutModifierNode {
    private var offset: Animatable<IntOffset, AnimationVector2D>? = null

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            val place = coordinates?.positionInParent()?.round()
            if (place == null) {
                placeable.place(IntOffset.Zero)
                return@layout
            }
            val animated = offset ?: Animatable(place, IntOffset.VectorConverter).also { offset = it }
            if (animated.targetValue != place) {
                coroutineScope.launch { animated.animateTo(place, tween(durationMillis)) }
            }
            // Reading the animated value here places the child again on every frame, without a recomposition.
            placeable.place(animated.value - place)
        }
    }
}
