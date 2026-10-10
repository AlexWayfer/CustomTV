package name.alexwayfer.customtv.ui.components

import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal object SharedEmoteStore {
    private const val MAX_IDLE = 64

    private val lock = Any()
    private val holders = mutableMapOf<String, Holder>()
    private val idle = LinkedHashMap<String, Holder>(MAX_IDLE + 1, 0.75f, true)
    private val aspectRatios = mutableStateMapOf<String, Float>()
    private val handler = Handler(Looper.getMainLooper())

    fun aspectRatio(url: String): Float? = aspectRatios[url]

    fun acquire(url: String): Holder = synchronized(lock) {
        val holder = holders.getOrPut(url) { Holder(url) }
        val wasIdle = holder.refCount == 0
        holder.refCount++
        idle.remove(url)
        if (wasIdle) holder.unpark()
        holder
    }

    fun release(url: String) {
        synchronized(lock) {
            val holder = holders[url] ?: return
            holder.refCount--
            if (holder.refCount > 0) return
            holder.park()
            rememberIdle(url, holder)
        }
    }

    private fun recordAspectRatio(url: String, drawable: Drawable) {
        val ratio = aspectRatioFromIntrinsic(drawable.intrinsicWidth, drawable.intrinsicHeight)
            ?: return
        aspectRatios[url] = ratio
    }

    private fun rememberIdle(url: String, holder: Holder) {
        idle[url] = holder
        while (idle.size > MAX_IDLE) {
            val oldest = idle.entries.iterator().next()
            idle.remove(oldest.key)
            if (oldest.value.refCount == 0) {
                holders.remove(oldest.key)
                oldest.value.dispose()
            }
        }
    }

    private fun dropUnusedEmpty(url: String) {
        synchronized(lock) {
            val holder = holders[url] ?: return
            if (holder.refCount == 0 && holder.drawable == null) {
                idle.remove(url)
                holders.remove(url)
            }
        }
    }

    private fun onDecoded(url: String, holder: Holder) {
        synchronized(lock) {
            if (holder.refCount > 0) {
                holder.unpark()
            } else {
                holder.park()
                rememberIdle(url, holder)
            }
        }
    }

    class Holder(private val url: String) {
        var refCount = 0
        var drawable by mutableStateOf<Drawable?>(null)
            private set
        var tick by mutableIntStateOf(0)
            private set

        private val mutex = Mutex()
        private val callback = object : Drawable.Callback {
            override fun invalidateDrawable(who: Drawable) {
                tick++
            }

            override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
                handler.postAtTime(what, who, `when`)
            }

            override fun unscheduleDrawable(who: Drawable, what: Runnable) {
                handler.removeCallbacks(what, who)
            }
        }

        suspend fun load(context: Context) {
            mutex.withLock {
                drawable?.let { loaded ->
                    recordAspectRatio(url, loaded)
                    return
                }
                val result = context.imageLoader.execute(
                    ImageRequest.Builder(context)
                        .data(url)
                        .size(coil.size.Size.ORIGINAL)
                        .allowHardware(false)
                        .diskCacheKey(url)
                        .memoryCacheKey(url)
                        .build(),
                )
                if (result !is SuccessResult) {
                    dropUnusedEmpty(url)
                    return
                }
                val loaded = result.drawable
                loaded.callback = callback
                drawable = loaded
                recordAspectRatio(url, loaded)
                onDecoded(url, this)
            }
        }

        fun park() {
            val current = drawable ?: return
            if (current is Animatable && current.isRunning) current.stop()
        }

        fun unpark() {
            val current = drawable ?: return
            if (current is Animatable && !current.isRunning) current.start()
        }

        fun dispose() {
            val current = drawable
            drawable = null
            current?.callback = null
            if (current is Animatable) current.stop()
        }
    }
}
