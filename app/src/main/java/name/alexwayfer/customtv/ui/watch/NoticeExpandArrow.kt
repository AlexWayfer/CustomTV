package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import name.alexwayfer.customtv.ui.components.CardFoldSpring

/** The arrow of a card that folds open: it turns over with the card instead of switching icons. */
@Composable
internal fun NoticeExpandArrow(
    expanded: Boolean,
    contentDescription: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, CardFoldSpring, label = "notice arrow")
    Icon(
        imageVector = Icons.Filled.KeyboardArrowDown,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.graphicsLayer { rotationZ = rotation },
    )
}
