package name.alexwayfer.customtv.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Text that state hands to a screen. The app's own words are a string resource, read when shown, so they follow the
 * app's language; a server's or library's own message, such as a TDLib error, is shown as it came.
 */
internal sealed interface UiText {
    data class Resource(@param:StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Raw(val text: String) : UiText
}

// stringResource takes the arguments only as a vararg; a data class keeps them as a List, never an Array.
@Suppress("SpreadOperator")
@Composable
internal fun UiText.asString(): String = when (this) {
    is UiText.Resource -> stringResource(id, *args.toTypedArray())
    is UiText.Raw -> text
}
