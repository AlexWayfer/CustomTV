package name.alexwayfer.customtv.auth

internal const val TWITCH_LOGIN_SCOPES =
    "user:read:follows user:write:chat user:read:chat user:read:emotes user:read:subscriptions"

internal fun followListCanLoad(grantedScopes: Set<String>?): Boolean {
    return grantedScopes?.contains("user:read:follows") == true
}

internal fun loginClickShowsScopePrompt(scopeReloginPending: Boolean): Boolean {
    return scopeReloginPending
}

internal fun resumeSavedChannel(
    channelId: String?,
    openedOnHome: Boolean,
    scopeReloginPending: Boolean,
): Boolean = !channelId.isNullOrBlank() && openedOnHome && !scopeReloginPending

internal fun playerYieldsToReloginPrompt(
    reloginForPermissions: Boolean,
    reloginForSession: Boolean,
): Boolean = reloginForPermissions || reloginForSession

internal fun rejectedSessionAsksToLogInAgain(hadAccount: Boolean, alreadyDismissed: Boolean): Boolean {
    return hadAccount && !alreadyDismissed
}

/**
 * A token works when it has every scope this build asks for. Extra scopes are fine: a Premium login
 * keeps working in the free build, and a free login asks again for Premium's scopes after an upgrade.
 */
internal fun tokenScopesMatch(expectedRaw: String, grantedScopes: Set<String>): Boolean {
    val expected = expectedRaw.split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()
    return grantedScopes.containsAll(expected)
}

internal fun savedSessionNeedsNewLogin(grantedScopes: Set<String>?, expectedRaw: String): Boolean {
    return grantedScopes != null && !tokenScopesMatch(expectedRaw, grantedScopes)
}
