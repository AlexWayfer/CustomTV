package name.alexwayfer.customtv.ui.settings

import name.alexwayfer.customtv.chat.DEFAULT_CHAT_TEXT_SIZE
import name.alexwayfer.customtv.data.AppSettings
import name.alexwayfer.customtv.data.ChatSettings
import name.alexwayfer.customtv.data.KeywordPhrase
import name.alexwayfer.customtv.data.LinkPreviewMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RestoredSettingsTest {
    @Test
    fun unsetHighlightStaysOutOfTheSwitch() {
        assertNull(animatedControlValue<AppSettings>(null))
    }

    @Test
    fun savedHighlightIsTheFirstSwitchValue() {
        val saved = AppSettings(highlightFirstMessages = true)

        assertEquals(saved, animatedControlValue(saved))
        assertTrue(animatedControlValue(saved)!!.highlightFirstMessages)
    }

    @Test
    fun playbackUsesDefaultsBeforeRestore() {
        val playback = (null as AppSettings?).beforeRestore()

        assertTrue(playback.sevenTvEmotes)
        assertTrue(playback.ffzEmotes)
        assertTrue(playback.bttvEmotes)
        assertTrue(playback.meMessageItalic)
        assertFalse(playback.highlightFirstMessages)
        assertFalse(playback.emoteCompletionWithoutColon)
        assertTrue(playback.streamStartNotifications)
        assertFalse(playback.streamChangeNotifications)
        assertEquals(LinkPreviewMode.None, playback.linkPreviewMode)
        assertEquals(emptyList<KeywordPhrase>(), playback.keywordPhrases)
    }

    @Test
    fun chatRowsUseChatDefaultsBeforeRestore() {
        val chat = (null as ChatSettings?).beforeRestore()

        assertTrue(chat.readableColors)
        assertFalse(chat.timestamps)
        assertEquals(DEFAULT_CHAT_TEXT_SIZE, chat.textSize)
        assertFalse(chat.smoothChatScroll)
    }

    @Test
    fun unsetChatSettingsKeepTheSheetClosed() {
        assertNull(animatedControlValue<ChatSettings>(null))
    }

    @Test
    fun savedChatTimestampsAreTheFirstSwitchValue() {
        val saved = ChatSettings(timestamps = true)

        assertTrue(animatedControlValue(saved)!!.timestamps)
    }

    @Test
    fun playbackKeepsTheSavedHighlight() {
        val saved = AppSettings(highlightFirstMessages = true)

        assertTrue(saved.beforeRestore().highlightFirstMessages)
    }

    @Test
    fun gistSeedAloneDoesNotShowTheBody() {
        val seed = chatterLabelFieldSeed(token = "ghp_saved", gistId = "a".repeat(32))

        assertNull(settingsBody(settings = null, labelSeed = seed))
    }

    @Test
    fun settingsAloneDoNotShowTheBody() {
        assertNull(settingsBody(settings = AppSettings(), labelSeed = null))
    }

    @Test
    fun bothSnapshotsShowTheBodyTogether() {
        val saved = AppSettings(highlightFirstMessages = true)
        val seed = chatterLabelFieldSeed(token = "ghp_saved", gistId = "a".repeat(32))
        val body = settingsBody(saved, seed)

        assertEquals(saved, body?.settings)
        assertEquals(seed, body?.labelSeed)
    }
}
