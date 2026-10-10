package name.alexwayfer.customtv.data

import kotlinx.coroutines.sync.Mutex

private val twitchSessionRefreshMutex = Mutex()

internal suspend fun <T> withTwitchSessionRefreshLock(block: suspend () -> T): T {
    twitchSessionRefreshMutex.lock()
    return try {
        block()
    } finally {
        twitchSessionRefreshMutex.unlock()
    }
}
