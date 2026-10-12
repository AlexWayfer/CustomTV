package name.alexwayfer.customtv.data

import android.content.Context
import name.alexwayfer.customtv.auth.TwitchSession
import name.alexwayfer.customtv.auth.parseSessionPayload
import name.alexwayfer.customtv.auth.sessionPayload

internal class TwitchSessionStore(context: Context) {
    private val file = EncryptedTextFile(context, "twitch_session", keyAlias = "customtv_twitch_session")

    fun read(): TwitchSession? = synchronized(STORE_LOCK) {
        try {
            file.read()?.let(::parseSessionPayload)
        } catch (_: Exception) {
            null
        }
    }

    fun write(session: TwitchSession) = synchronized(STORE_LOCK) {
        file.write(sessionPayload(session))
    }

    fun clear() = synchronized(STORE_LOCK) {
        file.delete()
    }

    private companion object {
        val STORE_LOCK = Any()
    }
}
