package name.alexwayfer.customtv.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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

/** The colors of a Premium button, whose gold comes from [goldButtonFill]. */
@Composable
internal fun goldButtonColors() = ButtonDefaults.buttonColors(
    containerColor = Color.Transparent,
    contentColor = TwitchBg,
)

// The play triangle of the Premium logo: pale gold at the top left, deep gold at the bottom right, and a light rim.
private val GoldButtonBrush = Brush.linearGradient(
    listOf(lerp(PremiumGold, Color.White, 0.1f), PremiumGold, lerp(PremiumGold, Color.Black, 0.1f)),
)
private val GoldButtonRim = Color.White.copy(alpha = 0.6f)

/** The gold of a Premium button with [goldButtonColors], in the shape of a filled button. */
internal fun Modifier.goldButtonFill(): Modifier = this
    .background(GoldButtonBrush, CircleShape)
    .border(0.5.dp, GoldButtonRim, CircleShape)

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
