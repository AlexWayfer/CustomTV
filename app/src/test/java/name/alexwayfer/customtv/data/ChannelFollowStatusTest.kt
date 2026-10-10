package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.chat.FollowForChatMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelFollowStatusTest {
    @Test
    fun aLoadedFollowsListOpensTheChannelWithoutARequest() {
        assertFalse(channelFollowNeedsRequest(followedIds = setOf("123"), returned = false))
        assertFalse(channelFollowNeedsRequest(followedIds = emptySet(), returned = false))
    }

    @Test
    fun anUnknownFollowsListAsksTwitch() {
        assertTrue(channelFollowNeedsRequest(followedIds = null, returned = false))
    }

    @Test
    fun comingBackToTheAppAsksTwitchEvenWithALoadedList() {
        assertTrue(channelFollowNeedsRequest(followedIds = setOf("123"), returned = true))
    }

    @Test
    fun withoutARequestTheFollowsListDecides() {
        assertEquals(ChannelFollowStatus.Following, channelFollowStatus(setOf("123"), FollowForChatMode.Unknown, "123"))
        assertEquals(ChannelFollowStatus.NotFollowing, channelFollowStatus(setOf("456"), FollowForChatMode.Unknown, "123"))
    }

    @Test
    fun withoutARequestOrAListTheFollowIsUnknown() {
        assertEquals(ChannelFollowStatus.Unknown, channelFollowStatus(null, FollowForChatMode.Unknown, "123"))
    }

    @Test
    fun aFollowMadeInABrowserFillsTheHeartBeforeTheListKnowsIt() {
        assertEquals(ChannelFollowStatus.Following, channelFollowStatus(setOf("456"), FollowForChatMode.Following(1L), "123"))
    }

    @Test
    fun anUnfollowMadeInABrowserEmptiesTheHeartTheListStillFills() {
        assertEquals(ChannelFollowStatus.NotFollowing, channelFollowStatus(setOf("123"), FollowForChatMode.NotFollowing, "123"))
    }

    @Test
    fun aFollowMadeInABrowserOutdatesAListWithoutTheChannel() {
        assertTrue(followListOutdated(setOf("456"), FollowForChatMode.Following(1L), "123"))
    }

    @Test
    fun anUnfollowMadeInABrowserOutdatesAListWithTheChannel() {
        assertTrue(followListOutdated(setOf("123"), FollowForChatMode.NotFollowing, "123"))
    }

    @Test
    fun anAnswerTheListAgreesWithKeepsIt() {
        assertFalse(followListOutdated(setOf("123"), FollowForChatMode.Following(1L), "123"))
        assertFalse(followListOutdated(setOf("456"), FollowForChatMode.NotFollowing, "123"))
    }

    @Test
    fun noAnswerOrNoListLeavesTheListAlone() {
        assertFalse(followListOutdated(setOf("456"), FollowForChatMode.Unknown, "123"))
        assertFalse(followListOutdated(null, FollowForChatMode.Following(1L), "123"))
    }
}
