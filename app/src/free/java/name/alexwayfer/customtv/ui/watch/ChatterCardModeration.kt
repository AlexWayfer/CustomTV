package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import name.alexwayfer.customtv.chat.ChatBadge
import name.alexwayfer.customtv.chat.ChatMessage

/** Moderator tools are Premium: the free chatter card keeps no moderator state. */
internal class ChatterModTools

private val NoModTools = ChatterModTools()

@Suppress("unused")
@Composable
internal fun rememberChatterModTools(userId: String?, login: String?, badges: List<ChatBadge>): ChatterModTools =
    NoModTools

/** The free chatter card has no Moderation section. */
@Suppress("unused")
@Composable
internal fun ChatterCardModeration(
    tools: ChatterModTools,
    displayName: String,
    message: ChatMessage?,
    renderMessage: @Composable (ChatMessage) -> Unit,
    onOpenLogs: (tab: Int) -> Unit,
    modifier: Modifier = Modifier,
) = Unit

/** The free chatter card never opens the moderation logs. */
@Suppress("unused")
@Composable
internal fun ChatterCardModLogsPage(
    tools: ChatterModTools,
    initialTab: Int,
    renderMessage: @Composable (ChatMessage) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) = Unit
