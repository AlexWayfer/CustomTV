package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.theme.TwitchDivider
import name.alexwayfer.customtv.ui.theme.TwitchText

/** "Hide for Yourself" under an expanded notice, shaped like the notice's other Material buttons. */
@Composable
internal fun ChatNoticeHideButton(
    onClick: () -> Unit,
    textSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    ChatNoticeButton(
        text = stringResource(R.string.chat_pinned_hide),
        onClick = onClick,
        textSize = textSize,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
internal fun ChatNoticeButton(
    text: String,
    onClick: () -> Unit,
    textSize: TextUnit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = if (primary) {
        ButtonDefaults.buttonColors()
    } else {
        ButtonDefaults.filledTonalButtonColors(
            containerColor = TwitchDivider,
            contentColor = TwitchText,
        )
    }
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
    ) {
        Text(
            text = text,
            fontSize = textSize,
            fontWeight = FontWeight.Medium,
        )
    }
}
