package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ProfileWithKeptDetailsTest {
    private val fetched = ChannelProfile(
        login = "alice",
        displayName = "Alice",
        avatarUrl = "https://example/new.png",
        id = "42",
        viewerCount = 10,
    )

    @Test
    fun aChannelSeenForTheFirstTimeStaysAsFetched() {
        assertSame(fetched, profileWithKeptDetails(fetched, previous = null))
    }

    @Test
    fun detailsOnlyTheFullQueryLoadsComeFromThePreviousProfile() {
        val previous = fetched.copy(
            highlightColorHex = "#112233",
            primaryColorHex = "#445566",
            highlightRewardCost = 300,
            channelPointsIconUrl = "https://example/points.png",
            tags = listOf("English"),
            chatRules = listOf("Be nice"),
        )

        val kept = profileWithKeptDetails(fetched, previous)

        assertEquals("#112233", kept.highlightColorHex)
        assertEquals("#445566", kept.primaryColorHex)
        assertEquals(300, kept.highlightRewardCost)
        assertEquals("https://example/points.png", kept.channelPointsIconUrl)
        assertEquals(listOf("English"), kept.tags)
        assertEquals(listOf("Be nice"), kept.chatRules)
        assertEquals(10, kept.viewerCount)
    }

    @Test
    fun freshColorsAndTagsWinOverThePreviousOnes() {
        val previous = fetched.copy(primaryColorHex = "#445566", tags = listOf("Old"))

        val kept = profileWithKeptDetails(fetched.copy(primaryColorHex = "#778899", tags = listOf("New")), previous)

        assertEquals("#778899", kept.primaryColorHex)
        assertEquals(listOf("New"), kept.tags)
    }
}
