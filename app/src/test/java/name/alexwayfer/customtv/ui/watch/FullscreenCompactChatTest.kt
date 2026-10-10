package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.chat.CHAT_WELCOME_NOTICE_ID
import name.alexwayfer.customtv.chat.ChatNotice
import name.alexwayfer.customtv.chat.chatNoticeMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FullscreenCompactChatTest {
    @Test
    fun anOverlaidChatStartsCompact() {
        assertEquals(
            FullscreenOverlayChatHeight.Compact,
            fullscreenOverlayChatHeight(overPlayer = true, expanded = false, composerOpen = false),
        )
    }

    @Test
    fun anExpandedOverlaidChatTakesTheFullHeight() {
        assertEquals(
            FullscreenOverlayChatHeight.Full,
            fullscreenOverlayChatHeight(overPlayer = true, expanded = true, composerOpen = false),
        )
    }

    @Test
    fun typingTakesTheFullColumnEvenFromTheCompactChat() {
        assertEquals(
            FullscreenOverlayChatHeight.None,
            fullscreenOverlayChatHeight(overPlayer = true, expanded = false, composerOpen = true),
        )
    }

    @Test
    fun aColumnChatIsNotOverlaid() {
        assertEquals(
            FullscreenOverlayChatHeight.None,
            fullscreenOverlayChatHeight(overPlayer = false, expanded = true, composerOpen = false),
        )
    }

    @Test
    fun aShownMessageKeepsItsFirstTimeAndANewOneTakesNow() {
        val shownAt = compactChatShownAt(
            mapOf("a" to 100L, "b" to 200L),
            listOf("b", "c"),
            nowMillis = 300L,
            lifetimeMillis = 7_000L,
        )
        assertEquals(mapOf("b" to 200L, "c" to 300L), shownAt)
    }

    @Test
    fun messagesThereBeforeTheFirstLookCountAsGone() {
        val shownAt = compactChatShownAt(null, listOf("a", "b"), nowMillis = 20_000L, lifetimeMillis = 7_000L)
        assertEquals(mapOf("a" to 13_000L, "b" to 13_000L), shownAt)
        assertTrue(compactChatVisibleIds(listOf("a", "b"), shownAt, 20_000L, 7_000L).isEmpty())
    }

    @Test
    fun aMessageHidesAfterItsLifetimeWithoutNewerOnes() {
        val shownAt = mapOf("a" to 0L, "b" to 2_000L)
        assertEquals(setOf("a", "b"), compactChatVisibleIds(listOf("a", "b"), shownAt, 4_999L, 5_000L))
        assertEquals(setOf("b"), compactChatVisibleIds(listOf("a", "b"), shownAt, 5_000L, 5_000L))
        assertTrue(compactChatVisibleIds(listOf("a", "b"), shownAt, 7_000L, 5_000L).isEmpty())
    }

    @Test
    fun aMessageNotYetRecordedWaits() {
        assertTrue(compactChatVisibleIds(listOf("a"), emptyMap(), 10_000L, 5_000L).isEmpty())
    }

    @Test
    fun theNextHideIsTheEarliestStillAhead() {
        val shownAt = mapOf("a" to 0L, "b" to 2_000L, "c" to 3_000L)
        assertEquals(7_000L, compactChatNextHideMillis(shownAt, nowMillis = 5_000L, lifetimeMillis = 5_000L))
    }

    @Test
    fun nothingHidesOnceEveryMessageHid() {
        assertNull(compactChatNextHideMillis(mapOf("a" to 0L), nowMillis = 5_000L, lifetimeMillis = 5_000L))
        assertNull(compactChatNextHideMillis(emptyMap(), nowMillis = 0L, lifetimeMillis = 5_000L))
    }

    @Test
    fun theTopMessageFadesOnlyWhenTheCornerIsFull() {
        val ids = listOf("a", "b", "c")
        assertEquals("a", compactChatFadedId(ids, setOf("a", "b", "c"), capacity = 3))
        assertNull(compactChatFadedId(ids, setOf("b", "c"), capacity = 3))
    }

    @Test
    fun theFoldedBarIsAShareOfItsHeight() {
        assertEquals(60, foldedHeightPx(120, 0.5f))
        assertEquals(120, foldedHeightPx(120, 1f))
    }

    @Test
    fun theFoldedBarNeverGoesBelowZero() {
        assertEquals(0, foldedHeightPx(120, 0f))
        assertEquals(0, foldedHeightPx(120, -0.2f))
        assertEquals(0, foldedHeightPx(-10, 1f))
    }

    @Test
    fun theFullChatRisesFromTheCompactHeight() {
        assertEquals(200, fullscreenChatRevealedHeightPx(compactPx = 200, fullPx = 1000, progress = 0f))
        assertEquals(600, fullscreenChatRevealedHeightPx(compactPx = 200, fullPx = 1000, progress = 0.5f))
        assertEquals(1000, fullscreenChatRevealedHeightPx(compactPx = 200, fullPx = 1000, progress = 1f))
    }

    @Test
    fun theFullChatRisesFromNothingWithoutACompactHeight() {
        assertEquals(0, fullscreenChatRevealedHeightPx(compactPx = 0, fullPx = 1000, progress = 0f))
    }

    @Test
    fun theRevealedHeightStaysWithinTheFullChat() {
        assertEquals(500, fullscreenChatRevealedHeightPx(compactPx = 800, fullPx = 500, progress = 0f))
        assertEquals(0, fullscreenChatRevealedHeightPx(compactPx = -20, fullPx = -10, progress = 2f))
        assertEquals(1000, fullscreenChatRevealedHeightPx(compactPx = 200, fullPx = 1000, progress = 1.5f))
    }

    @Test
    fun recentChatLoadedBeforeTheWelcomeStaysOutOfTheCompactChat() {
        val messages = listOf(
            notice("recent-1"),
            notice("recent-2"),
            chatNoticeMessage(ChatNotice.Welcome, CHAT_WELCOME_NOTICE_ID),
            notice("live-1"),
        )
        assertEquals(
            listOf(CHAT_WELCOME_NOTICE_ID, "live-1"),
            compactChatCandidates(messages).map { it.id },
        )
    }

    @Test
    fun aChatWithoutTheWelcomeShowsAllItsMessages() {
        val messages = listOf(notice("replay-1"), notice("replay-2"))
        assertEquals(listOf("replay-1", "replay-2"), compactChatCandidates(messages).map { it.id })
    }

    @Test
    fun theBarFoldsOverTheVideoUntilTheComposerOpens() {
        assertTrue(chatInputBarFolded(overVideo = true, composerOpen = false))
        assertFalse(chatInputBarFolded(overVideo = true, composerOpen = true))
        assertFalse(chatInputBarFolded(overVideo = false, composerOpen = false))
    }

    private fun notice(id: String) = chatNoticeMessage(ChatNotice.RecentChatFailed, id)
}
