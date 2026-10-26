package name.alexwayfer.customtv.data

import android.content.Context
import java.net.URLDecoder
import java.time.Instant
import java.util.Base64
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChatterLabelsGistClient.ASSIGNMENTS_FILE
import name.alexwayfer.customtv.data.ChatterLabelsGistClient.LABELS_FILE
import org.json.JSONArray
import org.json.JSONObject

internal const val CHATTER_LABELS_TAG = "ChatterLabels"

object ChatterLabelsRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private lateinit var store: ChatterLabelsCredentialsStore
    private val _credentials = MutableStateFlow<ChatterLabelsCredentials?>(null)
    internal val credentials = _credentials.asStateFlow()
    private val _credentialsLoaded = MutableStateFlow(false)
    internal val credentialsLoaded = _credentialsLoaded.asStateFlow()
    private val _labelsByUserId = MutableStateFlow<Map<String, List<ChatterLabelIcon>>>(emptyMap())
    val labelsByUserId = _labelsByUserId.asStateFlow()
    private val _labels = MutableStateFlow<List<ChatterLabelIcon>>(emptyList())

    /** Every enabled label, in the extension's order, for the chatter card to assign. */
    internal val labels = _labels.asStateFlow()

    /** The ETag of the gist the shown labels came from, for [refreshLocked] to skip an unchanged one. */
    private var labelsEtag: String? = null

    @Volatile
    private var lastCheck: TimeMark? = null

    fun init(context: Context) {
        store = ChatterLabelsCredentialsStore(context.applicationContext)
        scope.launch {
            mutex.withLock {
                _credentials.value = store.read()
                _credentialsLoaded.value = true
                refreshLocked()
            }
        }
    }

    suspend fun checkAndSave(token: String, gistId: String) = mutex.withLock {
        val candidate = ChatterLabelsCredentials(token.trim(), gistId.trim().lowercase(Locale.ROOT))
        if (candidate.token.isEmpty()) throw ChatterLabelsException(R.string.chatter_labels_error_enter_token)
        if (!GIST_ID.matches(candidate.gistId)) throw ChatterLabelsException(R.string.chatter_labels_error_gist_id)
        val fresh = withContext(Dispatchers.IO) { checkNotNull(ChatterLabelsGistClient.fetchLabels(candidate)) }
        withContext(Dispatchers.IO) { store.write(candidate) }
        _credentials.value = candidate
        labelsEtag = fresh.etag
        show(fresh.data)
    }

    suspend fun disconnect() = mutex.withLock {
        withContext(Dispatchers.IO) { store.clear() }
        _credentials.value = null
        labelsEtag = null
        show(ChatterLabelsData(emptyList(), emptyMap()))
    }

    /**
     * Gives the chatter exactly [checkedLabelIds] of the [shownLabelIds] in the Gist. It reads the Gist again first,
     * so an assignment the extension made in the meantime stays. Returns the assignments as they are now.
     */
    internal suspend fun saveAssignments(
        userId: String,
        login: String,
        displayName: String,
        shownLabelIds: Set<String>,
        checkedLabelIds: Set<String>,
    ): JSONArray = mutex.withLock {
        val saved = _credentials.value ?: throw ChatterLabelsException(R.string.chatter_labels_error_not_connected)
        val written = withContext(Dispatchers.IO) {
            val files = ChatterLabelsGistClient.fetchFiles(saved)
            val labels = JSONArray(ChatterLabelsGistClient.readFile(files, LABELS_FILE) ?: "[]")
            val assignments = updateChatterAssignments(
                assignments = JSONArray(ChatterLabelsGistClient.readFile(files, ASSIGNMENTS_FILE) ?: "[]"),
                chatter = LabeledChatter(userId, login, displayName),
                shownLabelIds = shownLabelIds,
                checkedLabelIds = checkedLabelIds,
                now = Instant.now(),
            )
            ChatterLabelsGistClient.writeFiles(saved, mapOf(ASSIGNMENTS_FILE to formatChatterLabelsJson(assignments)))
            labels to assignments
        }
        show(parseChatterLabels(written.first, written.second))
        written.second
    }

    /**
     * Gives every assignment of a chatter in [users] the name and login the chatter has now, as the extension does when
     * their card opens. It reads the Gist again first, so an assignment made in the meantime stays, and writes only
     * when an entry changed. Returns the assignments as they are now, or null when none needed a change.
     */
    internal suspend fun syncAssignmentUsers(users: Map<String, LabeledChatter>): JSONArray? = mutex.withLock {
        val saved = _credentials.value ?: throw ChatterLabelsException(R.string.chatter_labels_error_not_connected)
        val written = withContext(Dispatchers.IO) {
            val files = ChatterLabelsGistClient.fetchFiles(saved)
            val labels = JSONArray(ChatterLabelsGistClient.readFile(files, LABELS_FILE) ?: "[]")
            val assignments = JSONArray(ChatterLabelsGistClient.readFile(files, ASSIGNMENTS_FILE) ?: "[]")
            val synced = syncChatterAssignmentUsers(assignments, users) ?: return@withContext null
            ChatterLabelsGistClient.writeFiles(saved, mapOf(ASSIGNMENTS_FILE to formatChatterLabelsJson(synced)))
            labels to synced
        } ?: return@withLock null
        show(parseChatterLabels(written.first, written.second))
        written.second
    }

    /** Reads both Gist files for the settings page to edit; chat shows what was read too. */
    internal suspend fun loadForEditing(): ChatterLabelsGist = mutex.withLock {
        val saved = _credentials.value ?: throw ChatterLabelsException(R.string.chatter_labels_error_not_connected)
        val gist = withContext(Dispatchers.IO) {
            val files = ChatterLabelsGistClient.fetchFiles(saved)
            ChatterLabelsGist(
                labels = JSONArray(ChatterLabelsGistClient.readFile(files, LABELS_FILE) ?: "[]"),
                assignments = JSONArray(ChatterLabelsGistClient.readFile(files, ASSIGNMENTS_FILE) ?: "[]"),
            )
        }
        show(parseChatterLabels(gist.labels, gist.assignments))
        gist
    }

    /**
     * Writes [edited] as the labels of the [loaded] Gist, with the assignments of deleted labels removed, in one
     * request. Returns the files as they are now.
     */
    internal suspend fun saveLabels(loaded: ChatterLabelsGist, edited: List<EditableChatterLabel>): ChatterLabelsGist =
        mutex.withLock {
            val saved = _credentials.value ?: throw ChatterLabelsException(R.string.chatter_labels_error_not_connected)
            val update = chatterLabelsUpdate(loaded, edited)
            val files = buildMap {
                put(LABELS_FILE, formatChatterLabelsJson(update.labels))
                update.assignments?.let { put(ASSIGNMENTS_FILE, formatChatterLabelsJson(it)) }
            }
            withContext(Dispatchers.IO) { ChatterLabelsGistClient.writeFiles(saved, files) }
            val result = ChatterLabelsGist(update.labels, update.assignments ?: loaded.assignments)
            show(parseChatterLabels(result.labels, result.assignments))
            result
        }

    fun refresh(): Job {
        if (_credentials.value == null) return Job().apply { complete() }
        return scope.launch { mutex.withLock { refreshLocked() } }
    }

    /**
     * Reads the Gist again every [REFRESH_INTERVAL] until cancelled, so labels changed on another device show up in
     * an open chat. A check made meanwhile, such as the chat settings' refresh, moves the next one.
     */
    suspend fun refreshPeriodically(): Nothing {
        while (true) {
            delay(chatterLabelsRefreshDelay(lastCheck?.elapsedNow(), REFRESH_INTERVAL))
            lastCheck = TimeSource.Monotonic.markNow()
            refresh().join()
        }
    }

    private suspend fun refreshLocked() {
        lastCheck = TimeSource.Monotonic.markNow()
        val saved = _credentials.value ?: return
        try {
            val fresh = withContext(Dispatchers.IO) { ChatterLabelsGistClient.fetchLabels(saved, labelsEtag) }
                ?: return
            labelsEtag = fresh.etag
            show(fresh.data)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // Keep the last successfully loaded labels if the network is unavailable.
        }
    }

    private fun show(data: ChatterLabelsData) {
        val shown = reuseChatterLabelImages(data, _labels.value)
        _labels.value = shown.labels
        _labelsByUserId.value = shown.byUserId
    }

    private val GIST_ID = Regex("[0-9a-f]{32}")
    private val REFRESH_INTERVAL = 1.minutes
}

