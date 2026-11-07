package name.alexwayfer.customtv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import name.alexwayfer.customtv.ui.watch.OneShotRequest

internal class SettingsSearchFocus(
    val title: String,
    val hint: String?,
    val scroll: OneShotRequest<Unit>?,
    val onScrolled: () -> Unit,
)

internal val LocalSettingsSearchFocus = staticCompositionLocalOf<SettingsSearchFocus?> { null }

@Composable
internal fun settingsSearchFocusFor(
    entry: SettingsSearchEntry?,
    scroll: OneShotRequest<Unit>?,
    onScrolled: () -> Unit,
): SettingsSearchFocus? = entry?.let {
    SettingsSearchFocus(
        title = stringResource(it.anchorTitle),
        hint = it.anchorHint?.let { hint -> stringResource(hint) },
        scroll = scroll,
        onScrolled = onScrolled,
    )
}

/** Existing controls keep their layout and toggle state; the selected one gains a background and scroll request. */
@Composable
internal fun Modifier.settingsSearchAnchor(title: String, hint: String? = null): Modifier {
    val focus = LocalSettingsSearchFocus.current ?: return this
    if (focus.title != title || (focus.hint != null && focus.hint != hint)) return this
    val requester = remember { BringIntoViewRequester() }
    var placed by remember { mutableStateOf(false) }
    LaunchedEffect(placed, focus.scroll) {
        if (!placed || focus.scroll?.take() == null) return@LaunchedEffect
        requester.bringIntoView()
        focus.onScrolled()
    }
    return this
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .semantics { selected = true }
        .bringIntoViewRequester(requester)
        .onGloballyPositioned { placed = true }
}
