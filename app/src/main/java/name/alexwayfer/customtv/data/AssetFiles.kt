package name.alexwayfer.customtv.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.diagnostics.AppLog
import java.io.File
import java.io.IOException
import kotlin.time.Duration.Companion.days

/**
 * The last answers of emote and badge APIs, kept in the app cache so a channel shows its emotes and badges
 * at once on the next start while the network answer is on its way. Each file is the raw answer, read back
 * through the same parser. A channel's folder goes away a week after the channel was last opened.
 */
internal object AssetFiles {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var root: File? = null

    fun init(context: Context) {
        if (root != null) return
        val dir = context.applicationContext.cacheDir.resolve("emote_cache")
        root = dir
        scope.launch { pruneChannels(dir.resolve(CHANNEL_DIR)) }
    }

    fun global(name: String): String = "global/$name.json"

    fun channel(twitchUserId: String, name: String): String = "$CHANNEL_DIR/$twitchUserId/$name.json"

    fun user(twitchUserId: String): String = "user/$twitchUserId.json"

    /** Reads a saved answer. Reading a channel's file counts as opening that channel. */
    fun read(path: String): String? {
        val file = root?.resolve(path) ?: return null
        if (path.startsWith("$CHANNEL_DIR/")) file.parentFile?.setLastModified(System.currentTimeMillis())
        return try {
            file.takeIf { it.isFile }?.readText()
        } catch (error: IOException) {
            AppLog.w(TAG, "read failed ${error.javaClass.simpleName}")
            null
        }
    }

    fun write(path: String, body: String) {
        val file = root?.resolve(path) ?: return
        try {
            val dir = file.parentFile ?: return
            dir.mkdirs()
            // A whole file or none: a start never reads half an answer.
            val temp = File.createTempFile(file.name, ".tmp", dir)
            temp.writeText(body)
            if (!temp.renameTo(file)) {
                temp.delete()
                AppLog.w(TAG, "write failed rename")
            }
        } catch (error: IOException) {
            AppLog.w(TAG, "write failed ${error.javaClass.simpleName}")
        }
    }

    fun delete(path: String) {
        root?.resolve(path)?.delete()
    }

    private fun pruneChannels(dir: File) {
        val folders = dir.listFiles { file -> file.isDirectory }.orEmpty()
        val stale = staleChannelFolders(
            folders.associate { it.name to it.lastModified() },
            System.currentTimeMillis(),
        )
        stale.forEach { name -> dir.resolve(name).deleteRecursively() }
        if (stale.isNotEmpty()) AppLog.i(TAG, "pruned ${stale.size} channels")
    }

    private const val TAG = "AssetFiles"
    private const val CHANNEL_DIR = "channel"
}

internal val CHANNEL_ASSETS_KEPT = 7.days

/** The channel folders not opened for [CHANNEL_ASSETS_KEPT], by their last opened time in milliseconds. */
internal fun staleChannelFolders(lastOpened: Map<String, Long>, now: Long): List<String> =
    lastOpened.filterValues { now - it > CHANNEL_ASSETS_KEPT.inWholeMilliseconds }.keys.sorted()
