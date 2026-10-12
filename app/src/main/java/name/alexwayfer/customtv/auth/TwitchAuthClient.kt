package name.alexwayfer.customtv.auth

import name.alexwayfer.customtv.data.HTTP_CLIENT_ERROR
import name.alexwayfer.customtv.data.HTTP_SUCCESS
import name.alexwayfer.customtv.data.HTTP_UNAUTHORIZED
import name.alexwayfer.customtv.data.helixRequest
import name.alexwayfer.customtv.data.sharedHttpClient
import android.net.Network
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.diagnostics.AppLog
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

/** The log tag of the Twitch login and the session it keeps. */
internal const val TWITCH_AUTH_TAG = "TwitchAuth"

internal class TwitchAuthException(val httpCode: Int) : IOException("Twitch auth HTTP $httpCode")

internal sealed interface ValidatedScopes {
    data class Known(val scopes: Set<String>) : ValidatedScopes
    data object Rejected : ValidatedScopes
    data object Unavailable : ValidatedScopes
}

internal class TwitchAuthClient(
    private val clientId: String,
    private var http: OkHttpClient = defaultClient(),
) {
    fun useNetwork(network: Network?) {
        http = if (network == null) defaultClient() else networkClient(network)
    }
    suspend fun requestDeviceLogin(): TwitchDeviceLogin = withContext(Dispatchers.IO) {
        val body = formBody(deviceCodeForm(clientId))
        val (code, text) = execute(Request.Builder().url(DEVICE_URL).post(body).build())
        if (code !in HTTP_SUCCESS) {
            AppLog.w(TWITCH_AUTH_TAG, "device login failed HTTP $code: ${twitchAuthMessage(text)}")
            throw TwitchAuthException(code)
        }
        parseTwitchDeviceLogin(text) ?: throw TwitchAuthException(code)
    }

    suspend fun pollDeviceLogin(deviceCode: String): DeviceGrant = withContext(Dispatchers.IO) {
        val body = formBody(devicePollForm(clientId, deviceCode))
        val (code, text) = execute(Request.Builder().url(TOKEN_URL).post(body).build())
        val grant = parseDeviceGrant(code, text)
        if (grant == DeviceGrant.Failed) {
            AppLog.w(TWITCH_AUTH_TAG, "device login failed HTTP $code: ${twitchAuthMessage(text)}")
        }
        grant
    }

    suspend fun refresh(refreshToken: String): TwitchTokens = token(
        FormBody.Builder()
            .add("client_id", clientId)
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .build(),
    )

    suspend fun account(accessToken: String): TwitchAccount = withContext(Dispatchers.IO) {
        val request = helixRequest("https://api.twitch.tv/helix/users", accessToken, clientId)
            .build()
        val (code, body) = execute(request)
        if (code == HTTP_UNAUTHORIZED) throw TwitchAuthException(code)
        if (code !in HTTP_SUCCESS) {
            AppLog.w(TWITCH_AUTH_TAG, "profile failed HTTP $code")
            throw TwitchAuthException(code)
        }
        parseTwitchAccount(body) ?: throw TwitchAuthException(code)
    }

    private suspend fun token(body: FormBody): TwitchTokens = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(TOKEN_URL)
            .post(body)
            .build()
        val (code, text) = execute(request)
        if (code !in HTTP_SUCCESS) {
            AppLog.w(TWITCH_AUTH_TAG, "token failed HTTP $code: ${twitchAuthMessage(text)}")
            throw TwitchAuthException(code)
        }
        parseTwitchTokens(text) ?: throw TwitchAuthException(code)
    }

    suspend fun validatedScopes(accessToken: String): ValidatedScopes = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(VALIDATE_URL)
            .header("Authorization", "OAuth $accessToken")
            .build()
        val (code, body) = try {
            execute(request)
        } catch (error: IOException) {
            AppLog.w(TWITCH_AUTH_TAG, "validate failed ${error.javaClass.simpleName}")
            return@withContext ValidatedScopes.Unavailable
        }
        when (code) {
            in HTTP_SUCCESS -> {
                val scopes = parseValidatedScopes(body)
                if (scopes == null) ValidatedScopes.Unavailable else ValidatedScopes.Known(scopes)
            }
            in HTTP_CLIENT_ERROR -> {
                AppLog.w(TWITCH_AUTH_TAG, "validate rejected HTTP $code")
                ValidatedScopes.Rejected
            }
            else -> {
                AppLog.w(TWITCH_AUTH_TAG, "validate failed HTTP $code")
                ValidatedScopes.Unavailable
            }
        }
    }

    private fun execute(request: Request): Pair<Int, String> {
        http.newCall(request).execute().use { response ->
            return response.code to response.body.string()
        }
    }

    private companion object {
        fun defaultClient(): OkHttpClient {
            return sharedHttpClient.newBuilder()
                .connectTimeout(10.seconds)
                .readTimeout(10.seconds)
                .build()
        }

        fun networkClient(network: Network): OkHttpClient {
            return defaultClient().newBuilder()
                .socketFactory(network.socketFactory)
                .dns { hostname -> network.getAllByName(hostname).toList() }
                .build()
        }

        const val DEVICE_URL = "https://id.twitch.tv/oauth2/device"
        const val TOKEN_URL = "https://id.twitch.tv/oauth2/token"
        const val VALIDATE_URL = "https://id.twitch.tv/oauth2/validate"

        fun formBody(fields: List<Pair<String, String>>): FormBody {
            return FormBody.Builder().apply {
                fields.forEach { (name, value) -> add(name, value) }
            }.build()
        }
    }
}
