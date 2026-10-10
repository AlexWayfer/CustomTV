package name.alexwayfer.customtv.auth

import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TwitchOAuthTest {
    @Test
    fun deviceLoginReadsCodeUriAndInterval() {
        val login = parseTwitchDeviceLogin(
            """
            {"device_code":"device","expires_in":1800,"interval":5,"user_code":"ABCDEFGH","verification_uri":"https://www.twitch.tv/activate?public=true&device-code=ABCDEFGH"}
            """.trimIndent(),
        )
        assertEquals("device", login?.deviceCode)
        assertEquals("ABCDEFGH", login?.userCode)
        assertEquals("https://www.twitch.tv/activate?public=true&device-code=ABCDEFGH", login?.verificationUri)
        assertEquals(1800L, login?.expiresInSeconds)
        assertEquals(5L, login?.intervalSeconds)
    }

    @Test
    fun deviceLoginRejectsMissingCodeAndUsesFiveSecondFloor() {
        assertNull(parseTwitchDeviceLogin("""{"user_code":"ABCDEFGH","verification_uri":"https://www.twitch.tv/activate"}"""))
        val login = parseTwitchDeviceLogin(
            """{"device_code":"device","user_code":"ABCDEFGH","verification_uri":"https://www.twitch.tv/activate","expires_in":30,"interval":0}""",
        )
        assertEquals(5L, login?.intervalSeconds)
    }

    @Test
    fun clientHttpRejectionClearsTheSession() {
        assertTrue(rejectedAuthClearsSession(400))
        assertTrue(rejectedAuthClearsSession(401))
    }

    @Test
    fun serverHttpFailureKeepsTheSession() {
        assertFalse(rejectedAuthClearsSession(500))
        assertFalse(rejectedAuthClearsSession(503))
        assertFalse(rejectedAuthClearsSession(200))
    }
    @Test
    fun pendingDeviceGrantKeepsWaiting() {
        val grant = parseDeviceGrant(400, """{"status":400,"message":"authorization_pending"}""")
        assertEquals(DeviceGrant.Pending, grant)
        assertEquals(
            DeviceGrant.Pending,
            parseDeviceGrant(400, """{"status":400,"message":"Bad Request","error":"authorization_pending"}"""),
        )
    }

    @Test
    fun slowDownDeniedAndExpiredDeviceGrants() {
        assertEquals(DeviceGrant.SlowDown, parseDeviceGrant(400, """{"message":"slow_down"}"""))
        assertEquals(DeviceGrant.Denied, parseDeviceGrant(400, """{"message":"access_denied"}"""))
        assertEquals(DeviceGrant.Expired, parseDeviceGrant(400, """{"message":"invalid device code"}"""))
        assertEquals(DeviceGrant.Failed, parseDeviceGrant(400, """{"message":"something else"}"""))
        assertEquals(DeviceGrant.Unavailable(503), parseDeviceGrant(503, """{"message":"slow_down"}"""))
        assertEquals(DeviceGrant.Unavailable(500), parseDeviceGrant(500, ""))
    }

    @Test
    fun approvedDeviceGrantReadsTokens() {
        val grant = parseDeviceGrant(
            200,
            """{"access_token":"a","refresh_token":"r","expires_in":3600}""",
        )
        val approved = grant as DeviceGrant.Approved
        assertEquals("r", approved.tokens.refreshToken)
        assertEquals(3600L, approved.tokens.expiresInSeconds)
    }

    @Test
    fun firstPollWaitsOneSecondThenUsesTwitchInterval() {
        assertEquals(1_000L, devicePollDelayMillis(completedPolls = 0, intervalSeconds = 5))
        assertEquals(5_000L, devicePollDelayMillis(completedPolls = 1, intervalSeconds = 5))
        assertEquals(5_000L, devicePollDelayMillis(completedPolls = 4, intervalSeconds = 5))
        assertEquals(1_000L, devicePollDelayMillis(completedPolls = 2, intervalSeconds = 0))
    }

    @Test
    fun tokenRefreshStartsAMinuteBeforeExpiry() {
        assertFalse(shouldRefreshAccessToken(expiresAtMillis = 120_000, nowMillis = 50_000))
        assertTrue(shouldRefreshAccessToken(expiresAtMillis = 120_000, nowMillis = 60_000))
        assertTrue(shouldRefreshAccessToken(expiresAtMillis = 120_000, nowMillis = 130_000))
        assertEquals(65_000L, accessExpiryMillis(nowMillis = 5_000, expiresInSeconds = 60))
    }

    @Test
    fun aFreshSavedTokenCanLoadFollowsBeforeTheProfileRefresh() {
        assertEquals(
            "token" to "42",
            freshSavedFollowAccess("token", "42", expiresAtMillis = 120_000, nowMillis = 50_000),
        )
    }

    @Test
    fun anExpiringSavedTokenWaitsForRefresh() {
        assertNull(freshSavedFollowAccess("token", "42", expiresAtMillis = 120_000, nowMillis = 60_000))
        assertNull(freshSavedFollowAccess("", "42", expiresAtMillis = 120_000, nowMillis = 50_000))
        assertNull(freshSavedFollowAccess("token", null, expiresAtMillis = 120_000, nowMillis = 50_000))
    }

    @Test
    fun accountJsonUsesDisplayNameAndDropsBlankAvatar() {
        val account = parseTwitchAccount(
            """{"data":[{"login":"alice","display_name":"Alice","profile_image_url":""}]}""",
        )
        assertEquals("alice", account?.login)
        assertEquals("Alice", account?.displayName)
        assertNull(account?.avatarUrl)
        assertNull(account?.description)
    }

    @Test
    fun accountJsonKeepsTheProfileDescription() {
        val account = parseTwitchAccount(
            """{"data":[{"login":"alice","display_name":"Alice","profile_image_url":"https://example/a.png","description":" Software Developer. "}]}""",
        )

        assertEquals("Software Developer.", account?.description)
    }

    @Test
    fun accountJsonKeepsTheUserId() {
        val account = parseTwitchAccount(
            """{"data":[{"id":"42","login":"alice","display_name":"Alice","profile_image_url":"https://example/a.png"}]}""",
        )

        assertEquals("42", account?.userId)
    }

    @Test
    fun loginRequestsFollowsAndChatSend() {
        assertEquals(
            "user:read:follows user:write:chat user:read:chat user:read:emotes user:read:subscriptions",
            TWITCH_LOGIN_SCOPES,
        )
        assertTrue(deviceCodeForm("client").any { it.first == "scopes" && it.second == twitchLoginScopes() })
        assertTrue(devicePollForm("client", "device").any { it.first == "scopes" && it.second == twitchLoginScopes() })
    }

    @Test
    fun oldFollowPermissionStillLoadsTheFollowList() {
        assertTrue(followListCanLoad(setOf("user:read:follows", "user:write:chat", "user:read:emotes")))
        assertFalse(followListCanLoad(setOf("user:write:chat")))
        assertFalse(followListCanLoad(null))
    }

    @Test
    fun loginClickOpensTheScopePromptWhileAReloginIsPending() {
        assertTrue(loginClickShowsScopePrompt(scopeReloginPending = true))
        assertFalse(loginClickShowsScopePrompt(scopeReloginPending = false))
    }

    @Test
    fun aScopePromptKeepsTheLastChannelFromCoveringTheDialog() {
        assertFalse(resumeSavedChannel("12345678", openedOnHome = true, scopeReloginPending = true))
        assertTrue(resumeSavedChannel("12345678", openedOnHome = true, scopeReloginPending = false))
        assertFalse(resumeSavedChannel(null, openedOnHome = true, scopeReloginPending = false))
        assertTrue(playerYieldsToReloginPrompt(reloginForPermissions = true, reloginForSession = false))
        assertTrue(playerYieldsToReloginPrompt(reloginForPermissions = false, reloginForSession = true))
        assertFalse(playerYieldsToReloginPrompt(reloginForPermissions = false, reloginForSession = false))
    }

    @Test
    fun aDeviceGrantReplacesTheSessionStillOnScreen() {
        val shown = session("old-access", "old-refresh")
        val granted = session("new-access", "new-refresh")
        assertEquals(
            granted,
            latestSessionForRefresh(granted, memory = shown, persisted = shown, deviceGrant = true),
        )
        assertEquals(
            shown,
            latestSessionForRefresh(granted, memory = shown, persisted = shown, deviceGrant = false),
        )
        assertEquals(
            granted,
            latestSessionForRefresh(granted, memory = null, persisted = null, deviceGrant = false),
        )
    }

    @Test
    fun aRefreshRotatedByTheBackgroundReplacesTheStaleMemorySession() {
        val requested = session("old-access", "old-refresh")
        val persisted = session("new-access", "new-refresh")

        assertEquals(
            persisted,
            latestSessionForRefresh(
                requested = requested,
                memory = requested,
                persisted = persisted,
                deviceGrant = false,
            ),
        )
    }

    @Test
    fun aRejectedSessionAsksToLogInAgain() {
        assertTrue(rejectedSessionAsksToLogInAgain(hadAccount = true, alreadyDismissed = false))
        assertFalse(rejectedSessionAsksToLogInAgain(hadAccount = false, alreadyDismissed = false))
        assertFalse(rejectedSessionAsksToLogInAgain(hadAccount = true, alreadyDismissed = true))
    }

    @Test
    fun savedScopesMustCoverTheScopesTheAppRequests() {
        assertFalse(savedSessionNeedsNewLogin(grantedScopes = null, expectedRaw = "user:read:follows"))
        assertFalse(savedSessionNeedsNewLogin(setOf("user:read:follows"), "user:read:follows"))
        assertTrue(savedSessionNeedsNewLogin(emptySet(), "user:read:follows"))
        assertTrue(tokenScopesMatch("user:read:email user:read:follows", setOf("user:read:follows", "user:read:email")))
    }

    @Test
    fun aPremiumLoginKeepsWorkingInTheFreeBuild() {
        val premium = setOf("user:read:follows", "user:read:whispers", "user:manage:whispers")

        assertFalse(savedSessionNeedsNewLogin(premium, "user:read:follows"))
    }

    @Test
    fun aFreeLoginAsksAgainForPremiumScopesAfterAnUpgrade() {
        assertTrue(
            savedSessionNeedsNewLogin(
                setOf("user:read:follows"),
                "user:read:follows user:read:whispers user:manage:whispers",
            ),
        )
    }

    @Test
    fun tokenJsonKeepsTheGrantedScopes() {
        val tokens = parseTwitchTokens(
            """{"access_token":"a","refresh_token":"r","expires_in":3600,"scope":["user:read:follows"]}""",
        )
        assertEquals(setOf("user:read:follows"), tokens?.grantedScopes)
        assertNull(
            parseTwitchTokens(
                """{"access_token":"a","refresh_token":"r","expires_in":3600}""",
            )?.grantedScopes,
        )
    }

    @Test
    fun validateJsonReadsTheScopeList() {
        assertEquals(
            setOf("user:read:follows"),
            parseValidatedScopes("""{"client_id":"abc","scopes":["user:read:follows"],"expires_in":100}"""),
        )
        assertEquals(emptySet<String>(), parseValidatedScopes("""{"scopes":[]}"""))
        assertNull(parseValidatedScopes("""{"login":"alice"}"""))
    }

    @Test
    fun sessionRoundTripKeepsRefreshTokenAndNullAvatar() {
        val session = TwitchSession(
            accessToken = "access",
            refreshToken = "refresh",
            expiresAtMillis = 99L,
            account = TwitchAccount(login = "alice", displayName = "Alice", avatarUrl = null),
        )
        val restored = parseSessionPayload(sessionPayload(session))
        assertEquals(session, restored)
    }

    @Test
    fun sessionRoundTripKeepsBannerFollowersAndLinks() {
        val session = TwitchSession(
            accessToken = "access",
            refreshToken = "refresh",
            expiresAtMillis = 99L,
            account = TwitchAccount(
                login = "alice",
                displayName = "Alice",
                avatarUrl = "https://example/a.png",
                bannerUrl = "https://example/banner.png",
                description = "Software Developer.",
                followerCount = 255,
                links = listOf(
                    TwitchProfileLink(title = "GitHub", url = "https://github.com/alice", name = "github"),
                ),
                userId = "42",
            ),
            grantedScopes = setOf("user:read:follows"),
        )

        assertEquals(session, parseSessionPayload(sessionPayload(session)))
    }

    @Test
    fun tokenJsonRequiresRefreshToken() {
        assertNull(parseTwitchTokens("""{"access_token":"a","expires_in":3600}"""))
        val tokens = parseTwitchTokens(
            """{"access_token":"a","refresh_token":"r","expires_in":3600}""",
        )
        assertEquals("r", tokens?.refreshToken)
        assertEquals(3600L, tokens?.expiresInSeconds)
    }

    @Test
    fun connectionFailureWhileWaitingKeepsPolling() {
        assertTrue(devicePollKeepsWaiting(UnknownHostException()))
        assertTrue(devicePollKeepsWaiting(SocketTimeoutException()))
    }

    @Test
    fun authFailureWhileWaitingStopsTheLogin() {
        assertFalse(devicePollKeepsWaiting(TwitchAuthException(400)))
        assertFalse(devicePollKeepsWaiting(IllegalStateException("boom")))
    }

    @Test
    fun pendingLoginRestoresTheSameCodeBeforeExpiry() {
        val login = TwitchDeviceLogin(
            deviceCode = "device",
            userCode = "ABCDEFGH",
            verificationUri = "https://www.twitch.tv/activate?public=true",
            expiresInSeconds = 30L,
            intervalSeconds = 5L,
        )
        val restored = restorePendingLogin(pendingLoginPayload(login, nowMillis = 1_000L), nowMillis = 11_000L)
        assertEquals("device", restored?.deviceCode)
        assertEquals("ABCDEFGH", restored?.userCode)
        assertEquals(login.verificationUri, restored?.verificationUri)
        assertEquals(20L, restored?.expiresInSeconds)
        assertEquals(5L, restored?.intervalSeconds)
    }

    @Test
    fun pendingLoginDropsAtExpiryAndRejectsABlankCode() {
        val login = TwitchDeviceLogin(
            deviceCode = "device",
            userCode = "ABCDEFGH",
            verificationUri = "https://www.twitch.tv/activate",
            expiresInSeconds = 30L,
            intervalSeconds = 5L,
        )
        val payload = pendingLoginPayload(login, nowMillis = 1_000L)
        assertNull(restorePendingLogin(payload, nowMillis = 31_000L))
        assertNull(restorePendingLogin("""{"deviceCode":"","userCode":"ABCDEFGH","verificationUri":"https://www.twitch.tv/activate","expiresAtMillis":99999}""", 0L))
    }

    @Test
    fun loginSheetCoversMostOfTheScreen() {
        assertEquals(850, loginSheetHeightPx(1000))
        assertEquals(0, loginSheetHeightPx(0))
    }

    @Test
    fun linkWarningFollowsTheOpenSupportedLinksSetting() {
        assertTrue(loginNeedsLinkWarning(linkHandlingAllowed = true, handlerPackage = "com.android.chrome"))
        assertFalse(loginNeedsLinkWarning(linkHandlingAllowed = false, handlerPackage = "tv.twitch.android.app"))
        assertTrue(loginNeedsLinkWarning(linkHandlingAllowed = null, handlerPackage = "tv.twitch.android.app"))
        assertFalse(loginNeedsLinkWarning(linkHandlingAllowed = null, handlerPackage = "com.android.chrome"))
    }

    @Test
    fun linkSettingsOpenTheDefaultLinksPageOnCurrentAndroid() {
        assertEquals(
            android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
            twitchLinkSettingsAction(31),
        )
        assertEquals(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            twitchLinkSettingsAction(28),
        )
    }

    private fun session(accessToken: String, refreshToken: String): TwitchSession {
        return TwitchSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtMillis = 1L,
            account = TwitchAccount(login = "ada", displayName = "Ada", avatarUrl = null),
        )
    }
}
