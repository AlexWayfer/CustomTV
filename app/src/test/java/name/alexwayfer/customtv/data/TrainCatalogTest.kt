package name.alexwayfer.customtv.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.days

class TrainCatalogTest {
    private val banner = "features.hype-train.components.banner-v3-container"
    private val hash = "0123456789abcdef1234"
    private val page = """p.u=e=>"assets/"+({1:"$banner",2:"core.locales.ru"}[e]||e)+"-"+""" +
        """{1:"$hash",2:"$hash",69399:"$hash"}[e]+".js";<script src="https://assets.twitch.tv/assets/34609-$hash.js">"""
    private val theme = """return{id:"gta-vi",backgroundColors:["#445ed6","#fc52ae","#ed815f"],contrastOverlay:!1,
        trainName:(0,n.hw)("Heist Train","HypeTrainBannerV3"),
        sharedTrainName:(0,n.hw)("Shared Heist Train","HypeTrainBannerV3")};"""
    private val community = """case"community":return{id:"community",backgroundColors:[u.K["color-blue-9"],"#0e3fa8"],
        contrastOverlay:!1};"""
    private val names = """case i.Z6D.COMMUNITY:return r?(0,n.hw)("Shared Community Train","HypeTrainBanner_AriaLabel"):
        (0,n.hw)("Community Train","HypeTrainBanner_AriaLabel")"""
    private val ru = """{"[HypeTrainBannerV3] Heist Train":"Хайповоз",
        "[HypeTrainBannerV3] Shared Heist Train":"Мегахайповоз",
        "[HypeTrainBanner_AriaLabel] Community Train":"Поезд сообщества",
        "[HypeTrainBanner_AriaLabel] Shared Community Train":"Мегапоезд"}"""

    private fun source(file: String) = when {
        file.startsWith(banner) -> "positionPercent:55};$theme$community"
        file.startsWith("core.locales.ru") -> ru
        file.startsWith("34609-") -> """{"color-blue-9":"#1f69ff"}"""
        file.startsWith("69399-") -> names
        else -> error(file)
    }

    @Test
    fun appOpensUseThePersistedAttemptUntilADayPassesAndRecoverFromAClockReset() {
        val last = 10.days.inWholeMilliseconds
        assertTrue(trainCatalogCheckDue(0, last))
        assertFalse(trainCatalogCheckDue(last, last + 1.days.inWholeMilliseconds - 1))
        assertTrue(trainCatalogCheckDue(last, last + 1.days.inWholeMilliseconds))
        assertTrue(trainCatalogCheckDue(last, last - 1))
    }

    @Test
    fun namesColorsAndStopsComeFromTheSameBuildAndOnlyChangedFilesAreDownloaded() {
        val files = mutableListOf<String>()
        val cache = refreshTrainCatalog(page, JSONObject()) {
            files += it
            source(it)
        }
        assertEquals(4, files.distinct().size)
        val styles = trainAppearances(cache)
        assertEquals("Хайповоз", styles.getValue("gta-vi").russian)
        assertEquals("Shared Community Train", styles.getValue("shared:community").english)
        assertEquals(0xff1f69ffL, styles.getValue("community").stops.first().second)
        assertEquals(0.55f to 0xfffc52aeL, styles.getValue("gta-vi").stops[1])
        val restored = JSONObject(cache.toString())
        refreshTrainCatalog(page, restored) { error("unchanged $it") }
        val next = page.replace("2:\"$hash\"", "2:\"123456789abcdef01234\"")
        val updated = refreshTrainCatalog(next, restored) {
            check(it.startsWith("core.locales.ru"))
            ru.replace("Хайповоз", "Новое название")
        }
        assertEquals("Новое название", trainAppearances(updated).getValue("gta-vi").russian)
    }

    @Test
    fun removedCampaignDisappearsButABrokenReplacementCannotOverwriteTheGoodCache() {
        val cache = refreshTrainCatalog(page, JSONObject(), ::source)
        val previous = cache.toString()
        val next = page.replace("1:\"$hash\"", "1:\"abcdef01234567890123\"")
        assertThrows(IllegalArgumentException::class.java) { refreshTrainCatalog(next, cache) { "broken" } }
        assertEquals(previous, cache.toString())
        val updated = refreshTrainCatalog(next, cache) { "positionPercent:55};$community" }
        assertFalse(trainAppearances(updated).containsKey("gta-vi"))
        assertEquals("Поезд сообщества", trainAppearances(updated).getValue("community").russian)
    }
}
