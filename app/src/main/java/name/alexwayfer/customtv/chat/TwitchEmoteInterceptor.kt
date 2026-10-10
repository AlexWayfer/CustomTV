package name.alexwayfer.customtv.chat

import coil.intercept.Interceptor
import coil.request.ErrorResult
import coil.request.ImageResult
import coil.request.SuccessResult
import java.util.concurrent.ConcurrentHashMap

internal fun twitchEmoteUrl(
    id: String,
    animated: Boolean = true,
    scale: String = TWITCH_EMOTE_SCALE_INLINE,
): String {
    val format = if (animated) "animated" else "default"
    return "https://static-cdn.jtvnw.net/emoticons/v2/$id/$format/dark/$scale"
}

internal fun twitchEmoteScale(url: String): String {
    return TWITCH_EMOTE_SCALE.find(url)?.groupValues?.get(1) ?: TWITCH_EMOTE_SCALE_INLINE
}

internal const val TWITCH_EMOTE_SCALE_INLINE = "2.0"

private val TWITCH_EMOTE_SCALE = Regex("/([1-4]\\.0)$")

internal class TwitchEmoteInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val data = chain.request.data as? String ?: return chain.proceed(chain.request)
        val id = ANIMATED_EMOTE_ID.find(data)?.groupValues?.get(1)
            ?: return chain.proceed(chain.request)
        val scale = twitchEmoteScale(data)
        if (id in staticOnlyIds) {
            return chain.proceed(
                chain.request.newBuilder().data(twitchEmoteUrl(id, animated = false, scale)).build(),
            )
        }
        val result = chain.proceed(chain.request)
        if (result is SuccessResult) return result
        if (isNotFound(result)) {
            staticOnlyIds += id
        }
        return chain.proceed(
            chain.request.newBuilder().data(twitchEmoteUrl(id, animated = false, scale)).build(),
        )
    }

    private companion object {
        val staticOnlyIds: MutableSet<String> = ConcurrentHashMap.newKeySet()
        val ANIMATED_EMOTE_ID = Regex("/emoticons/v2/([^/]+)/animated/")

        fun isNotFound(result: ImageResult): Boolean {
            val error = (result as? ErrorResult)?.throwable ?: return false
            return generateSequence(error) { it.cause }.any { throwable ->
                throwable.message?.contains("404") == true
            }
        }
    }
}
