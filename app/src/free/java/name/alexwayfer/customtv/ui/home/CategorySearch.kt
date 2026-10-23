package name.alexwayfer.customtv.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarState
import androidx.compose.runtime.Composable
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.TwitchCategory
import name.alexwayfer.customtv.ui.watch.OneShotRequest

/** A category opens Explore's streams, which are Premium: the free build's search looks for channels alone. */
@Suppress("FunctionOnlyReturningConstant")
internal fun categorySearchOffered(): Boolean = false

@Suppress("FunctionOnlyReturningConstant")
@StringRes
internal fun categorySearchHint(): Int = R.string.channel_hint

@Composable
internal fun ColumnScope.SearchModePager(
    mode: SearchMode,
    onMode: (SearchMode) -> Unit,
    channels: @Composable () -> Unit,
    categories: @Composable () -> Unit,
) = Unit

@OptIn(ExperimentalMaterial3Api::class)
internal fun openPickedCategory(state: SearchBarState, showsUnderSearch: Boolean, open: () -> Unit) = Unit

@Composable
internal fun CategorySearchSuggestions(
    query: String,
    submit: OneShotRequest<Unit>?,
    onCategory: (TwitchCategory) -> Unit,
) = Unit
