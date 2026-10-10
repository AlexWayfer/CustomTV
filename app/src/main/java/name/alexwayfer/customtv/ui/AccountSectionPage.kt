package name.alexwayfer.customtv.ui

internal enum class AccountSectionPage {
    LoginLoader,
    Profile,
    Empty,
}

/**
 * A login in progress shows the loader, also when it replaces an account still on screen, such as a login again for
 * new permissions: the browser may take a moment to open, and the old profile would look like nothing happened.
 */
internal fun accountSectionPage(loggingIn: Boolean, signedIn: Boolean): AccountSectionPage = when {
    loggingIn -> AccountSectionPage.LoginLoader
    signedIn -> AccountSectionPage.Profile
    else -> AccountSectionPage.Empty
}
