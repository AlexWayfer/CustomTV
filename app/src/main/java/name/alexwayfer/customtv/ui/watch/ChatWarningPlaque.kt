package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChatWarning
import name.alexwayfer.customtv.chat.warningAcknowledgeWaitSeconds
import name.alexwayfer.customtv.ui.rememberHeldWhileLoading
import name.alexwayfer.customtv.ui.theme.TwitchLive
import name.alexwayfer.customtv.ui.theme.TwitchSurfaceAlt
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary
import kotlin.time.Duration.Companion.seconds

/**
 * The channel's warning in the message field's place, as on the Twitch web: what may follow, the rules it cites, and
 * its reason. Acknowledge waits a few seconds so the user reads it first. Without acknowledging in the app, the plaque
 * opens the channel's chat in the browser instead, and Close brings the field back until the next refused send.
 */
@Composable
internal fun ChatWarningPlaque(
    gate: ChatWarningGate,
    warning: ChatWarning,
    onOpenInBrowser: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = TwitchSurfaceAlt,
        contentColor = TwitchText,
    ) {
        // A refused send shows the plaque before the reason and rules load, so it grows to them.
        Column(
            modifier = Modifier
                .animateContentSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Warning, contentDescription = null, tint = TwitchText)
                // TalkBack reads the title as the plaque takes the field's place.
                Text(
                    text = stringResource(R.string.chat_warning_title),
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .semantics {
                            heading()
                            liveRegion = LiveRegionMode.Polite
                        },
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Text(
                text = stringResource(R.string.chat_warning_consequences),
                style = MaterialTheme.typography.bodyMedium,
                color = TwitchTextSecondary,
            )
            if (warning.citedRules.isNotEmpty()) {
                WarningSection(stringResource(R.string.chat_warning_rules), warning.citedRules)
            }
            if (warning.reason.isNotBlank()) {
                WarningSection(stringResource(R.string.chat_warning_reason), listOf(warning.reason))
            }
            if (gate.canAcknowledge) {
                AcknowledgeButton(gate, warning)
            } else {
                Text(
                    text = stringResource(R.string.chat_warning_on_web),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TwitchTextSecondary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    TextButton(onClick = gate::clear) { Text(stringResource(R.string.close)) }
                    Button(onClick = onOpenInBrowser) { Text(stringResource(R.string.chat_warning_open_browser)) }
                }
            }
        }
    }
}

/** A titled list of the warning's parts, each after a bullet that TalkBack skips. */
@Composable
private fun WarningSection(title: String, lines: List<String>) {
    Column {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.labelLarge,
        )
        lines.forEach { line ->
            Row(modifier = Modifier.padding(top = 2.dp)) {
                Text(text = "•", modifier = Modifier.clearAndSetSemantics {}, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = line,
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/** Acknowledge counts down its wait first, then sends, with a spinner while Twitch answers. */
@Composable
private fun AcknowledgeButton(gate: ChatWarningGate, warning: ChatWarning) {
    val scope = rememberCoroutineScope()
    val shownAt = remember(warning) { System.currentTimeMillis() }
    var now by remember(warning) { mutableLongStateOf(shownAt) }
    val waitSeconds = warningAcknowledgeWaitSeconds(shownAt, now)
    LaunchedEffect(warning) {
        while (warningAcknowledgeWaitSeconds(shownAt, now) > 0) {
            delay(1.seconds)
            now = System.currentTimeMillis()
        }
    }
    val acknowledging = rememberHeldWhileLoading(gate.acknowledging, gate.acknowledging)
    Button(
        onClick = { scope.launch { gate.acknowledge() } },
        modifier = Modifier.fillMaxWidth(),
        enabled = waitSeconds == 0 && !acknowledging,
    ) {
        when {
            acknowledging -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            waitSeconds > 0 -> Text(pluralStringResource(R.plurals.chat_warning_wait_seconds, waitSeconds, waitSeconds))
            else -> Text(stringResource(R.string.chat_warning_acknowledge))
        }
    }
    AnimatedVisibility(
        visible = gate.acknowledgeFailed,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Text(
            text = stringResource(R.string.chat_warning_acknowledge_failed),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodyMedium,
            color = TwitchLive,
        )
    }
}
