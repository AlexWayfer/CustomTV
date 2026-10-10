package name.alexwayfer.customtv.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.core.net.toUri
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.diagnostics.AppLog

private const val TAG = "LanguageSettings"

/**
 * The app's language row. It opens Android's per-app language screen, which exists since Android 13; older
 * versions show nothing here and follow the system language.
 */
@Composable
internal fun LanguageSettings() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val language = locale.getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) }
    SettingsSection(stringResource(R.string.settings_section_language))
    ListItem(
        modifier = Modifier.clickable(role = Role.Button) {
            val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS, "package:${context.packageName}".toUri())
            try {
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                AppLog.w(TAG, "app locale settings unavailable")
            }
        },
        leadingContent = { Icon(painterResource(R.drawable.ic_language), contentDescription = null) },
        headlineContent = { Text(language) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
