package name.alexwayfer.customtv.ui.account

/** What the profile does once a swipe comes to rest on a page. */
internal sealed interface ProfilePageSettled {
    data class ShowTab(val tab: Int) : ProfilePageSettled

    /** The page past the last tab stands for Chat: it opens the stream, and the profile goes back to its first tab. */
    data object OpenChat : ProfilePageSettled
}
