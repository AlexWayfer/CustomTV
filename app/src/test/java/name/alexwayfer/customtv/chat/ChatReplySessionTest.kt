package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatReplySessionTest {
    private val starterId = "11111111-1111-1111-1111-111111111111"
    private val childId = "22222222-2222-2222-2222-222222222222"
    private val otherId = "33333333-3333-3333-3333-333333333333"

    @Test
    fun aReplyOnTheFirstMessageHidesTheReplyingPlaque() {
        val starter = message(starterId, "host")
        val session = replySessionFor(starter, listOf(starter))
        assertEquals(starterId, session?.starterId)
        assertEquals(starterId, session?.targetId)
        assertFalse(replySessionShowsReplyingTo(session!!))
        assertEquals(starterId, replyParentMessageId(session))
    }

    @Test
    fun aReplyOnALaterMessageShowsWhoIsBeingRepliedTo() {
        val child = message(
            childId,
            "guest",
            ChatReply(
                parentMsgId = starterId,
                parentUserLogin = "host",
                parentDisplayName = "Host",
                parentBody = "start",
                threadParentMsgId = starterId,
                threadParentUserLogin = "host",
            ),
        )
        val session = replySessionFor(child, listOf(child))
        assertEquals(starterId, session?.starterId)
        assertEquals(childId, session?.targetId)
        assertEquals(listOf(starterId, childId), session?.entries?.map { it.id })
        assertEquals("start", session?.entries?.first()?.body)
        assertTrue(replySessionShowsReplyingTo(session!!))
    }

    @Test
    fun cancelRepliesToTheThreadStarterAndHidesThePlaque() {
        val session = ChatReplySession(
            starterId = starterId,
            targetId = childId,
            entries = listOf(entry(starterId), entry(childId)),
        )
        val cancelled = replySessionCancelsToStarter(session)
        assertEquals(starterId, cancelled.targetId)
        assertFalse(replySessionShowsReplyingTo(cancelled))
        assertEquals(starterId, replyParentMessageId(cancelled))
    }

    @Test
    fun choosingAnotherMessageInTheThreadChangesTheReplyTarget() {
        val session = ChatReplySession(
            starterId = starterId,
            targetId = starterId,
            entries = listOf(entry(starterId), entry(childId), entry(otherId)),
        )
        val chosen = replySessionChooses(session, otherId)
        assertEquals(otherId, chosen.targetId)
        assertTrue(replySessionShowsReplyingTo(chosen))
        assertEquals(session, replySessionChooses(session, "missing"))
    }

    @Test
    fun aNoticeCannotBeRepliedTo() {
        val notice = message(starterId, "host").copy(notice = ChatNotice.Welcome)
        assertNull(replySessionFor(notice, listOf(notice)))
        assertFalse(chatMessageCanBeReplied(message("not-a-twitch-id", "host")))
    }

    @Test
    fun aLoadedThreadKeepsALocalBodyAndSortsTheStarterFirst() {
        val local = listOf(entry(childId, "local body", 20))
        val remote = listOf(
            entry(childId, "remote body", 20),
            entry(starterId, "root", 5),
        )
        val merged = mergeThreadEntries(starterId, local, remote)
        assertEquals(listOf(starterId, childId), merged.map { it.id })
        assertEquals("local body", merged[1].body)
    }

    @Test
    fun aLoadedThreadKeepsTheLocalMessageWithBadges() {
        val remote = entry(childId, "hello", 20)
        val localMessage = message(childId, "guest").copy(
            rawText = "hello",
            badges = listOf(ChatBadge("moderator", "1")),
        )
        val local = entry(childId, "hello", 20).copy(message = localMessage)
        val merged = mergeThreadEntries(starterId, listOf(remote), listOf(local))
        assertEquals("moderator/1", merged.single().message?.badges?.single()?.key)
        assertEquals("hello", merged.single().body)
    }

    @Test
    fun theThreadStarterStaysPinnedAndLaterMessagesScroll() {
        val window = threadWindow(
            listOf(entry(starterId, "start"), entry(childId, "later"), entry(otherId, "newest")),
            starterId,
        )
        assertEquals(starterId, window.pinned?.id)
        assertEquals(listOf(childId, otherId), window.following.map { it.id })
    }

    @Test
    fun aThreadWithoutItsStarterScrollsEveryMessage() {
        val window = threadWindow(listOf(entry(childId), entry(otherId)), starterId)
        assertNull(window.pinned)
        assertEquals(listOf(childId, otherId), window.following.map { it.id })
    }

    @Test
    fun aDownwardDragPastTheThresholdClosesTheThread() {
        assertFalse(threadPanelDragCloses(dragPx = 0f, heightPx = 800, slopPx = 200f))
        assertFalse(threadPanelDragCloses(dragPx = 199f, heightPx = 800, slopPx = 200f))
        assertTrue(threadPanelDragCloses(dragPx = 200f, heightPx = 800, slopPx = 200f))
        assertTrue(threadPanelDragCloses(dragPx = 40f, heightPx = 100, slopPx = 200f))
        assertFalse(threadPanelDragCloses(dragPx = 30f, heightPx = 100, slopPx = 200f))
    }

    @Test
    fun aStarterAndRepliesThatFitKeepTheirFullHeight() {
        assertEquals(60, threadPinnedMaxHeightPx(availablePx = 200, pinnedPx = 60, followingPx = 100))
    }

    @Test
    fun aLongStarterLeavesRoomForShortReplies() {
        assertEquals(170, threadPinnedMaxHeightPx(availablePx = 200, pinnedPx = 300, followingPx = 30))
    }

    @Test
    fun aLongStarterAndLongRepliesShareTheBodyInHalves() {
        assertEquals(100, threadPinnedMaxHeightPx(availablePx = 200, pinnedPx = 300, followingPx = 300))
    }

    @Test
    fun aShortStarterKeepsItsHeightBesideLongReplies() {
        assertEquals(40, threadPinnedMaxHeightPx(availablePx = 200, pinnedPx = 40, followingPx = 300))
    }

    @Test
    fun anUnmeasuredPartDoesNotLimitTheStarter() {
        assertEquals(200, threadPinnedMaxHeightPx(availablePx = 200, pinnedPx = 0, followingPx = 300))
        assertEquals(200, threadPinnedMaxHeightPx(availablePx = 200, pinnedPx = 300, followingPx = 0))
    }

    @Test
    fun onlyAReplyWithTheKeyboardOrPickerUpHidesTheChrome() {
        assertTrue(threadReplyHidesChrome(replying = true, composerOpen = true))
        assertFalse(threadReplyHidesChrome(replying = true, composerOpen = false))
        assertFalse(threadReplyHidesChrome(replying = false, composerOpen = true))
    }

    private fun message(id: String, login: String, reply: ChatReply? = null) = ChatMessage(
        id = id,
        userLogin = login,
        displayName = login,
        color = Color.Unspecified,
        rawText = "hi",
        parts = emptyList(),
        timestampMillis = 10,
        reply = reply,
    )

    private fun entry(id: String, body: String = "hi", timestampMillis: Long = 10) = ChatThreadEntry(
        id = id,
        login = id,
        displayName = id,
        body = body,
        timestampMillis = timestampMillis,
    )
}
