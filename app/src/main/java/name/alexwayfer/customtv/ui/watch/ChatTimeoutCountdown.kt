package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import name.alexwayfer.customtv.chat.ChatTimeoutCheck
import name.alexwayfer.customtv.chat.chatTimeoutSecondsLeft
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun CheckChatTimeoutOnOpen(input: ChatInput) {
    val channel = input.channel
    val userId = input.user.userId
    val lookup = input.timeoutLookup
    LaunchedEffect(channel.login, channel.timeouts, userId, lookup) {
        if (userId == null || lookup == null) return@LaunchedEffect
        val beforeCheck = channel.timeouts.state.first { it.userId == userId }
        val startedAt = channel.timeouts.nowMillis()
        val check = lookup() ?: return@LaunchedEffect
        channel.timeouts.applyCheck(channel.login, userId, beforeCheck, check, startedAt)
    }
}

/** A refused send locks the draft immediately, while Twitch or history supplies the exact deadline. */
internal suspend fun recoverChatInputTimeout(
    input: ChatInput,
    composer: ChatComposerState,
    reportedDeadlineMillis: Long?,
) {
    val userId = input.user.userId ?: return
    val channel = input.channel.login
    val timeouts = input.channel.timeouts
    val beforeCheck = timeouts.state.value
    val startedAt = timeouts.nowMillis()
    composer.timeoutSendRefused = true
    if (chatTimeoutSecondsLeft(beforeCheck.deadline(channel, userId), startedAt) > 0L) return

    composer.timeoutUnresolved = true
    val check = reportedDeadlineMillis?.let(ChatTimeoutCheck::Until) ?: input.timeoutLookup?.invoke()
    if (check is ChatTimeoutCheck.Until &&
        timeouts.applyCheck(channel, userId, beforeCheck, check, startedAt)
    ) {
        composer.timeoutUnresolved = false
        if (check.deadlineMillis <= timeouts.nowMillis()) {
            composer.timeoutSendRefused = false
        }
        return
    }
    timeouts.recover(channel, userId)
}

@Composable
internal fun rememberChatTimeoutSeconds(deadlineMillis: Long?, nowMillis: () -> Long): Long {
    var seconds by remember(deadlineMillis, nowMillis) {
        mutableLongStateOf(chatTimeoutSecondsLeft(deadlineMillis, nowMillis()))
    }
    LaunchedEffect(deadlineMillis, nowMillis) {
        val deadline = deadlineMillis ?: return@LaunchedEffect
        while (true) {
            val now = nowMillis()
            val left = chatTimeoutSecondsLeft(deadline, now)
            seconds = left
            if (left == 0L) break
            delay((deadline - now).milliseconds - (left - 1).seconds)
        }
    }
    return seconds
}

internal fun formatChatTimeoutCountdown(seconds: Long): String =
    seconds.seconds.toComponents { hours, minutes, remaining, _ ->
        if (hours > 0) "%d:%02d:%02d".format(Locale.ROOT, hours, minutes, remaining)
        else "%d:%02d".format(Locale.ROOT, minutes, remaining)
    }
