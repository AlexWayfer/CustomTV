package name.alexwayfer.customtv.ui.account

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.rememberHeldWhileLoading

/**
 * A profile part that did not load, with Retry. While [retrying], a spinner takes the button's place
 * without changing its size.
 */
@Composable
internal fun ProfileLoadFailed(text: String, retrying: Boolean, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val shownRetrying = rememberHeldWhileLoading(retrying, loading = retrying)
    Column(modifier) {
        Text(
            text = text,
            modifier = Modifier.padding(top = 20.dp),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
        )
        TextButton(onClick = onRetry, enabled = !shownRetrying) {
            Box(contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.retry), Modifier.alpha(if (shownRetrying) 0f else 1f))
                if (shownRetrying) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            }
        }
    }
}
