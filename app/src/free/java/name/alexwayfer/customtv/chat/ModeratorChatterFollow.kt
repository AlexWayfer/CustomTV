package name.alexwayfer.customtv.chat

/** Moderator tools are Premium: the free build knows only the user's own follow. */
@Suppress("unused", "RedundantSuspendModifier", "FunctionOnlyReturningConstant")
internal suspend fun moderatorChatterFollow(broadcasterId: String, userId: String): ChatterFollow? = null

@Suppress("unused", "FunctionOnlyReturningConstant")
internal fun moderatesChannel(broadcasterId: String): Boolean = false
