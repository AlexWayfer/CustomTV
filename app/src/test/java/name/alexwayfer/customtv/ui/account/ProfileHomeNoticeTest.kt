package name.alexwayfer.customtv.ui.account

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileHomeNoticeTest {
    private fun ready(chaptersFailed: Set<String> = emptySet()) =
        ChannelRecordings.Ready(ChannelRecordingsPage(emptyList(), emptyMap(), chaptersFailed = chaptersFailed))

    @Test
    fun recordingsStillLoadingShowTheSpinner() {
        assertEquals(
            ProfileHomeNotice.Loading,
            profileHomeNotice(ChannelRecordings.Loading, hasContent = true, chaptersLoading = false),
        )
    }

    @Test
    fun recordingsThatFailedAreAFailureEvenBesideALiveStream() {
        assertEquals(
            ProfileHomeNotice.Failed,
            profileHomeNotice(ChannelRecordings.Failed, hasContent = true, chaptersLoading = false),
        )
    }

    @Test
    fun loadedContentNeedsNoNotice() {
        assertEquals(ProfileHomeNotice.None, profileHomeNotice(ready(), hasContent = true, chaptersLoading = true))
    }

    @Test
    fun failedChaptersAreNotAnEmptyHomeTheirNoteSaysSo() {
        assertEquals(
            ProfileHomeNotice.None,
            profileHomeNotice(ready(chaptersFailed = setOf("1")), hasContent = false, chaptersLoading = true),
        )
    }

    @Test
    fun chaptersLoadingTheFirstTimeKeepTheSpinnerOnAnOtherwiseEmptyHome() {
        assertEquals(ProfileHomeNotice.Loading, profileHomeNotice(ready(), hasContent = false, chaptersLoading = true))
    }

    @Test
    fun aHomeThatLoadedEverythingAndHoldsNothingIsEmpty() {
        assertEquals(ProfileHomeNotice.Empty, profileHomeNotice(ready(), hasContent = false, chaptersLoading = false))
    }
}
