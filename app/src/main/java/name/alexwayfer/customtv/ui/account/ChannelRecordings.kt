package name.alexwayfer.customtv.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

internal sealed interface ChannelRecordings {
    data object Loading : ChannelRecordings
    data object Failed : ChannelRecordings
    data class Ready(val page: ChannelRecordingsPage) : ChannelRecordings
}

/**
 * A channel's recent recordings and their chapters, shared by the profile's Home and Videos tabs.
 * They load once, in one request by the channel's login, while the profile is open. The login is
 * known when the profile opens, so the load does not wait for the channel's id.
 */
@Stable
internal class ChannelRecordingsState {
    var recordings: ChannelRecordings by mutableStateOf(ChannelRecordings.Loading)
        private set
    var reloads by mutableIntStateOf(0)
        private set

    fun reload() {
        reloads += 1
    }

    suspend fun load(channelLogin: String, repository: ChannelRecordingsRepository) {
        recordings = ChannelRecordings.Loading
        val page = repository.load(channelLogin)
        recordings = page?.let(ChannelRecordings::Ready) ?: ChannelRecordings.Failed
    }
}

@Composable
internal fun rememberChannelRecordings(channelLogin: String): ChannelRecordingsState {
    val state = remember(channelLogin) { ChannelRecordingsState() }
    val repository = remember { ChannelRecordingsRepository() }
    LaunchedEffect(state, state.reloads) {
        state.load(channelLogin, repository)
    }
    return state
}
