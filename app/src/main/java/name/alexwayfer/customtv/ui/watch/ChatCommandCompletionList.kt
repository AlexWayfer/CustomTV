package name.alexwayfer.customtv.ui.watch

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

@Composable
internal fun ChatCommandCompletionList(
    commands: List<ChatCommand>,
    onPick: (ChatCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp),
        contentPadding = PaddingValues(bottom = 4.dp),
    ) {
        items(commands) { command ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusProperties { canFocus = false }
                    .clickable(role = Role.Button, onClick = { onPick(command) })
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = command.word,
                        color = TwitchText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    Text(
                        text = stringResource(command.argument),
                        color = TwitchText,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                Text(
                    text = stringResource(command.description),
                    color = TwitchTextSecondary,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@get:StringRes
private val ChatCommand.argument: Int
    get() = when (this) {
        ChatCommand.User -> R.string.chat_command_user_argument
    }

@get:StringRes
private val ChatCommand.description: Int
    get() = when (this) {
        ChatCommand.User -> R.string.chat_command_user_description
    }
