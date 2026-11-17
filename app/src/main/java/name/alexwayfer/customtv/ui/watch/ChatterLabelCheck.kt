package name.alexwayfer.customtv.ui.watch

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChatterCheckChannel
import name.alexwayfer.customtv.data.ChatterCheckMessage
import name.alexwayfer.customtv.data.ChatterCheckResult
import name.alexwayfer.customtv.data.ChatterCheckTwitchRequirement
import name.alexwayfer.customtv.data.ChatterLabelChecks
import name.alexwayfer.customtv.data.LabeledChatter
import name.alexwayfer.customtv.data.chatterCheckTwitchRequirement
import name.alexwayfer.customtv.data.offsetTimeMillisOrNull
import name.alexwayfer.customtv.ui.asString
import name.alexwayfer.customtv.ui.components.TextButton

internal val LocalChatterCheckChannel = compositionLocalOf<ChatterCheckChannel?> { null }
private val numericChannelId = Regex("[0-9]+")

@Composable
internal fun ChatterLabelCheck(script: String, user: LabeledChatter, channel: ChatterCheckChannel?, enabled: Boolean) {
    val experimentalLogin = experimentalLoginOffer()
    val loggedIn = experimentalLogin == ExperimentalLoginOffer.LoggedIn
    key(script, user, channel, loggedIn) {
        val requirement = remember { chatterCheckTwitchRequirement(script) }
        var result by remember { mutableStateOf<ChatterCheckResult?>(null) }
        var running by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val needsLogin = !loggedIn &&
            (requirement == ChatterCheckTwitchRequirement.Session || result == ChatterCheckResult.TwitchLoginRequired)
        Column(modifier = Modifier.padding(start = 48.dp, end = 16.dp, bottom = 8.dp)) {
            when {
                requirement == ChatterCheckTwitchRequirement.Browser ->
                    DisabledChatterCheck(R.string.chatter_check_twitch_login)
                needsLogin -> {
                    DisabledChatterCheck(R.string.chatter_check_twitch_session_required)
                    if (enabled && experimentalLogin == ExperimentalLoginOffer.CanLogIn) ChatterCheckLogInButton()
                }
                running -> TextButton(onClick = {}, enabled = false) {
                    Text(stringResource(R.string.chatter_check_running))
                }
                else -> ChatterCheckOutcome(result, enabled) {
                    running = true
                    scope.launch {
                        try {
                            result = ChatterLabelChecks.run(context, script, user, channel, twitchAvailable = loggedIn)
                        } finally {
                            running = false
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DisabledChatterCheck(@StringRes message: Int) {
    TextButton(onClick = {}, enabled = false) { Text(stringResource(R.string.chatter_check_run)) }
    Text(
        text = stringResource(message),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun ChatterCheckOutcome(result: ChatterCheckResult?, enabled: Boolean, onRun: () -> Unit) {
    when (result) {
        is ChatterCheckResult.Done -> {
            Text(result.text.asString(), style = MaterialTheme.typography.bodyMedium)
            if (result.messages.isNotEmpty()) ChatterCheckMessages(result.messages)
        }
        is ChatterCheckResult.Failed -> {
            Text(
                text = result.text.asString(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onRun, enabled = enabled) { Text(stringResource(R.string.retry)) }
        }
        else -> TextButton(onClick = onRun, enabled = enabled) { Text(stringResource(R.string.chatter_check_run)) }
    }
}

@Composable
private fun ChatterCheckMessages(messages: List<ChatterCheckMessage>) {
    var open by remember { mutableStateOf(false) }
    val title = stringResource(R.string.chatter_check_messages, messages.size)
    TextButton(onClick = { open = true }) { Text(title) }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(messages) { message -> ChatterCheckMessageRow(message) }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.close)) } },
        )
    }
}

@Composable
private fun ChatterCheckMessageRow(message: ChatterCheckMessage) {
    val locale = LocalConfiguration.current.locales[0]
    val instant = remember(message.timestamp) {
        try {
            offsetTimeMillisOrNull(message.timestamp)?.let(Instant::ofEpochMilli)
        } catch (_: ArithmeticException) {
            null
        }
    }
    Column {
        if (instant != null) {
            val time = remember(instant, locale) {
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(locale)
                    .withZone(ZoneId.systemDefault()).format(instant)
            }
            val link = remember(message, instant) { chatterCheckMessageLink(message, instant) }
            val uriHandler = LocalUriHandler.current
            if (link != null) {
                TextButton(onClick = { uriHandler.openUri(link) }) { Text(time) }
            } else {
                Text(time, style = MaterialTheme.typography.labelMedium)
            }
        }
        Text(message.text, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun chatterCheckMessageLink(message: ChatterCheckMessage, instant: Instant): String? {
    if (message.id.isBlank() || !numericChannelId.matches(message.channelId)) return null
    return Uri.Builder().scheme("https").authority("lurkology.com").path("logs")
        .appendQueryParameter("c", "id:${message.channelId}")
        .appendQueryParameter("d", instant.atOffset(ZoneOffset.UTC).toLocalDate().toString())
        .fragment(message.id)
        .build().toString()
}
