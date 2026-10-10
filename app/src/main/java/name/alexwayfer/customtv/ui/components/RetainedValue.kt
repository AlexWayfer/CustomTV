package name.alexwayfer.customtv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * The last [value] that was not null, so content that animates out after its value is gone still
 * shows what it showed.
 */
@Composable
internal fun <T : Any> rememberLastNonNull(value: T?): T? {
    val holder = remember { RetainedHolder<T>() }
    if (value != null) holder.value = value
    return holder.value
}

private class RetainedHolder<T : Any> {
    var value: T? = null
}
