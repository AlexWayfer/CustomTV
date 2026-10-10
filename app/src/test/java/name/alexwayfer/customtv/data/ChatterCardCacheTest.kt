package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.chat.ChatterFollow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatterCardCacheTest {
    @Test
    fun aLoadedFollowIsKeptPerChannel() {
        val cache = ChatterCardCache()
        assertEquals(ChatterFollow.Following(5), cache.rememberFollow("1", "Viewer", ChatterFollow.Following(5)))
        assertEquals(ChatterFollow.Following(5), cache.cachedFollow("1", "viewer"))
        assertNull(cache.cachedFollow("2", "viewer"))
    }

    @Test
    fun aFreshFollowAnswerReplacesTheCachedOne() {
        val cache = ChatterCardCache()
        cache.rememberFollow("1", "viewer", ChatterFollow.Following(5))
        assertEquals(ChatterFollow.NotFollowing, cache.rememberFollow("1", "viewer", ChatterFollow.NotFollowing))
    }

    @Test
    fun aFailedFollowLoadKeepsTheCachedAnswer() {
        val cache = ChatterCardCache()
        cache.rememberFollow("1", "viewer", ChatterFollow.Following(5))
        assertEquals(ChatterFollow.Following(5), cache.rememberFollow("1", "viewer", null))
        assertNull(cache.rememberFollow("2", "viewer", null))
    }
}