/**
 * How long to wait before the next periodic check, [sinceLastCheck] after the last one. A chat that comes back from
 * the background after a longer pause checks at once.
 */
internal fun chatterLabelsRefreshDelay(sinceLastCheck: Duration?, interval: Duration): Duration =
    if (sinceLastCheck == null) Duration.ZERO else (interval - sinceLastCheck).coerceAtLeast(Duration.ZERO)

/** Labels as read from the gist, with its [etag] for asking whether they changed since. */
internal class ChatterLabelsVersion(val data: ChatterLabelsData, val etag: String?)

/** Why reading the gist failed with HTTP [code]. */
internal fun chatterLabelsReadFailure(code: Int): ChatterLabelsException = when (code) {
    HTTP_UNAUTHORIZED -> ChatterLabelsException(R.string.chatter_labels_error_token_invalid)
    HTTP_FORBIDDEN -> ChatterLabelsException(R.string.chatter_labels_error_read_denied)
    HTTP_NOT_FOUND -> ChatterLabelsException(R.string.chatter_labels_error_not_found)
    else -> ChatterLabelsException(R.string.chatter_labels_error_http, code)
}

/** Why saving to the gist failed with HTTP [code]. */
internal fun chatterLabelsWriteFailure(code: Int): ChatterLabelsException = when (code) {
    HTTP_UNAUTHORIZED -> ChatterLabelsException(R.string.chatter_labels_error_token_invalid)
    HTTP_FORBIDDEN, HTTP_NOT_FOUND -> ChatterLabelsException(R.string.chatter_labels_error_write_denied)
    else -> ChatterLabelsException(R.string.chatter_labels_error_http, code)
}

