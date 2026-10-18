package name.alexwayfer.customtv.ui.watch

/** Where the experimental login stands, as far as the chat settings care; the free build never has it. */
internal enum class ExperimentalLoginOffer { Unavailable, CanLogIn, InProgress, LoggedIn }

internal data class ChatBrowserRows(val openInBrowser: Boolean, val experimentalLogin: Boolean)

/**
 * The chat settings send the viewer to the site for Channel Points, polls, and predictions until the experimental
 * login brings them into the app. While it can start, a logged-in viewer is offered it next to the site. Both are
 * for the live chat, so a recording's or a clip's replayed chat offers neither.
 */
internal fun chatBrowserRows(offer: ExperimentalLoginOffer, loggedIn: Boolean, liveChat: Boolean): ChatBrowserRows =
    ChatBrowserRows(
        openInBrowser = liveChat && offer != ExperimentalLoginOffer.LoggedIn,
        experimentalLogin = liveChat && loggedIn && offer == ExperimentalLoginOffer.CanLogIn,
    )
