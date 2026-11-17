package name.alexwayfer.customtv.data

internal object ChatBadgeRepository {
    private val loader = ChatBadgeLoader()

    fun titleForImage(url: String): String? = loader.titleForImage(url)

    suspend fun keepLoaded(login: String, publish: (ChannelBadgeUrls) -> Unit) = loader.keepLoaded(login, publish)

    suspend fun restore(login: String): ChannelBadgeUrls? = loader.restore(login)
}