internal class ChatterLabelsData(
    val labels: List<ChatterLabelIcon>,
    val byUserId: Map<String, List<ChatterLabelIcon>>,
)

internal fun parseChatterLabels(
    labels: JSONArray,
    assignments: JSONArray,
): ChatterLabelsData {
    val icons = labels.objects()
        .filter { it.optBoolean("enabled", true) }
        .mapNotNull(::chatterLabelIcon)
        .associateBy { it.id }
    val result = mutableMapOf<String, MutableList<ChatterLabelIcon>>()
    for (assignment in assignments.objects()) {
        val userId = assignment.optString("userId")
        val icon = icons[assignment.optString("labelId")]
        if (userId.isNotBlank() && icon != null) {
            val assignedAt = instantMillisOrNull(assignment.optString("assignedAt"))
            result.getOrPut(userId) { mutableListOf() }
                .add(ChatterLabelIcon(icon.id, icon.name, icon.image, assignedAt))
        }
    }
    return ChatterLabelsData(icons.values.toList(), result)
}

/** A label with its name and a picture that decodes; one without them shows nothing. */
private fun chatterLabelIcon(label: JSONObject): ChatterLabelIcon? {
    val id = label.optString("id")
    val name = label.optString("name")
    val image = decodeChatterLabelIcon(label.optString("icon")) ?: return null
    return ChatterLabelIcon(id, name, image).takeIf { id.isNotBlank() && name.isNotBlank() }
}

/**
 * [data] with each image that did not change taken from [previous]: an image is the icon's model, and a new
 * array with the same bytes would load it again, so the icon blinks after every save or refresh.
 */
internal fun reuseChatterLabelImages(data: ChatterLabelsData, previous: List<ChatterLabelIcon>): ChatterLabelsData {
    val previousImages = previous.associate { it.id to it.image }
    val images = data.labels.associate { label ->
        label.id to (previousImages[label.id]?.takeIf { it.contentEquals(label.image) } ?: label.image)
    }
    fun ChatterLabelIcon.withImage(): ChatterLabelIcon {
        val image = images[id] ?: return this
        return if (image === this.image) this else ChatterLabelIcon(id, name, image, assignedAtMillis)
    }
    return ChatterLabelsData(
        labels = data.labels.map { it.withImage() },
        byUserId = data.byUserId.mapValues { (_, labels) -> labels.map { it.withImage() } },
    )
}

internal fun decodeChatterLabelIcon(dataUrl: String): ByteArray? = try {
    when {
        dataUrl.startsWith("data:image/png;base64,") ->
            Base64.getDecoder().decode(dataUrl.substringAfter(','))
        dataUrl.startsWith("data:image/svg+xml,") ->
            URLDecoder.decode(dataUrl.substringAfter(','), "UTF-8").toByteArray(Charsets.UTF_8)
        else -> null
    }?.takeIf { it.isNotEmpty() }
} catch (_: IllegalArgumentException) {
    null
}
