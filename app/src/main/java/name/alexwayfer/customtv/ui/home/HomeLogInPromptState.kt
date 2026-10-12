package name.alexwayfer.customtv.ui.home

/** Whether Home offers to log in to Twitch, and the action that puts the offer away until the next log out. */
internal class HomeLogInPromptState(val shown: Boolean, val dismiss: () -> Unit)
