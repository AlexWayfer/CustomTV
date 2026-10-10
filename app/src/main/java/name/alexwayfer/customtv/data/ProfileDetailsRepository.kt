package name.alexwayfer.customtv.data

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * The profiles chatter cards and profile screens show, for the whole app. A profile opened again shows what was
 * kept at once while one request loads it fresh. The [PROFILES_KEPT] most recently opened stay in the cache
 * directory between launches, keyed by user id; they are public data, so logging out keeps them.
 */
internal object ProfileDetailsRepository {
    private const val TAG = "ProfileDetails"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val http = sharedHttpClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val writeLock = Mutex()
    private var file: AtomicFile? = null

    @Volatile
    private var entries: List<ProfileDetails> = emptyList()

    fun start(context: Context) {
        val stored = AtomicFile(File(context.cacheDir, "profile_details.json")).also { file = it }
        scope.launch {
            val read = runCatching { stored.readFully().toString(Charsets.UTF_8) }.getOrNull() ?: return@launch
            val kept = parseStoredProfiles(read)
            synchronized(this@ProfileDetailsRepository) {
                // A profile loaded while the file was read is newer than its stored copy.
                entries = kept.fold(entries) { list, details ->
                    if (list.any { it.userId == details.userId }) list else (list + details).take(PROFILES_KEPT)
                }
            }
        }
    }

    fun cached(login: String): ProfileDetails? = profileByLogin(entries, login)

    /** Loads [login]'s profile; on success it is kept as the most recently opened. Null when the load fails. */
    suspend fun load(login: String): ProfileDetails? {
        if (login.isBlank()) return null
        val details = fetch(login) ?: return null
        synchronized(this) { entries = profilesAfterOpen(entries, details) }
        save()
        return details
    }

    private suspend fun fetch(login: String): ProfileDetails? = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject()
                .put("query", PROFILE_DETAILS_QUERY)
                .put("variables", JSONObject().put("login", login))
            val body = http.newCall(twitchGqlRequest(payload.toString()).build()).execute().use { response ->
                val text = response.body.string()
                Diagnostics.reportGql(TAG, "profile details", response.code, text)
                if (!response.isSuccessful) {
                    AppLog.w(TAG, "profile details failed HTTP ${response.code}")
                    return@withContext null
                }
                text
            }
            parseProfileDetails(body, login).also { details ->
                // A user without links answers an empty list; null means Twitch left them out.
                when {
                    details == null -> AppLog.w(TAG, "profile details unreadable: ${gqlFailureCause(body)}")
                    details.links == null -> AppLog.w(TAG, "profile links unavailable: ${gqlFailureCause(body)}")
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            AppLog.w(TAG, "profile details failed: ${error.javaClass.simpleName}")
            null
        }
    }

    /** Writes the list as it is when the write starts, so a slower write never puts back an older list. */
    private suspend fun save() {
        val target = file ?: return
        withContext(Dispatchers.IO) {
            writeLock.withLock {
                val output = try {
                    target.startWrite()
                } catch (error: Exception) {
                    AppLog.w(TAG, "profile cache write failed: ${error.javaClass.simpleName}")
                    return@withLock
                }
                try {
                    output.write(profilesJson(entries).toByteArray(Charsets.UTF_8))
                    target.finishWrite(output)
                } catch (error: Exception) {
                    target.failWrite(output)
                    AppLog.w(TAG, "profile cache write failed: ${error.javaClass.simpleName}")
                }
            }
        }
    }
}
