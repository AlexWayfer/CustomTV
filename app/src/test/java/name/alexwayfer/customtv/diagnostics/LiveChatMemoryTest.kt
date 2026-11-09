package name.alexwayfer.customtv.diagnostics

import androidx.compose.ui.graphics.Color
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatterHistory
import name.alexwayfer.customtv.chat.PersonalEmotes
import name.alexwayfer.customtv.chat.SevenTvCosmetics
import name.alexwayfer.customtv.chat.SevenTvEmote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LiveChatMemoryTest {
    @Test
    fun `empty live chat reports zero counts`() {
        assertEquals(
            "rows=0 authors=0 authorRows=0 mentionTargets=0 mentionRows=0 " +
                "personalSets=0 personalEmotes=0 personalUsers=0 bttvUsers=0 bttvEmotes=0 " +
                "paints=0 badges=0 paintUsers=0 badgeUsers=0",
            liveChatMemoryCounts(emptyList(), ChatterHistory(), PersonalEmotes(), SevenTvCosmetics()),
        )
    }

    @Test
    fun `history counts include authors and mentions outside the visible rows without their contents`() {
        val first = message("private-first")
        val second = message("private-second")
        val counts = liveChatMemoryCounts(
            listOf(second),
            ChatterHistory(
                messages = mapOf("private-alice" to listOf(first, second), "private-bob" to listOf(first)),
                mentions = mapOf("private-carol" to listOf(first, second)),
            ),
            PersonalEmotes(),
            SevenTvCosmetics(),
        )
        assertEquals(
            "rows=1 authors=2 authorRows=3 mentionTargets=1 mentionRows=2 " +
                "personalSets=0 personalEmotes=0 personalUsers=0 bttvUsers=0 bttvEmotes=0 " +
                "paints=0 badges=0 paintUsers=0 badgeUsers=0",
            counts,
        )
        assertFalse(counts.contains("private"))
    }

    @Test
    fun `personal set shared by several users is counted once`() {
        val emote = SevenTvEmote(url = "https://private.invalid/emote", id = "private-emote")
        val counts = liveChatMemoryCounts(
            emptyList(),
            ChatterHistory(),
            PersonalEmotes(
                sevenTvSets = mapOf("set" to mapOf("name" to emote)),
                sevenTvUsers = mapOf("alice" to listOf("set"), "bob" to listOf("set")),
                bttv = mapOf("carol" to mapOf("name" to emote)),
            ),
            SevenTvCosmetics(userPaints = mapOf("alice" to "paint"), userBadges = mapOf("bob" to "badge")),
        )
        assertEquals(
            "rows=0 authors=0 authorRows=0 mentionTargets=0 mentionRows=0 " +
                "personalSets=1 personalEmotes=1 personalUsers=2 bttvUsers=1 bttvEmotes=1 " +
                "paints=0 badges=0 paintUsers=1 badgeUsers=1",
            counts,
        )
        assertFalse(counts.contains("private"))
    }

    private fun message(id: String) = ChatMessage(
        id = id,
        userLogin = "private-login",
        displayName = "private-display-name",
        color = Color.Unspecified,
        rawText = "private-message",
        parts = emptyList(),
        timestampMillis = 0L,
    )
}
