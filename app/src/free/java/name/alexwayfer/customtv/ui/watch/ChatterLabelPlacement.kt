package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp

@Suppress("unused", "UnusedReceiverParameter")
internal fun AnnotatedString.Builder.appendChatterLabelBadges(
    userId: String?,
    look: ChatMessageLook,
    inlineContent: MutableMap<String, InlineTextContent>,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) = Unit

@Suppress("unused")
@Composable
internal fun ChatterLabelIcons(userId: String?, badgeSize: Dp) = Unit

/** The free chatter card has no Labels section. */
@Suppress("unused")
@Composable
internal fun ChatterCardLabels(userId: String?, onManage: () -> Unit) = Unit

/** The free chatter card never opens the labels page. */
@Suppress("unused")
@Composable
internal fun ChatterCardLabelsPage(
    userId: String,
    login: String,
    displayName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) = Unit
