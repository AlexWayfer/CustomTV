package name.alexwayfer.customtv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TwitchColorScheme = darkColorScheme(
    primary = TwitchPurple,
    onPrimary = Color.White,
    primaryContainer = TwitchPurpleHover,
    onPrimaryContainer = Color.White,
    secondary = TwitchTextSecondary,
    onSecondary = Color.White,
    secondaryContainer = TwitchPurple,
    onSecondaryContainer = Color.White,
    background = TwitchBg,
    onBackground = TwitchText,
    surface = TwitchSurface,
    onSurface = TwitchText,
    surfaceVariant = TwitchSurfaceAlt,
    onSurfaceVariant = TwitchTextSecondary,
    outline = TwitchTextSecondary,
    error = Color(0xFFEB0400),
)

@Composable
fun CustomTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TwitchColorScheme,
        typography = Typography,
        content = content,
    )
}
