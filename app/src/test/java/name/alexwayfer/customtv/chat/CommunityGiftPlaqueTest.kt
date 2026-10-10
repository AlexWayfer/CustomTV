package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CommunityGiftPlaqueTest {
    @Test
    fun theLatestCommunityGiftIsVisible() {
        val first = message("first", gift = true)
        val latest = message("latest", gift = true)

        assertEquals(
            latest,
            visibleCommunityGiftMessage(
                latestCommunityGiftMessage(listOf(first, message("chat"), latest)),
                hiddenMessageId = null,
                nowMillis = latest.timestampMillis,
            ),
        )
    }

    @Test
    fun hidingTheLatestGiftDoesNotRevealAnOlderGift() {
        val first = message("first", gift = true)
        val latest = message("latest", gift = true)

        assertNull(
            visibleCommunityGiftMessage(
                latestCommunityGiftMessage(listOf(first, latest)),
                hiddenMessageId = latest.id,
                nowMillis = latest.timestampMillis,
            ),
        )
    }

    @Test
    fun aNewGiftAppearsAfterThePreviousOneWasHidden() {
        val hidden = message("hidden", gift = true)
        val latest = message("latest", gift = true)

        assertEquals(
            latest,
            visibleCommunityGiftMessage(latestCommunityGiftMessage(listOf(hidden, latest)), hidden.id, latest.timestampMillis),
        )
    }

    @Test
    fun aCommunityGiftExpiresAfterFifteenSeconds() {
        val gift = message("gift", gift = true)

        assertEquals(COMMUNITY_GIFT_PLAQUE_MILLIS, communityGiftPlaqueRemainingMillis(gift, 1L))
        assertEquals(1L, communityGiftPlaqueRemainingMillis(gift, 15_000L))
        assertEquals(0L, communityGiftPlaqueRemainingMillis(gift, 15_001L))
        assertNull(
            visibleCommunityGiftMessage(
                latestCommunityGiftMessage(listOf(gift)),
                hiddenMessageId = null,
                nowMillis = 15_001L,
            ),
        )
    }

    private fun message(id: String, gift: Boolean = false) = ChatMessage(
        id = id,
        userLogin = "viewer",
        displayName = "Viewer",
        color = Color.White,
        rawText = id,
        parts = emptyList(),
        timestampMillis = 1L,
        communityGift = if (gift) {
            ChatCommunityGift(10, 1, "Viewer", 10, anonymous = false)
        } else {
            null
        },
    )
}
