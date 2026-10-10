package name.alexwayfer.customtv.ui.watch

/** Where the experimental login stands, as far as the chat settings care; the free build never has it. */
internal enum class ExperimentalLoginOffer { Unavailable, CanLogIn, InProgress, LoggedIn }

internal data class ChatBrowserRows(val openInBrowser: Boolean, val experimentalLogin: Boolean)

/**
 * The chat settings send the viewer to the site for Channel Points, polls, and predictions until the experimental
 * login brings them into the app. While it can start, a logged-in viewer is offered it next to the site.
 */
internal fun chatBrowserRows(offer: ExperimentalLoginOffer, loggedIn: Boolean): ChatBrowserRows =
    ChatBrowserRows(
        openInBrowser = offer != ExperimentalLoginOffer.LoggedIn,
        experimentalLogin = loggedIn && offer == ExperimentalLoginOffer.CanLogIn,
    )
