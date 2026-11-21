package name.alexwayfer.customtv.data

import android.content.Context
import androidx.annotation.StringRes
import androidx.concurrent.futures.await
import androidx.javascriptengine.JavaScriptException
import androidx.javascriptengine.JavaScriptIsolate
import androidx.javascriptengine.JavaScriptSandbox
import androidx.javascriptengine.Message
import androidx.javascriptengine.SandboxUnsupportedException
import java.io.IOException
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.UiText
import org.json.JSONException
import org.json.JSONObject

private val checkTimeout = 60.seconds
private const val CHECK_ASSET = "chatter-label-check.js"
private const val CHECK_PORT = "chatterCheck"

internal object ChatterLabelChecks {
    // Android allows one sandbox connection at a time. Each check starts with fresh JavaScript state.
    private val mutex = Mutex()

    suspend fun run(
        context: Context,
        script: String,
        user: LabeledChatter,
        channel: ChatterCheckChannel?,
        twitchAvailable: Boolean,
    ): ChatterCheckResult = mutex.withLock {
        if (!JavaScriptSandbox.isSupported()) return@withLock checkFailure(R.string.chatter_check_unavailable)
        withTimeoutOrNull(checkTimeout) {
            withContext(Dispatchers.IO) {
                evaluateChatterCheck(context.applicationContext, script, user, channel, twitchAvailable)
            }
        } ?: checkFailure(R.string.chatter_check_timeout)
    }
}

private suspend fun evaluateChatterCheck(
    context: Context,
    script: String,
    user: LabeledChatter,
    channel: ChatterCheckChannel?,
    twitchAvailable: Boolean,
): ChatterCheckResult = try {
    openCheckSandbox(context).use { sandbox ->
        val hasPorts = sandbox.isFeatureSupported(JavaScriptSandbox.JS_FEATURE_MESSAGE_PORTS)
        val hasPromises = sandbox.isFeatureSupported(JavaScriptSandbox.JS_FEATURE_PROMISE_RETURN)
        if (!hasPorts || !hasPromises) {
            checkFailure(R.string.chatter_check_unavailable)
        } else {
            val code = chatterCheckCode(context, script, user, channel, twitchAvailable)
            val logs = ChatterBestLogs(channel?.id, user.userId)
            sandbox.createIsolate().use { isolate -> runChatterCheck(isolate, code, logs) }
        }
    }
} catch (error: JavaScriptException) {
    ChatterCheckResult.Failed(error.message?.let { UiText.Raw(it) } ?: UiText.Resource(R.string.chatter_check_failed))
} catch (_: SandboxUnsupportedException) {
    checkFailure(R.string.chatter_check_unavailable)
} catch (error: CancellationException) {
    throw error
} catch (_: IllegalStateException) {
    checkFailure(R.string.chatter_check_failed)
} catch (_: IOException) {
    checkFailure(R.string.chatter_check_failed)
} catch (_: JSONException) {
    checkFailure(R.string.chatter_check_failed)
}

/** If the page closes while binding, close the arriving sandbox so the next check can bind again. */
private suspend fun openCheckSandbox(context: Context): JavaScriptSandbox = suspendCancellableCoroutine { waiter ->
    val connection = JavaScriptSandbox.createConnectedInstanceAsync(context)
    connection.addListener({
        try {
            // The completion listener runs only after the future is done.
            @Suppress("BlockingMethodInNonBlockingContext")
            waiter.resume(connection.get()) { _, sandbox, _ -> sandbox.close() }
        } catch (error: ExecutionException) {
            waiter.resumeWithException(IOException("Could not connect to the JavaScript sandbox", error.cause))
        }
    }, Runnable::run)
}

private suspend fun runChatterCheck(
    isolate: JavaScriptIsolate,
    code: String,
    logs: ChatterBestLogs,
): ChatterCheckResult = coroutineScope {
    val requests = Channel<String>(Channel.BUFFERED)
    // evaluateChatterCheck verifies message port support before creating this isolate.
    @Suppress("RequiresFeature")
    val channel = isolate.createMessageChannel(CHECK_PORT, Runnable::run) { message ->
        if (message.type == Message.TYPE_STRING && requests.trySend(message.string).isFailure) {
            requests.close(IOException("Too many pending label check requests"))
        }
    }
    channel.use { port ->
        val worker = launch {
            for (request in requests) {
                val query = request.toJsonObjectOrNull() ?: continue
                val response = when (query.optString("kind")) {
                    "archive" -> logs.search(query)
                    "twitch-gql" -> requestChatterCheckTwitch(query)
                    else -> JSONObject().put("error", "invalid-request")
                }.put("id", query.optInt("id"))
                port.postMessage(Message.createStringMessage(response.toString()))
            }
        }
        try {
            parseChatterCheckResult(JSONObject(isolate.evaluateJavaScriptAsync(code).await()))
        } finally {
            worker.cancel()
            requests.cancel()
        }
    }
}

private fun chatterCheckCode(
    context: Context,
    script: String,
    user: LabeledChatter,
    channel: ChatterCheckChannel?,
    twitchAvailable: Boolean,
): String {
    val userInput = JSONObject().put("id", user.userId).put("login", user.login).put("username", user.displayName)
    val channelInput = channel?.let { JSONObject().put("login", it.login) }
    val input = JSONObject()
        .put("user", userInput)
        .put("channel", channelInput ?: JSONObject.NULL)
        .put("channelId", channel?.id ?: JSONObject.NULL)
        .put("timeoutMillis", checkTimeout.inWholeMilliseconds)
        .put("twitchAvailable", twitchAvailable)
    val runtime = context.assets.open(CHECK_ASSET).bufferedReader().use { it.readText() }
    return "$runtime\nrunChatterCheck($input, async (user, channel, bestLogs, twitch, signal) => {\n$script\n});"
}

private fun checkFailure(@StringRes messageId: Int): ChatterCheckResult =
    ChatterCheckResult.Failed(UiText.Resource(messageId))
