package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.chat.ChatBadge
import name.alexwayfer.customtv.data.DisplayedChatBadges
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatterCardBadgesTest {
    private val subscriber = ChatBadge("subscriber", "12")
    private val moderator = ChatBadge("moderator", "1")
    private val channelUrls = mapOf("subscriber/12" to "sub-url")

    @Test
    fun aCardFromAMessageKeepsItsBadgesAndIgnoresALoad() {
        val card = card(badges = listOf(subscriber), badgesKnown = true)
        val loaded = DisplayedChatBadges(listOf(moderator), mapOf("moderator/1" to "mod-url"))
        assertEquals(ChatterCardBadges(listOf(subscriber), channelUrls), chatterCardBadges(card, loaded))
    }

    @Test
    fun aCardWithoutBadgesHasNoneWhileTheyLoad() {
        assertEquals(ChatterCardBadges(null, channelUrls), chatterCardBadges(card(badgesKnown = false), null))
    }

    @Test
    fun aCardWithoutBadgesShowsTheLoadedOnesWithTheirImages() {
        val loaded = DisplayedChatBadges(listOf(moderator), mapOf("moderator/1" to "mod-url"))
        assertEquals(
            ChatterCardBadges(listOf(moderator), channelUrls + ("moderator/1" to "mod-url")),
            chatterCardBadges(card(badgesKnown = false), loaded),
        )
    }

    @Test
    fun aLoadedEmptyListMeansAChatterWithoutBadges() {
        val loaded = DisplayedChatBadges(emptyList(), emptyMap())
        assertEquals(ChatterCardBadges(emptyList(), channelUrls), chatterCardBadges(card(badgesKnown = false), loaded))
    }

    private fun card(badges: List<ChatBadge> = emptyList(), badgesKnown: Boolean) = ChatterCardRequest(
        login = "someuser",
        displayName = "SomeUser",
        userId = null,
        badges = badges,
        badgeUrls = channelUrls,
        badgesKnown = badgesKnown,
    )
}
