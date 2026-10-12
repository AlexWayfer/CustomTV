package name.alexwayfer.customtv.data

import kotlinx.coroutines.runBlocking
import name.alexwayfer.customtv.chat.SevenTvEmote
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmoteSourceCacheTest {
    /** The requests by address, and the answer each address gets: a body, or an HTTP code without one. */
    private val requests = mutableListOf<String>()
    private val answers = mutableMapOf<String, Any>()

    private val http = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val url = chain.request().url.toString()
            synchronized(requests) { requests += url }
            val answer = answers[url] ?: 404
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(answer as? Int ?: 200)
                .message("")
                .body((answer as? String).orEmpty().toResponseBody())
                .build()
        }
        .build()

    private val cache = EmoteSourceCache(
        file = "test",
        globalUrl = GLOBAL,
        channelUrl = { id -> "$CHANNEL$id" },
        parseGlobal = ::emotes,
        parseChannel = { _, body -> EmoteChannel(emotes(body)) },
        http = http,
    )

    /** Each comma-separated name in [body] as an emote. */
    private fun emotes(body: String): Map<String, SevenTvEmote> =
        body.split(',').filter { it.isNotBlank() }.associateWith { SevenTvEmote(url = "https://emote/$it") }

    @Test
    fun oneLoadAsksForTheGlobalAndTheChannelSetsOnce() = runBlocking {
        answers[GLOBAL] = "Kappa"
        answers["${CHANNEL}1"] = "Pepe"

        assertEquals(setOf("Kappa", "Pepe"), cache.forTwitchUser("1").keys)
        assertEquals(setOf("Kappa", "Pepe"), cache.forTwitchUser("1").keys)

        assertEquals(2, requests.size)
        assertEquals(setOf(GLOBAL, "${CHANNEL}1"), requests.toSet())
        assertTrue(cache.isLoaded("1"))
    }

    @Test
    fun aRefreshAsksAgain() = runBlocking {
        answers[GLOBAL] = "Kappa"
        answers["${CHANNEL}1"] = "Pepe"
        cache.forTwitchUser("1")
        answers["${CHANNEL}1"] = "Pepe,Sadge"

        assertEquals(setOf("Kappa", "Pepe", "Sadge"), cache.forTwitchUser("1", forceRefresh = true).keys)
        assertEquals(4, requests.size)
    }

    @Test
    fun aRefusedRefreshKeepsTheSetsLoadedBefore() = runBlocking {
        answers[GLOBAL] = "Kappa"
        answers["${CHANNEL}1"] = "Pepe"
        cache.forTwitchUser("1")
        answers[GLOBAL] = 400
        answers["${CHANNEL}1"] = 400

        assertEquals(setOf("Kappa", "Pepe"), cache.forTwitchUser("1", forceRefresh = true).keys)
    }

    @Test
    fun aChannelWithoutASetThereHasNoEmotesAndCountsAsLoaded() = runBlocking {
        answers[GLOBAL] = "Kappa"

        assertEquals(setOf("Kappa"), cache.forTwitchUser("1").keys)
        assertTrue(cache.isLoaded("1"))
        cache.forTwitchUser("1")
        assertEquals(2, requests.size)
    }

    @Test
    fun anotherChannelLoadsItsOwnSetAndTheGlobalOneOnlyOnce() = runBlocking {
        answers[GLOBAL] = "Kappa"
        answers["${CHANNEL}1"] = "Pepe"
        answers["${CHANNEL}2"] = "Sadge"
        cache.forTwitchUser("1")

        assertFalse(cache.isLoaded("2"))
        assertEquals(setOf("Kappa", "Sadge"), cache.forTwitchUser("2").keys)
        assertEquals(3, requests.size)
        assertEquals(setOf(GLOBAL, "${CHANNEL}1", "${CHANNEL}2"), requests.toSet())
    }

    @Test
    fun aLiveChangeReachesOnlyALoadedChannel() = runBlocking {
        answers[GLOBAL] = "Kappa"
        answers["${CHANNEL}1"] = "Pepe"
        cache.forTwitchUser("1")

        assertNull(cache.mutateChannel("2") { it + ("Sadge" to SevenTvEmote(url = "https://emote/Sadge")) })
        val changed = cache.mutateChannel("1") { it + ("Sadge" to SevenTvEmote(url = "https://emote/Sadge")) }
        assertEquals(setOf("Kappa", "Pepe", "Sadge"), changed?.keys)
    }

    @Test
    fun aLiveEmoteIsAddedRenamedAndRemovedByName() = runBlocking {
        answers[GLOBAL] = "Kappa"
        answers["${CHANNEL}1"] = "Pepe"
        cache.forTwitchUser("1")
        val sadge = SevenTvEmote(url = "https://emote/Sadge")

        assertEquals(setOf("Kappa", "Pepe", "Sadge"), cache.addChannelEmote("1", "Sadge", sadge)?.keys)
        val renamed = cache.renameChannelEmote("1", "Sadge", "Sadder", sadge)
        assertEquals(setOf("Kappa", "Pepe", "Sadder"), renamed?.keys)
        assertEquals(sadge, renamed?.get("Sadder"))
        assertEquals(setOf("Kappa", "Sadder"), cache.removeChannelEmote("1", "Pepe")?.keys)
        assertNull(cache.removeChannelEmote("2", "Pepe"))
    }

    private companion object {
        const val GLOBAL = "https://emotes.test/global"
        const val CHANNEL = "https://emotes.test/channel/"
    }
}
