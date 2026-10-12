package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.text.LinkAnnotation

internal const val CHAT_AUTHOR_LINK_TAG = "author"

/** What a long press on a link in a chat message does. */
internal sealed interface ChatLinkLongPress {
    /** The same as a long press elsewhere on the message. */
    data object SelectMessage : ChatLinkLongPress

    /** Opens the menu with Open and Copy for [url]. */
    data class LinkMenu(val url: String) : ChatLinkLongPress
}

/** Null for a link that gets no touch target of its own. */
internal fun chatLinkLongPress(link: LinkAnnotation): ChatLinkLongPress? = when {
    link is LinkAnnotation.Clickable && link.tag == CHAT_AUTHOR_LINK_TAG -> ChatLinkLongPress.SelectMessage
    link is LinkAnnotation.Url -> ChatLinkLongPress.LinkMenu(link.url)
    else -> null
}
