package name.alexwayfer.customtv.ui.account

import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelProfileMenuItemsTest {
    @Test
    fun ownProfileHasNeitherWhisperNorReport() {
        assertEquals(
            ChannelProfileMenuItems(whisper = false, report = false),
            channelProfileMenuItems("alice", "Alice"),
        )
    }

    @Test
    fun anotherChannelHasWhisperAndReport() {
        assertEquals(
            ChannelProfileMenuItems(whisper = true, report = true),
            channelProfileMenuItems("alice", "bob"),
        )
    }

    @Test
    fun withoutALoginTheMenuHasReportButNoWhisper() {
        assertEquals(
            ChannelProfileMenuItems(whisper = false, report = true),
            channelProfileMenuItems(null, "bob"),
        )
    }
}
