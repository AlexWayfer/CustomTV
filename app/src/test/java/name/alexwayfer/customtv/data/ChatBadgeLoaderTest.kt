package name.alexwayfer.customtv.data

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatBadgeLoaderTest {
    private var globalAnswer: Any = GLOBAL
    private var channelAnswer: Any = CHANNEL
    private var globalRequests = 0
    private var channelRequests = 0
    private val http = OkHttpClient.Builder().addInterceptor { chain ->
        val payload = Buffer().also { chain.request().body!!.writeTo(it) }.readUtf8()
        val answer = if (payload.contains("broadcastBadges")) {
            channelRequests++
            channelAnswer
        } else {
            globalRequests++
            globalAnswer
        }
        if (answer is IOException) throw answer
        Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(answer as? Int ?: 200)
            .message("")
            .body((answer as? String).orEmpty().toResponseBody())
            .build()
    }.build()
    private val loader = ChatBadgeLoader(http)

    @Test
    fun globalBadgesRecoverWhileTheSameChatRemainsOpen() = runBlocking {
        globalAnswer = IOException("offline")
        val updates = mutableListOf<Set<String>>()

        loader.keepLoaded("alice") { badges ->
            updates += badges.twitch.keys
            globalAnswer = GLOBAL
        }

        assertEquals(listOf(setOf("subscriber/1"), setOf("moderator/1", "subscriber/1")), updates)
        assertEquals(2, globalRequests)
        assertEquals(1, channelRequests)
    }

    @Test
    fun channelBadgesRecoverAfterAServerErrorWhileWatching() = runBlocking {
        channelAnswer = 503
        val updates = mutableListOf<Set<String>>()

        loader.keepLoaded("alice") { badges ->
            updates += badges.twitch.keys
            channelAnswer = CHANNEL
        }

        assertEquals(listOf(setOf("moderator/1"), setOf("moderator/1", "subscriber/1")), updates)
        assertEquals(1, globalRequests)
        assertEquals(2, channelRequests)
    }

    @Test
    fun aTransientGqlErrorIsRetriedEvenWithHttpSuccess() = runBlocking {
        globalAnswer = """{"errors":[{"message":"service error"}],"data":{"badges":null}}"""
        val updates = mutableListOf<Set<String>>()

        loader.keepLoaded("alice") { badges ->
            updates += badges.twitch.keys
            globalAnswer = GLOBAL
        }

        assertEquals(listOf(setOf("subscriber/1"), setOf("moderator/1", "subscriber/1")), updates)
    }

    @Test
    fun aRefusedGlobalRequestIsNotRepeatedWhileTheChannelRecovers() = runBlocking {
        globalAnswer = 403
        channelAnswer = IOException("offline")
        val updates = mutableListOf<Set<String>>()

        loader.keepLoaded("alice") { badges ->
            updates += badges.twitch.keys
            channelAnswer = CHANNEL
        }

        assertEquals(listOf(emptySet(), setOf("subscriber/1")), updates)
        assertEquals(1, globalRequests)
        assertEquals(2, channelRequests)
    }

    @Test
    fun cancellingTheOpenChatStopsPendingRetries() = runBlocking {
        globalAnswer = IOException("offline")
        val updated = CompletableDeferred<Unit>()
        val watching = async { loader.keepLoaded("alice") { updated.complete(Unit) } }
        updated.await()
        watching.cancelAndJoin()

        assertEquals(1, globalRequests)
        assertEquals(1, channelRequests)
    }

    @Test
    fun aFailedGlobalLoadCanRecoverInTheSameChannel() = runBlocking {
        globalAnswer = IOException("offline")
        assertEquals(setOf("subscriber/1"), loader.forChannel("alice").twitch.keys)
        globalAnswer = GLOBAL

        assertEquals(setOf("moderator/1", "subscriber/1"), loader.forChannel("alice").twitch.keys)
        assertEquals(2, globalRequests)
    }

    @Test
    fun aFailedChannelLoadCanRecoverWithoutReloadingGlobalBadges() = runBlocking {
        channelAnswer = IOException("offline")
        assertEquals(setOf("moderator/1"), loader.forChannel("alice").twitch.keys)
        channelAnswer = CHANNEL

        assertEquals(setOf("moderator/1", "subscriber/1"), loader.forChannel("alice").twitch.keys)
        assertEquals(1, globalRequests)
    }

    @Test
    fun cancellingTheFirstChannelDoesNotPoisonGlobalBadges() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val blockingHttp = OkHttpClient.Builder().addInterceptor { chain ->
            val payload = Buffer().also { chain.request().body!!.writeTo(it) }.readUtf8()
            if (!payload.contains("broadcastBadges")) {
                entered.complete(Unit)
                // This synchronous interceptor holds the HTTP thread until the test releases it.
                @Suppress("RunBlocking")
                runBlocking { release.await() }
            }
            chain.proceed(chain.request())
        }.addInterceptor(http.interceptors.single()).build()
        val cancellingLoader = ChatBadgeLoader(blockingHttp)
        val first = async { cancellingLoader.forChannel("alice") }
        entered.await()
        first.cancel()
        release.complete(Unit)
        first.cancelAndJoin()

        assertTrue(cancellingLoader.forChannel("bob").twitch.containsKey("moderator/1"))
    }

    @Test
    fun returningBeforeTheOldRequestFinishesDoesNotCancelTheNewLoad() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val blockingHttp = OkHttpClient.Builder().addInterceptor { chain ->
            val payload = Buffer().also { chain.request().body!!.writeTo(it) }.readUtf8()
            if (!payload.contains("broadcastBadges")) {
                entered.complete(Unit)
                // This synchronous interceptor holds the HTTP thread until the test releases it.
                @Suppress("RunBlocking")
                runBlocking { release.await() }
            }
            chain.proceed(chain.request())
        }.addInterceptor(http.interceptors.single()).build()
        val cancellingLoader = ChatBadgeLoader(blockingHttp)
        val first = async { cancellingLoader.forChannel("alice") }
        entered.await()
        first.cancel()
        val returning = async { cancellingLoader.forChannel("alice") }
        yield()
        release.complete(Unit)
        first.cancelAndJoin()

        assertEquals(setOf("moderator/1", "subscriber/1"), returning.await().twitch.keys)
    }

    private companion object {
        const val GLOBAL = """{"data":{"badges":[
            {"setID":"moderator","version":"1","title":"Moderator","imageURL":"https://badge/mod"}
        ]}}"""
        const val CHANNEL = """{"data":{"user":{"broadcastBadges":[
            {"setID":"subscriber","version":"1","title":"Subscriber","imageURL":"https://badge/sub"}
        ]}}}"""
    }
}
