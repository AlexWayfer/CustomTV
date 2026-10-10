package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChannelAvatarRepository
import name.alexwayfer.customtv.data.ChatRulesStore
import name.alexwayfer.customtv.ui.components.ChannelAvatar

/**
 * The channel's rules come before the user's first message: only when the user can send, the
 * channel has rules, and the user has not confirmed these exact rules before. Until the stored
 * confirmation is read ([acknowledgedFingerprint] still loading), nothing is shown.
 */
internal fun chatRulesNeedConfirmation(
    canSend: Boolean,
    rules: List<String>?,
    rulesFingerprint: String?,
    confirmationLoaded: Boolean,
    acknowledgedFingerprint: String?,
): Boolean =
    canSend && confirmationLoaded && !rules.isNullOrEmpty() && rulesFingerprint != acknowledgedFingerprint

/** What opened the rules: something the user reached for, or the chat settings item. */
internal enum class ChatRulesTrigger { MessageField, EmotePicker, Send, Settings }

internal enum class ChatRulesFollowUp { Keyboard, EmotePicker, Nothing }

/**
 * Confirming the rules continues with what the user reached for: the emote picker button opens the
 * picker, a tap into the field or a send attempt returns to the field with the keyboard, and the
 * chat settings item just closes the rules.
 */
internal fun chatRulesFollowUp(trigger: ChatRulesTrigger): ChatRulesFollowUp = when (trigger) {
    ChatRulesTrigger.EmotePicker -> ChatRulesFollowUp.EmotePicker
    ChatRulesTrigger.MessageField, ChatRulesTrigger.Send -> ChatRulesFollowUp.Keyboard
    ChatRulesTrigger.Settings -> ChatRulesFollowUp.Nothing
}

internal class ChatRulesGate(private val store: ChatRulesStore, private val channelId: String?) {
    var confirmationLoaded by mutableStateOf(false)
        private set
    private var acknowledgedFingerprint by mutableStateOf<String?>(null)

    suspend fun loadConfirmation() {
        val id = channelId?.takeIf { it.isNotBlank() } ?: return
        acknowledgedFingerprint = store.acknowledgedFingerprint(id)
        confirmationLoaded = true
    }

    fun needsConfirmation(canSend: Boolean, rules: List<String>?, rulesFingerprint: String?): Boolean =
        chatRulesNeedConfirmation(canSend, rules, rulesFingerprint, confirmationLoaded, acknowledgedFingerprint)

    fun acknowledge(scope: CoroutineScope, rulesFingerprint: String?) {
        val id = channelId?.takeIf { it.isNotBlank() } ?: return
        val fingerprint = rulesFingerprint ?: return
        acknowledgedFingerprint = fingerprint
        scope.launch { store.acknowledge(id, fingerprint) }
    }
}

/** Reads which rules of this channel the user already confirmed. */
@Composable
internal fun rememberChatRulesGate(channelId: String?): ChatRulesGate {
    val context = LocalContext.current
    val gate = remember(channelId) { ChatRulesGate(ChatRulesStore(context), channelId) }
    LaunchedEffect(gate) { gate.loadConfirmation() }
    return gate
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatRulesSheet(
    channelLogin: String,
    channelName: String,
    rules: List<String>,
    /** Null when there is nothing to confirm: these rules were confirmed, or the user cannot send. */
    onConfirm: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    DismissWhenPlayerContentHidden(onDismiss)
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .semantics(mergeDescendants = true) { heading() },
        ) {
            ChannelAvatar(
                channel = channelLogin,
                profile = ChannelAvatarRepository.cached(channelLogin),
                size = 48.dp,
                modifier = Modifier.clearAndSetSemantics {},
            )
            Column {
                Text(
                    text = channelName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.chat_rules_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            rules.forEachIndexed { index, rule ->
                Text(
                    text = if (rules.size == 1) rule else stringResource(R.string.chat_rule_numbered, index + 1, rule),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        if (onConfirm != null) {
            Button(
                onClick = {
                    scope.launch {
                        sheetState.hide()
                        onConfirm()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
            ) {
                Text(stringResource(R.string.chat_rules_confirm))
            }
        }
    }
}
