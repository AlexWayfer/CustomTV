package name.alexwayfer.customtv.chat

import org.json.JSONException
import java.io.IOException

/** A load that failed on the network or on its answer is a failed result; any other error is a bug and is thrown. */
internal inline fun <T> loadResult(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (error: IOException) {
        Result.failure(error)
    } catch (error: JSONException) {
        Result.failure(error)
    }
}
