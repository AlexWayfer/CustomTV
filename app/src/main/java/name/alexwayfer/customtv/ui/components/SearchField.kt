package name.alexwayfer.customtv.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import name.alexwayfer.customtv.R

/**
 * A one-line search field with a magnifier, and a button that clears it once something is typed. A smaller
 * [contentPadding] makes it lower than Material's standard field.
 */
@Composable
internal fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    @StringRes placeholder: Int,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = OutlinedTextFieldDefaults.contentPadding(),
) {
    PaddedOutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        contentPadding = contentPadding,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(placeholder)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            AnimatedVisibility(visible = query.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.search_clear))
                }
            }
        },
    )
}
