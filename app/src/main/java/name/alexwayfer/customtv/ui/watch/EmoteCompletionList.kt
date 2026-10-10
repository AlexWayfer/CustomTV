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
import name.alexwayfer.customtv.chat.PickerEmote
import name.alexwayfer.customtv.ui.components.SharedEmoteImage
import name.alexwayfer.customtv.ui.theme.TwitchText

@Composable
internal fun EmoteCompletionList(
    emotes: List<PickerEmote>,
    onPick: (PickerEmote) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp),
        contentPadding = PaddingValues(bottom = 4.dp),
    ) {
        // Lazy: a short query matches hundreds of emotes, and only the visible rows are built.
        items(emotes, key = { it.name }) { emote ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusProperties { canFocus = false }
                    .clickable(role = Role.Button, onClick = { onPick(emote) })
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SharedEmoteImage(
                    url = emote.url,
                    contentDescription = null,
                    size = 24.dp,
                    aspectRatio = emote.aspectRatio,
                )
                Text(
                    text = emote.name,
                    color = TwitchText,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
