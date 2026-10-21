package name.alexwayfer.customtv.ui.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.MainActivity
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.components.TextButton

/** The row in the settings that opens the licenses page. */
@Composable
internal fun LicensesSettingsEntry(onOpen: () -> Unit) {
    SettingsPageEntry(
        icon = painterResource(R.drawable.ic_license),
        title = stringResource(R.string.settings_licenses),
        hint = stringResource(R.string.settings_licenses_hint),
        onOpen = onOpen,
    )
}

/** The app's own code when it is open, and the libraries and icons it is built with: each opening its site. */
@Composable
internal fun LicensesSettingsPage(modifier: Modifier = Modifier) {
    val activity = LocalActivity.current as MainActivity
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
    ) {
        softwareCredits.forEach { credit ->
            CreditRow(credit, onOpen = { activity.openExternalLink(credit.url) })
        }
        // The Apache License asks for a copy of its text to go with the software.
        ApacheLicenseText()
    }
}

@Composable
private fun CreditRow(credit: Credit, onOpen: () -> Unit) {
    ListItem(
        headlineContent = { Text(credit.name) },
        supportingContent = {
            Column {
                Text(stringResource(credit.role))
                Text(credit.license.title, style = MaterialTheme.typography.bodySmall)
            }
        },
        trailingContent = { Icon(painterResource(R.drawable.ic_open_in_new), contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onOpen),
    )
}

/** The Apache License text the app carries, shown on request. */
@Composable
private fun ApacheLicenseText() {
    val resources = LocalResources.current
    val text = remember { resources.openRawResource(R.raw.apache_license_2_0).bufferedReader().use { it.readText() } }
    var shown by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { shown = !shown }, modifier = Modifier.padding(horizontal = 8.dp)) {
        Text(stringResource(if (shown) R.string.licenses_hide_apache else R.string.licenses_show_apache))
    }
    AnimatedVisibility(visible = shown, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
