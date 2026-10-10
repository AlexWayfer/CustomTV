package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import name.alexwayfer.customtv.data.CheermoteRepository
import name.alexwayfer.customtv.data.CheermoteTier
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class CheermoteMatcherTest {
    @Test
    fun usesTierForEachCheerAndKeepsMessageText() {
        val message = message("Cheer1 hello Cheer150!", 151)
        val tiers = mapOf("cheer" to listOf(CheermoteTier(1, "one"), CheermoteTier(100, "hundred")))

        val result = CheermoteMatcher.apply(message, tiers)

        assertEquals("Cheer1 hello Cheer150!", result.rawText)
        assertEquals("one", (result.parts[0] as ChatPart.Emote).url)
        assertEquals(ChatPart.Text("1"), result.parts[1])
        assertEquals(ChatPart.Text(" hello "), result.parts[2])
        assertEquals("hundred", (result.parts[3] as ChatPart.Emote).url)
        assertEquals(ChatPart.Text("150"), result.parts[4])
        assertEquals(ChatPart.Text("!"), result.parts[5])
    }

    @Test
    fun convertsOnlyKnownCheermotesAndPreservesOtherEmotes() {
        val original = message("Party100 ", 100).copy(
            parts = listOf(ChatPart.Text("Party100 "), ChatPart.Emote("Kappa", "kappa")),
        )
        val result = CheermoteMatcher.apply(original, mapOf("party" to listOf(CheermoteTier(1, "party"))))

        assertEquals("party", (result.parts[0] as ChatPart.Emote).url)
        assertEquals(ChatPart.Text("100"), result.parts[1])
        assertEquals(ChatPart.Emote("Kappa", "kappa"), result.parts.last())
        assertEquals(message("Party100 ", null).parts, CheermoteMatcher.apply(message("Party100 ", null),
            mapOf("party" to listOf(CheermoteTier(1, "party")))).parts)
    }

    @Test
    fun readsAnimatedDarkImagesFromTwitchCatalog() {
        val payload = JSONObject("""{"data":{"user":{"cheer":{"emotes":[{"prefix":"Cheer","tiers":[{"bits":100,"images":[{"theme":"DARK","isAnimated":true,"dpiScale":2,"url":"https://example.com/cheer.gif"}]}]}]}}}}""")

        assertEquals("https://example.com/cheer.gif", CheermoteRepository.parse(payload)["cheer"]?.single()?.imageUrl)
    }

    private fun message(text: String, bits: Int?) = ChatMessage(
        id = "1",
        userLogin = "viewer",
        displayName = "Viewer",
        color = Color.White,
        rawText = text,
        parts = listOf(ChatPart.Text(text)),
        timestampMillis = 1,
        cheerBits = bits,
    )
}
