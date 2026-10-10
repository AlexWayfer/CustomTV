package name.alexwayfer.customtv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material3.Badge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * How many unread whispers there are, as a Material badge that grows in with the first one and keeps its last
 * number while it shrinks away. The row or the item that holds it announces the count.
 */
@Composable
internal fun UnreadCountBadge(count: Int, modifier: Modifier = Modifier) {
    val shown = rememberLastNonNull(count.takeIf { it > 0 })
    AnimatedVisibility(
        visible = count > 0,
        modifier = modifier,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
    ) {
        Badge { Text(shown?.toString().orEmpty()) }
    }
}
