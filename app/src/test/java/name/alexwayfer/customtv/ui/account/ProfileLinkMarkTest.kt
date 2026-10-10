package name.alexwayfer.customtv.ui.account

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileLinkMarkTest {
    @Test
    fun knownServiceNamesUseTheirMark() {
        assertEquals(ProfileLinkMark.Twitter, profileLinkMark("twitter", "https://example.com"))
        assertEquals(ProfileLinkMark.Twitter, profileLinkMark("X", "https://example.com"))
        assertEquals(ProfileLinkMark.GitHub, profileLinkMark("github", "https://example.com"))
        assertEquals(ProfileLinkMark.Discord, profileLinkMark("discord", "https://example.com"))
        assertEquals(ProfileLinkMark.Deezer, profileLinkMark("deezer", "https://example.com"))
        assertEquals(ProfileLinkMark.YouTube, profileLinkMark("youtube", "https://example.com"))
        assertEquals(ProfileLinkMark.Instagram, profileLinkMark("instagram", "https://example.com"))
        assertEquals(ProfileLinkMark.TikTok, profileLinkMark("tiktok", "https://example.com"))
        assertEquals(ProfileLinkMark.Facebook, profileLinkMark("facebook", "https://example.com"))
        assertEquals(ProfileLinkMark.Steam, profileLinkMark("Steam", "https://example.com"))
        assertEquals(ProfileLinkMark.Boosty, profileLinkMark("Boosty", "https://example.com"))
    }

    @Test
    fun hostFillsInWhenTheNameIsNotAKnownService() {
        assertEquals(ProfileLinkMark.GitHub, profileLinkMark("website", "https://github.com/alice"))
        assertEquals(ProfileLinkMark.Deezer, profileLinkMark(null, "https://www.deezer.com/artist/1"))
        assertEquals(ProfileLinkMark.Discord, profileLinkMark(null, "https://discord.gg/room"))
        assertEquals(ProfileLinkMark.YouTube, profileLinkMark(null, "https://youtu.be/abc"))
        assertEquals(ProfileLinkMark.Twitter, profileLinkMark(null, "https://x.com/alice"))
        assertEquals(ProfileLinkMark.Steam, profileLinkMark(null, "https://steamcommunity.com/id/alice"))
        assertEquals(ProfileLinkMark.Boosty, profileLinkMark(null, "https://boosty.to/alice"))
    }

    @Test
    fun steamAndBoostyNamesMatchAWholeWordInEitherAlphabetAndCase() {
        assertEquals(ProfileLinkMark.Steam, profileLinkMark("🎮 My STEAM", "https://example.com"))
        assertEquals(ProfileLinkMark.Steam, profileLinkMark("Мой стим!", "https://example.com"))
        assertEquals(ProfileLinkMark.Boosty, profileLinkMark("💜Бусти💜", "https://example.com"))
        assertEquals(ProfileLinkMark.Boosty, profileLinkMark("Support me on boosty", "https://example.com"))
        assertEquals(ProfileLinkMark.Link, profileLinkMark("Бустиренко", "https://example.com"))
        assertEquals(ProfileLinkMark.Link, profileLinkMark("Steamer tips", "https://example.com"))
    }

    @Test
    fun telegramNamesMatchEveryAgreedWordAndItsHostIsExact() {
        for (name in listOf("Telegram", "TELEGA", "✈️ tg", "ТГ", "Мой Телеграм", "телеграмм", "Телега!")) {
            assertEquals(name, ProfileLinkMark.Telegram, profileLinkMark(name, "https://example.com"))
        }
        assertEquals(ProfileLinkMark.Link, profileLinkMark("Телеграмка", "https://example.com"))
        assertEquals(ProfileLinkMark.Telegram, profileLinkMark(null, "https://t.me/alice"))
        assertEquals(ProfileLinkMark.Link, profileLinkMark(null, "https://at.me/alice"))
    }

    @Test
    fun steamAndBoostyHostsMatchOnlyTheirOwnDomains() {
        assertEquals(ProfileLinkMark.Steam, profileLinkMark("store", "https://store.steampowered.com/app/1"))
        assertEquals(ProfileLinkMark.Steam, profileLinkMark(null, "https://STEAMPOWERED.com"))
        assertEquals(ProfileLinkMark.Link, profileLinkMark(null, "https://steampowered.com.example.com"))
        assertEquals(ProfileLinkMark.Link, profileLinkMark(null, "https://steam.example.com"))
        assertEquals(ProfileLinkMark.Link, profileLinkMark(null, "https://boosty.com/alice"))
        assertEquals(ProfileLinkMark.Link, profileLinkMark(null, "https://hypeboosty.to/alice"))
        assertEquals(ProfileLinkMark.Link, profileLinkMark(null, "https://music.youtube.com/channel/1"))
    }

    @Test
    fun unknownNameAndHostStayThePlainLink() {
        assertEquals(ProfileLinkMark.Link, profileLinkMark("website", "https://example.com"))
        assertEquals(ProfileLinkMark.Link, profileLinkMark(null, "not a url"))
        assertEquals(ProfileLinkMark.Link, profileLinkMark("  ", ""))
    }
}
