package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.alexwayfer.customtv.chat.ChatNick
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

@Composable
internal fun NickCompletionList(
    nicks: List<ChatNick>,
    onPick: (ChatNick) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp),
        contentPadding = PaddingValues(bottom = 4.dp),
    ) {
        // Lazy: a busy chat has many chatters, and only the visible rows are built.
        items(nicks) { nick ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusProperties { canFocus = false }
                    .clickable(role = Role.Button, onClick = { onPick(nick) })
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = nick.displayName,
                    color = TwitchText,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                )
                if (!nick.displayName.equals(nick.login, ignoreCase = true)) {
                    Text(
                        text = nick.login,
                        color = TwitchTextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}
