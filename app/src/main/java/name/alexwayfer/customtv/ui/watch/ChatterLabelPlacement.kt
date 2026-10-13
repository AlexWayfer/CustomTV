package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage

/** The labels [look] has for [userId], as badges after the chat badges. */
internal fun AnnotatedString.Builder.appendChatterLabelBadges(
    userId: String?,
    look: ChatMessageLook,
    inlineContent: MutableMap<String, InlineTextContent>,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    userId?.let(look.chatterLabels::get)?.forEachIndexed { index, label ->
        val key = "chatter_label_$index"
        appendInlineContent(key, label.name)
        inlineContent[key] = badgeInlineContent(
            model = label.image,
            contentDescription = label.name,
            sizes = look.sizes,
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }
}

/** The chatter's labels as images for a `Row` that shows the name next to its badges. */
@Composable
internal fun ChatterLabelIcons(userId: String?, badgeSize: Dp) {
    val labels = userId?.let(LocalChatterLabels.current::get).orEmpty()
    labels.forEach { label ->
        AsyncImage(
            model = label.image,
            contentDescription = label.name,
            modifier = Modifier
                .padding(end = BadgeGap)
                .size(badgeSize),
            contentScale = ContentScale.Fit,
        )
    }
}
