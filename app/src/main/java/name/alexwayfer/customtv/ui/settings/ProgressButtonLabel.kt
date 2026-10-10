package name.alexwayfer.customtv.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

/**
 * A button's label that turns into a spinner while its request is out. The text keeps its place, so the button
 * does not change its width, and still names the button for TalkBack.
 */
@Composable
internal fun ProgressButtonLabel(text: String, inProgress: Boolean) {
    // The same spring as the spinner's fade, so the text and the spinner cross at once.
    val textAlpha by animateFloatAsState(
        targetValue = if (inProgress) 0f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "progressButtonText",
    )
    Box(contentAlignment = Alignment.Center) {
        Text(text = text, modifier = Modifier.alpha(textAlpha))
        AnimatedVisibility(visible = inProgress, enter = fadeIn(), exit = fadeOut()) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = LocalContentColor.current,
                strokeWidth = 2.dp,
            )
        }
    }
}
