package name.alexwayfer.customtv.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import name.alexwayfer.customtv.diagnostics.DiagnosticEvent
import name.alexwayfer.customtv.diagnostics.DiagnosticKind
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import kotlin.time.Duration.Companion.days

private const val TAG = "TrainCatalog"
private const val PAGE = "https://www.twitch.tv/"
private const val CDN = "https://assets.twitch.tv/assets/"
private const val SNAPSHOT = "snapshot"
private const val ATTEMPT = "checked_at"

internal fun trainCatalogCheckDue(lastAttempt: Long, now: Long): Boolean =
    lastAttempt == 0L || now - lastAttempt !in 0 until 1.days.inWholeMilliseconds

internal object TwitchTrainCatalog {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()
    private val http = sharedHttpClient.newBuilder().callTimeout(30, TimeUnit.SECONDS).build()
    private val current = MutableStateFlow<Map<String, TrainAppearance>?>(null)
    val appearances = current.asStateFlow()
    private var cache: JSONObject? = null

    fun onAppOpen(context: Context) {
        val app = context.applicationContext
        scope.launch {
            lock.withLock {
                val prefs = app.getSharedPreferences("twitch_train_catalog", Context.MODE_PRIVATE)
                val old = cache ?: restore(prefs).also { cache = it }
                val now = System.currentTimeMillis()
                val last = prefs.getLong(ATTEMPT, 0L)
                if (trainCatalogCheckDue(last, now)) {
                    prefs.edit { putLong(ATTEMPT, now) }
                    refresh(prefs, old)
                }
            }
        }
    }

    private fun restore(prefs: SharedPreferences): JSONObject {
        val saved = prefs.getString(SNAPSHOT, null) ?: return JSONObject()
        return try {
            JSONObject(saved).also { current.value = trainAppearances(it) }
        } catch (error: JSONException) {
            AppLog.w(TAG, "invalid cached catalog: ${error.javaClass.simpleName}")
            JSONObject()
        } catch (error: IllegalArgumentException) {
            AppLog.w(TAG, "invalid cached catalog: ${error.message}")
            JSONObject()
        }
    }

    private fun refresh(prefs: SharedPreferences, old: JSONObject) {
        var reading = "homepage"
        try {
            val page = read(PAGE, reading)
            val next = refreshTrainCatalog(page, old) { file ->
                reading = file
                read(CDN + file, file)
            }
            val styles = trainAppearances(next)
            if (next.toString() != old.toString()) {
                if (!prefs.edit().putString(SNAPSHOT, next.toString()).commit()) {
                    AppLog.w(TAG, "could not save catalog snapshot")
                    return
                }
                cache = next
                current.value = styles
            }
            AppLog.i(TAG, "checked ${styles.size} titles: ${styles.keys.joinToString()}")
        } catch (error: IOException) {
            AppLog.w(TAG, "$reading download failed: ${error.javaClass.simpleName}; keeping previous catalog")
        } catch (error: IllegalArgumentException) {
            parseFailed(reading, error.message.orEmpty(), old)
        } catch (error: JSONException) {
            parseFailed(reading, "unexpected JSON structure (${error.javaClass.simpleName})", old)
        }
    }

    private fun parseFailed(reading: String, reason: String, old: JSONObject) {
        val previous = TrainAsset.entries.mapNotNull { old.optJSONObject(it.name)?.trimmedText("file") }
        val message = "Last read: $reading. $reason. Previous assets: ${previous.joinToString().ifEmpty { "none" }}."
        AppLog.w(TAG, message)
        Diagnostics.recordAndNotice(
            DiagnosticEvent(
                DiagnosticKind.ParseError, TAG, "Twitch train catalog",
                message = message, firstAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    private fun read(url: String, asset: String): String {
        val request = Request.Builder().url(url).build()
        return http.newCall(request).execute().use { response ->
            Diagnostics.reportHttp(TAG, asset.substringBeforeLast('-'), response.code, "", emptySet())
            if (!response.isSuccessful) {
                AppLog.w(TAG, "$asset HTTP ${response.code}")
                throw IOException("HTTP ${response.code}")
            }
            response.body.string().also { AppLog.i(TAG, "$asset HTTP ${response.code}, ${it.length} characters") }
        }
    }
}
