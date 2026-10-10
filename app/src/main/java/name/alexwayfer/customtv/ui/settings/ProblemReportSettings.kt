package name.alexwayfer.customtv.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.diagnostics.diagnosticReportIntent

/** Sends a report with the whole app log, for a problem the app did not notice itself. */
@Composable
internal fun ProblemReportSettings() {
    val context = LocalContext.current
    SettingsSection(stringResource(R.string.diagnostics_channel))
    ListItem(
        modifier = Modifier.clickable(role = Role.Button) {
            context.startActivity(diagnosticReportIntent(context, withAppLog = true))
        },
        leadingContent = { Icon(painterResource(R.drawable.ic_bug_report), contentDescription = null) },
        headlineContent = { Text(stringResource(R.string.diagnostics_send)) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
