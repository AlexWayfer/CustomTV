package name.alexwayfer.customtv.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.theme.PremiumGold
import name.alexwayfer.customtv.ui.theme.TwitchBg

/** The Premium section: the free build lists what Premium adds, the paid build thanks. */
@Composable
internal fun PremiumSettings() {
    SettingsSection(stringResource(R.string.settings_section_premium))
    EditionSettings()
}

@Composable
internal fun goldButtonColors() = ButtonDefaults.buttonColors(
    containerColor = PremiumGold,
    contentColor = TwitchBg,
)

@Composable
internal fun PremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = colors,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium)
    }
}

internal fun openLink(context: Context, link: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, link.toUri()))
    } catch (_: ActivityNotFoundException) {
        return
    }
}
