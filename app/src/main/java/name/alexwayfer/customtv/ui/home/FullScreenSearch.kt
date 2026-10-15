package name.alexwayfer.customtv.ui.home

import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarColors
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.ui.rememberHeldWhileLoading
import name.alexwayfer.customtv.ui.theme.TwitchDivider
import name.alexwayfer.customtv.ui.theme.TwitchHint
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.theme.TwitchSurfaceAlt
import name.alexwayfer.customtv.ui.theme.TwitchText

private val SearchErrorColor = Color(0xFFEB0400)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun searchInputColors(): TextFieldColors = SearchBarDefaults.inputFieldColors(
    focusedTextColor = TwitchText,
    unfocusedTextColor = TwitchText,
    cursorColor = TwitchPurple,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun searchBarColors(): SearchBarColors = SearchBarDefaults.colors(
    containerColor = TwitchSurfaceAlt,
    dividerColor = TwitchDivider,
    inputFieldColors = searchInputColors(),
)

/**
 * The app's full-screen search of [state], closing back into [collapsedShape], with its [content] under the field:
 * the suggestions for the [value] typed. Enter calls [onSubmit]; a spinner shows at the field's end while
 * [checking], and the [error] under it. [onExpandedChange] hears when it opens and closes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FullScreenSearch(
    state: SearchBarState,
    collapsedShape: Shape,
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes placeholder: Int,
    onSubmit: () -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    error: String? = null,
    checking: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    // The keyboard belongs to the search's own window while it is open; the screen below ignores it.
    val open = state.currentValue == SearchBarValue.Expanded
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    DisposableEffect(open) {
        currentOnExpandedChange(open)
        onDispose { if (open) currentOnExpandedChange(false) }
    }
    val showChecking = rememberHeldWhileLoading(checking, loading = checking)
    val text = rememberSearchText(value, onValueChange)

    // The search's window takes its status bar icons from the content color here: light, for the dark screen.
    CompositionLocalProvider(LocalContentColor provides TwitchText) {
        ExpandedFullScreenSearchBar(
            state = state,
            inputField = { SearchInputField(text, state, placeholder, onSubmit, error, showChecking) },
            collapsedShape = collapsedShape,
            colors = searchBarColors(),
        ) {
            if (error != null) {
                Text(
                    text = error,
                    color = SearchErrorColor,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 4.dp),
                )
            }
            content()
        }
    }
}

/**
 * The text in the search's field. A state rather than a plain string, so text put in from outside, such as a
 * picked suggestion, gets the cursor at its end instead of keeping the cursor where the typing was. Only typing goes
 * up to [onValueChange]: the text put in from [value] itself must not search again or clear the error.
 */
@Composable
private fun rememberSearchText(value: String, onValueChange: (String) -> Unit): TextFieldState {
    val text = rememberTextFieldState(value)
    LaunchedEffect(value) {
        if (text.text.toString() != value) text.setTextAndPlaceCursorAtEnd(value)
    }
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    LaunchedEffect(text) {
        snapshotFlow { text.text.toString() }.collect { typed ->
            if (typed != currentValue) currentOnValueChange(typed)
        }
    }
    return text
}

/** The full-screen search's field, with a spinner at its end while something typed is checked. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchInputField(
    text: TextFieldState,
    state: SearchBarState,
    @StringRes placeholder: Int,
    onSubmit: () -> Unit,
    error: String?,
    showChecking: Boolean,
) {
    SearchBarDefaults.InputField(
        textFieldState = text,
        searchBarState = state,
        onSearch = { onSubmit() },
        modifier = Modifier
            .fillMaxWidth()
            // The open search gives its field the height it opened out of, the 48dp button or Home's cropped field,
            // and that cut the text under its own padding. The field keeps its full height, centered, overlapping
            // the search's padding above and below it.
            .wrapContentHeight(unbounded = true)
            .semantics { if (error != null) error(error) },
        // Another mode's hint fades in over the last one.
        placeholder = {
            Crossfade(targetState = placeholder, label = "search hint") { hint ->
                Text(stringResource(hint), color = TwitchHint)
            }
        },
        trailingIcon = if (showChecking) {
            {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = TwitchPurple,
                    trackColor = TwitchDivider,
                    strokeWidth = 2.dp,
                )
            }
        } else {
            null
        },
        colors = searchInputColors(),
    )
}
