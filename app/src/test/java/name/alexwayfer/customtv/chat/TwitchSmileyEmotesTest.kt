package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class TwitchSmileyEmotesTest {
    @Test
    fun spellingVariantsOfOneFaceCollapseToTheCanonicalCode() {
        val picked = twitchPickerEmotes(
            listOf(
                face(":o", "8"),
                face(":-O", "555555611"),
                face(":O", "emotesv2_face"),
                face(":|", "5"),
                face(":-|", "444"),
                face(":Z", "z"),
                face(":\\", "slash"),
                face(":/", "noslash"),
                face("8-)", "eight"),
                face("B)", "b"),
                face("Kappa", "25"),
                face("Kappa", "emotesv2_kappa"),
            ),
        )

        assertEquals(listOf(":O", ":|", ":/", "B)", "Kappa"), picked.map { it.name })
        assertEquals(url("emotesv2_face"), picked.first { it.name == ":O" }.url)
        assertEquals(url("emotesv2_kappa"), picked.first { it.name == "Kappa" }.url)
    }

    @Test
    fun turboImageReplacesTheDefaultFaceAndKeepsTheCanonicalCode() {
        val picked = twitchPickerEmotes(
            globals = listOf(face(":)", "1")),
            personalSmilies = listOf(face(":)", "smile")),
            turbo = listOf(face(":-)", "turbo"), face("CoolCat", "cool")),
        )

        assertEquals(listOf(":)", "CoolCat"), picked.map { it.name })
        assertEquals(url("turbo"), picked.first { it.name == ":)" }.url)
    }

    @Test
    fun aPersonalFaceReplacesTheDefaultOnlyWhenItsImageIsNotAlreadyGlobal() {
        val sameImage = twitchPickerEmotes(
            globals = listOf(face(":)", "1"), face(":)", "emotesv2_smile")),
            personalSmilies = listOf(face(":)", "1")),
        )
        assertEquals(url("emotesv2_smile"), sameImage.single().url)

        val purple = twitchPickerEmotes(
            globals = listOf(face(":)", "1"), face(":)", "emotesv2_smile")),
            personalSmilies = listOf(face(":-)", "purple")),
        )
        assertEquals(":)", purple.single().name)
        assertEquals(url("purple"), purple.single().url)
    }

    @Test
    fun aRegexSmileyCodeUsesThePersonalImageAndInsertsTheShortCode() {
        val picked = twitchPickerEmotes(
            globals = listOf(face(":)", "emotesv2_smile")),
            personalSmilies = listOf(face("\\:-?\\)", "purple")),
        )

        assertEquals(":)", picked.single().name)
        assertEquals(url("purple"), picked.single().url)
    }

    private fun face(name: String, id: String) = TwitchCatalogEmote(name, url(id), TwitchEmoteGroup.Global)

    private fun url(id: String) = "https://static-cdn.jtvnw.net/emoticons/v2/$id/static/dark/2.0"
}
