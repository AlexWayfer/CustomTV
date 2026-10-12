package name.alexwayfer.customtv.ui.watch

import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.mentionBaselineResets
import name.alexwayfer.customtv.chat.newMentionArrived
import name.alexwayfer.customtv.data.AppSettings
import name.alexwayfer.customtv.ui.settings.mentionSoundStillPlaying
import name.alexwayfer.customtv.ui.settings.mentionSoundUri
import name.alexwayfer.customtv.ui.settings.playMentionVibration

/**
 * Live chat alerts. Reads [messages] directly instead of through composition: while the screen is locked
 * the activity is stopped, `collectAsStateWithLifecycle` stops collecting, and recomposition pauses, so a
 * composed list would only alert after unlocking.
 */
@Composable
internal fun MentionFeedback(
    messages: StateFlow<List<ChatMessage>>,
    channel: String,
    selfLogin: String?,
    selfDisplayName: String?,
    settings: AppSettings,
) {
    val timeline = remember(messages) { messages.map { it to 0 } }
    MentionAlerts(timeline, channel, selfLogin, selfDisplayName, settings)
}

/**
 * The alerts behind both the live chat and a replay: [timeline] carries the chat and its jump generation, and
 * [settings] the mention vibration, sound, and keyword phrases.
 */
@Composable
internal fun MentionAlerts(
    timeline: Flow<Pair<List<ChatMessage>, Int>>,
    channel: String,
    selfLogin: String?,
    selfDisplayName: String?,
    settings: AppSettings,
) {
    val context = LocalContext.current
    val settingsState = rememberUpdatedState(settings)
    val loginState = rememberUpdatedState(selfLogin.orEmpty())
    val nameState = rememberUpdatedState(selfDisplayName.orEmpty())
    LaunchedEffect(channel, timeline) {
        var baselineGeneration: Int? = null
        var seen = emptySet<String>()
        var soundJob: Job? = null
        timeline.collect { (current, generation) ->
            if (mentionBaselineResets(baselineGeneration, generation)) {
                baselineGeneration = generation
                seen = current.map { it.id }.toSet()
                return@collect
            }
            val login = loginState.value
            val name = nameState.value
            val alerts = settingsState.value
            val mentioned = newMentionArrived(seen, current, login, name, alerts.keywordPhrases)
            seen = current.map { it.id }.toSet()
            if (!mentioned) return@collect
            if (alerts.mentionVibration) {
                playMentionVibration(
                    context,
                    alerts.mentionVibrationMs,
                    alerts.mentionVibrationPercent,
                )
            }
            if (alerts.mentionSound) {
                soundJob?.cancel()
                soundJob = this@LaunchedEffect.launch { playMentionSound(context, alerts.mentionSoundUri) }
            }
        }
    }
}

private suspend fun playMentionSound(context: Context, storedUri: String) {
    val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.toString().orEmpty()
    val resolved = mentionSoundUri(storedUri, defaultUri)
    if (resolved.isBlank()) return
    val ringtone = RingtoneManager.getRingtone(context, resolved.toUri()) ?: return
    ringtone.audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    try {
        ringtone.play()
        val started = SystemClock.elapsedRealtime()
        while (mentionSoundStillPlaying(SystemClock.elapsedRealtime() - started, ringtone.isPlaying)) {
            delay(50.milliseconds)
        }
    } finally {
        ringtone.stop()
    }
}
