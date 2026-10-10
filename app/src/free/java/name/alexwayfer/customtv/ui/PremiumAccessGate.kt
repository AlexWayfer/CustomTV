package name.alexwayfer.customtv.ui

import androidx.compose.runtime.Composable

/** The free build needs no Premium access. */
@Composable
internal fun PremiumAccessGate(content: @Composable () -> Unit) {
    content()
}
