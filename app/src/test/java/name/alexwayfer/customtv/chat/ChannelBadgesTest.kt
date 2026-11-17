package name.alexwayfer.customtv.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import name.alexwayfer.customtv.data.ChannelBadgeUrls
import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelBadgesTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val loads = mutableListOf<String>()
    private val cancelled = mutableListOf<String>()
    private val publishers = mutableMapOf<String, (ChannelBadgeUrls) -> Unit>()
    private val badges = ChannelBadges(
        scope,
        restore = { null },
        load = { channel, publish ->
            loads += channel
            publishers[channel] = publish
            publish(urls(channel))
            try {
                awaitCancellation()
            } finally {
                cancelled += channel
            }
        },
    )

    @Test
    fun hidingTheChatStopsRetriesAndReturningResumesThemWithoutClearingBadges() {
        try {
            badges.open("alice")
            badges.setForeground(false)
            assertEquals(listOf("alice"), cancelled)
            assertEquals(urls("alice").twitch, badges.urls.value)

            badges.setForeground(true)
            assertEquals(listOf("alice", "alice"), loads)
            assertEquals(urls("alice").twitch, badges.urls.value)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun aChannelOpenedWhileHiddenLoadsOnlyAfterReturning() {
        try {
            badges.setForeground(false)
            badges.open("alice")
            assertEquals(emptyList<String>(), loads)

            badges.setForeground(true)
            assertEquals(listOf("alice"), loads)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun changingTheChannelCancelsTheOldLoadAndIgnoresItsLateResult() {
        try {
            badges.open("alice")
            badges.open("bob")
            publishers.getValue("alice")(urls("alice"))

            assertEquals(listOf("alice"), cancelled)
            assertEquals(urls("bob").twitch, badges.urls.value)
        } finally {
            scope.cancel()
        }
    }

    private fun urls(channel: String) = ChannelBadgeUrls(twitch = mapOf("moderator/1" to "https://badge/$channel"))
}
