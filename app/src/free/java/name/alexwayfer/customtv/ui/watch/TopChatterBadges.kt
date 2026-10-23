package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp

// Top chatter badges are Premium: the free build shows none.

@Composable
internal fun ProvideTopChatters(
    enabled: Boolean,
    channelLogin: String,
    channelId: String?,
    content: @Composable () -> Unit,
) = content()

@Suppress("FunctionOnlyReturningConstant")
@Composable
internal fun topChatterBadge(login: String): TopChatterBadge? = null

internal fun AnnotatedString.Builder.appendTopChatterBadge(
    badge: TopChatterBadge?,
    sizes: ChatTextSizes,
    inlineContent: MutableMap<String, InlineTextContent>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) = Unit

@Composable
internal fun TopChatterNameIcon(login: String?, badgeSize: Dp) = Unit

@Composable
internal fun TopChatterCardRefreshEffect(login: String) = Unit

@Composable
internal fun ChatterCardTopChatterItem(login: String) = Unit
