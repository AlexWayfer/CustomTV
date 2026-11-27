package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatImageRetentionTest {
    @Test
    fun newAndExistingSettingsWithoutAValidValueUseThreeDays() {
        assertEquals(ChatImageRetention.ThreeDays, AppSettings().chatImageRetention)
        assertEquals(ChatImageRetention.ThreeDays, storedChatImageRetention(null))
        assertEquals(ChatImageRetention.ThreeDays, storedChatImageRetention("unknown"))
    }

    @Test
    fun theSixSavedChoicesMapToTheHostsSupportedSeconds() {
        val choices = listOf(
            "OneHour" to 3_600L,
            "OneDay" to 86_400L,
            "ThreeDays" to 259_200L,
            "OneWeek" to 604_800L,
            "OneMonth" to 2_592_000L,
            "NoExpiry" to 0L,
        )
        assertEquals(choices, ChatImageRetention.entries.map { it.name to it.seconds })
        ChatImageRetention.entries.forEach { choice -> assertEquals(choice, storedChatImageRetention(choice.name)) }
    }
}
