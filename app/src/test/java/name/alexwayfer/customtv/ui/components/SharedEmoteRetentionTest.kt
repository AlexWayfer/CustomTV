package name.alexwayfer.customtv.ui.components

import android.app.Application
import android.graphics.drawable.ColorDrawable
import coil.Coil
import coil.ImageLoader
import coil.decode.DataSource
import coil.intercept.Interceptor
import coil.request.SuccessResult
import coil.request.ErrorResult
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SharedEmoteRetentionTest {
    private val prefix = "https://emotes.invalid/${UUID.randomUUID()}/"
    private val app: Application get() = RuntimeEnvironment.getApplication()
    private lateinit var previousLoader: ImageLoader
    private lateinit var loader: ImageLoader
    private var delayedUrl: String? = null
    private var failingUrl: String? = null
    private val decoding = CompletableDeferred<Unit>()
    private val finishDecode = CompletableDeferred<Unit>()

    @Before
    fun imagesWithoutNetwork() {
        previousLoader = Coil.imageLoader(app)
        loader = ImageLoader.Builder(app).components {
            add(Interceptor { chain ->
                if (chain.request.data == delayedUrl) {
                    decoding.complete(Unit)
                    finishDecode.await()
                }
                if (chain.request.data == failingUrl) {
                    ErrorResult(null, chain.request, IllegalStateException("Simulated image failure"))
                } else {
                    val drawable = object : ColorDrawable() {
                        override fun getIntrinsicWidth(): Int = 4
                        override fun getIntrinsicHeight(): Int = 2
                    }
                    SuccessResult(drawable, chain.request, DataSource.MEMORY)
                }
            })
        }.build()
        Coil.setImageLoader(loader)
    }

    @After
    fun restoreImages() {
        Coil.setImageLoader(previousLoader)
        loader.shutdown()
    }

    @Test
    fun `evicting an idle image also forgets its dimensions`() = runBlocking {
        val url = "${prefix}first"
        val holder = SharedEmoteStore.acquire(url)
        holder.load(app)
        assertEquals(2f, SharedEmoteStore.aspectRatio(url)!!, 0f)
        SharedEmoteStore.release(url)
        fillIdleImages()

        assertNull(holder.drawable)
        assertNull(SharedEmoteStore.aspectRatio(url))
        val memory = SharedEmoteStore.memoryLine()
        assertTrue(memory.contains("emoteRatios=64 "))
        assertFalse(memory.contains(prefix))
    }

    @Test
    fun `an active image keeps its dimensions while idle images are evicted`() = runBlocking {
        val url = "${prefix}active"
        val holder = SharedEmoteStore.acquire(url)
        try {
            holder.load(app)
            fillIdleImages()
            assertEquals(2f, SharedEmoteStore.aspectRatio(url)!!, 0f)
        } finally {
            SharedEmoteStore.release(url)
        }
    }

    @Test
    fun `a late decode cannot restore dimensions after its holder was evicted`() = runBlocking {
        val url = "${prefix}delayed"
        delayedUrl = url
        val old = SharedEmoteStore.acquire(url)
        val load = async(start = CoroutineStart.UNDISPATCHED) { old.load(app) }
        decoding.await()
        SharedEmoteStore.release(url)
        fillIdleImages()
        val replacement = SharedEmoteStore.acquire(url)
        try {
            assertNotSame(old, replacement)
            finishDecode.complete(Unit)
            load.await()
            assertNull(old.drawable)
            assertNull(SharedEmoteStore.aspectRatio(url))
            assertNull(replacement.drawable)
        } finally {
            SharedEmoteStore.release(url)
        }
    }

    @Test
    fun `a late decode leaves the replacement image and its dimensions intact`() = runBlocking {
        val url = "${prefix}replaced"
        delayedUrl = url
        val old = SharedEmoteStore.acquire(url)
        val load = async(start = CoroutineStart.UNDISPATCHED) { old.load(app) }
        decoding.await()
        SharedEmoteStore.release(url)
        fillIdleImages()
        val replacement = SharedEmoteStore.acquire(url)
        try {
            assertNotSame(old, replacement)
            delayedUrl = null
            replacement.load(app)
            finishDecode.complete(Unit)
            load.await()
            assertNull(old.drawable)
            assertEquals(2f, SharedEmoteStore.aspectRatio(url)!!, 0f)
        } finally {
            SharedEmoteStore.release(url)
        }
    }

    @Test
    fun `an old failed load cannot discard a newer idle holder`() = runBlocking {
        val url = "${prefix}failure"
        delayedUrl = url
        failingUrl = url
        val old = SharedEmoteStore.acquire(url)
        val load = async(start = CoroutineStart.UNDISPATCHED) { old.load(app) }
        decoding.await()
        SharedEmoteStore.release(url)
        fillIdleImages()
        val replacement = SharedEmoteStore.acquire(url)
        SharedEmoteStore.release(url)
        finishDecode.complete(Unit)
        load.await()

        val reacquired = SharedEmoteStore.acquire(url)
        try {
            assertSame(replacement, reacquired)
        } finally {
            SharedEmoteStore.release(url)
        }
    }

    private suspend fun fillIdleImages() {
        repeat(65) { index ->
            val url = "$prefix$index"
            val holder = SharedEmoteStore.acquire(url)
            holder.load(app)
            SharedEmoteStore.release(url)
        }
    }
}
