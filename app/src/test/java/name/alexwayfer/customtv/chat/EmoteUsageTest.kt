package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmoteUsageTest {
    @Test
    fun usedEmotesLeadAndALaterTieBreaksAMatchingCount() {
        val emotes = listOf(
            PickerEmote("Kripp", "https://kripp"),
            PickerEmote("KappaPride", "https://pride"),
            PickerEmote("Kappa", "https://kappa"),
            PickerEmote("Keepo", "https://keepo"),
        )
        val usage = listOf(
            EmoteUse("Kappa", count = 2, usedAtMillis = 10L),
            EmoteUse("KappaPride", count = 2, usedAtMillis = 20L),
            EmoteUse("Keepo", count = 1, usedAtMillis = 30L),
        )

        assertEquals(
            listOf("KappaPride", "Kappa", "Keepo", "Kripp"),
            matchingPickerEmotes(emotes, "k", usage).map { it.name },
        )
    }

    @Test
    fun aSentMessageCountsExactEmoteNamesOnThatChannel() {
        val known = setOf("Kappa", ":)")
        val once = emoteUsesAfterMessage(emptyList(), "привет Kappa Kappa :)", known, usedAtMillis = 5L)
        assertEquals(
            listOf(EmoteUse(":)", 1, 5L), EmoteUse("Kappa", 2, 5L)),
            once.sortedBy { it.name },
        )
        val again = emoteUsesAfterMessage(once, "Kappa", known, usedAtMillis = 9L)
        assertEquals(EmoteUse("Kappa", 3, 9L), again.first { it.name == "Kappa" })
        assertEquals(once, emoteUsesAfterMessage(once, "kappa", known, usedAtMillis = 9L))
    }

    @Test
    fun ownLiveMessageCountsWhateverDeviceSentIt() {
        assertEquals("Kappa hi", ownEmoteUsageText(chatLine(userId = "me", text = "Kappa hi"), ownUserId = "me"))
    }

    @Test
    fun ownHighlightedRewardAndActionMessagesCount() {
        assertEquals("Kappa", ownEmoteUsageText(chatLine("me", "Kappa", ChatEventKind.Highlight), "me"))
        assertEquals("Kappa", ownEmoteUsageText(chatLine("me", "Kappa", ChatEventKind.Reward), "me"))
    }

    @Test
    fun anotherChattersMessageDoesNotCount() {
        assertNull(ownEmoteUsageText(chatLine(userId = "other", text = "Kappa"), ownUserId = "me"))
    }

    @Test
    fun nothingCountsWhenSignedOutOrTheSenderIsUnknown() {
        assertNull(ownEmoteUsageText(chatLine(userId = "me", text = "Kappa"), ownUserId = null))
        assertNull(ownEmoteUsageText(chatLine(userId = "", text = "Kappa"), ownUserId = ""))
        assertNull(ownEmoteUsageText(chatLine(userId = null, text = "Kappa"), ownUserId = "me"))
    }

    @Test
    fun deletionsAndSystemLinesAboutTheUserDoNotCount() {
        assertNull(ownEmoteUsageText(chatLine("me", "Kappa", ChatEventKind.MessageDeleted), "me"))
        assertNull(ownEmoteUsageText(chatLine("me", "Kappa", ChatEventKind.UserMessagesDeleted), "me"))
        assertNull(ownEmoteUsageText(chatLine("me", "Kappa", ChatEventKind.System), "me"))
    }

    @Test
    fun anOwnMessageWithoutTextDoesNotCount() {
        assertNull(ownEmoteUsageText(chatLine(userId = "me", text = " "), ownUserId = "me"))
    }

    @Test
    fun usageSurvivesARoundTripAndStaysSplitByChannel() {
        val encoded = encodeEmoteUsage(
            mapOf(
                "one" to listOf(EmoteUse("Kappa", 2, 10L)),
                "two" to listOf(EmoteUse("HeyGuys", 1, 4L)),
            ),
        )
        val decoded = decodeEmoteUsage(encoded)
        assertEquals(listOf(EmoteUse("Kappa", 2, 10L)), decoded?.get("one"))
        assertEquals(listOf(EmoteUse("HeyGuys", 1, 4L)), decoded?.get("two"))
        assertNull(decodeEmoteUsage("{"))
    }

    private fun chatLine(
        userId: String?,
        text: String,
        kind: ChatEventKind = ChatEventKind.Normal,
    ) = ChatMessage(
        id = "id",
        userLogin = "login",
        displayName = "Login",
        color = Color.Unspecified,
        rawText = text,
        parts = emptyList(),
        timestampMillis = 0L,
        eventKind = kind,
        userId = userId,
    )
}
