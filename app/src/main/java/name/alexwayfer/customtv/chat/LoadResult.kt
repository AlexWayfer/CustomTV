package name.alexwayfer.customtv.chat

import kotlinx.coroutines.CancellationException

internal inline fun <T> resultUnlessCancelled(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }
}
