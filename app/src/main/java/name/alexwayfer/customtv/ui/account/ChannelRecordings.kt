package name.alexwayfer.customtv.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal sealed interface ChannelRecordings {
    data object Loading : ChannelRecordings
    data object Failed : ChannelRecordings
    data class Ready(val page: ChannelRecordingsPage) : ChannelRecordings
}

/** What a recording's card knows of its chapters: them, or that they did not load and whether they load again. */
internal data class VideoChaptersLoad(val categories: VodCategories?, val failed: Boolean, val loading: Boolean)

/**
 * A channel's recent recordings and their chapters, shared by the profile's Home and Videos tabs.
 * They load once, in one request by the channel's login, while the profile is open. The login is
 * known when the profile opens, so the load does not wait for the channel's id. Chapters that failed
 * in that request are asked again by the recordings' ids, and the recordings show meanwhile.
 */
@Stable
internal class ChannelRecordingsState(
    private val scope: CoroutineScope,
    private val chaptersRepository: VodChaptersRepository,
) {
    var recordings: ChannelRecordings by mutableStateOf(ChannelRecordings.Loading)
        private set
    var reloads by mutableIntStateOf(0)
        private set

    /** The recordings whose chapters are being asked again. */
    var chaptersLoading: Set<String> by mutableStateOf(emptySet())
        private set

    fun reload() {
        reloads += 1
    }

    fun chaptersOf(videoId: String): VideoChaptersLoad {
        val page = (recordings as? ChannelRecordings.Ready)?.page
        return VideoChaptersLoad(
            categories = page?.chapters?.get(videoId),
            failed = page?.chaptersFailed?.contains(videoId) == true,
            loading = videoId in chaptersLoading,
        )
    }

    /** Asks again for the chapters of [videoIds] that did not load. */
    fun retryChapters(videoIds: Set<String>) {
        scope.launch { loadChapters(videoIds) }
    }

    suspend fun load(channelLogin: String, repository: ChannelRecordingsRepository) {
        recordings = ChannelRecordings.Loading
        val page = repository.load(channelLogin)
        if (page == null) {
            recordings = ChannelRecordings.Failed
            return
        }
        // Chapters that failed show as failed only once asking them again failed too.
        recordings = ChannelRecordings.Ready(page.copy(chaptersFailed = emptySet()))
        if (page.chaptersFailed.isNotEmpty()) loadChapters(page.chaptersFailed)
    }

    private suspend fun loadChapters(videoIds: Set<String>) {
        val asked = videoIds - chaptersLoading
        if (asked.isEmpty()) return
        chaptersLoading = chaptersLoading + asked
        try {
            val loaded = chaptersRepository.loadMany(asked)
            (recordings as? ChannelRecordings.Ready)?.let { ready ->
                recordings = ChannelRecordings.Ready(pageWithChapters(ready.page, asked, loaded))
            }
        } finally {
            chaptersLoading = chaptersLoading - asked
        }
    }
}

/**
 * [page] after the chapters of [asked] were asked again: those that [loaded] join the page, and the rest
 * stay failed. A recording the page no longer lists is left out.
 */
internal fun pageWithChapters(
    page: ChannelRecordingsPage,
    asked: Set<String>,
    loaded: Map<String, VodCategories>,
): ChannelRecordingsPage {
    val listed = page.videos.mapTo(mutableSetOf()) { it.id }
    return page.copy(
        chapters = page.chapters + loaded.filterKeys { it in listed },
        chaptersFailed = (page.chaptersFailed + asked.filter { it in listed }) - loaded.keys,
    )
}

@Composable
internal fun rememberChannelRecordings(channelLogin: String): ChannelRecordingsState {
    val scope = rememberCoroutineScope()
    val chaptersRepository = remember { VodChaptersRepository() }
    val state = remember(channelLogin) { ChannelRecordingsState(scope, chaptersRepository) }
    val repository = remember { ChannelRecordingsRepository() }
    LaunchedEffect(state, state.reloads) {
        state.load(channelLogin, repository)
    }
    return state
}
