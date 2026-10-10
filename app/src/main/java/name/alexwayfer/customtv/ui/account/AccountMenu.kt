package name.alexwayfer.customtv.ui.account

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import name.alexwayfer.customtv.R

/** The signed-in account's own actions, in a menu in the profile's top corner. */
@Composable
internal fun AccountMenu(
    onLogOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProfileMenu(description = stringResource(R.string.account_menu), modifier = modifier) { close ->
        ExperimentalLoginMenuItem(onClick = close)
        DropdownMenuItem(
            text = { Text(stringResource(R.string.log_out)) },
            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.ExitToApp, contentDescription = null) },
            onClick = {
                close()
                onLogOut()
            },
        )
    }
}

/**
 * The three-dot menu in a profile's top corner, named [description] for TalkBack. [items] get a call that closes
 * the menu.
 */
@Composable
internal fun ProfileMenu(
    description: String,
    modifier: Modifier = Modifier,
    items: @Composable (close: () -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        IconButton(
            onClick = { expanded = true },
            // A darkened circle keeps the icon readable over any banner.
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.4f),
                contentColor = Color.White,
            ),
        ) {
            Icon(Icons.Filled.MoreVert, contentDescription = description)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items { expanded = false }
        }
    }
}
