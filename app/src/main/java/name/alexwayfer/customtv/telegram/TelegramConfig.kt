package name.alexwayfer.customtv.telegram

/** The Telegram app this build logs in as, and the groups its releases are posted in. */
internal data class TelegramConfig(
    val apiId: Int = 0,
    val apiHash: String = "",
    val openChatId: Long = 0L,
    val openTopicId: Int = 0,
    val premiumChatId: Long = 0L,
    val premiumTopicId: Int = 0,
    val premiumEdition: Boolean = false,
)

/** The state with [group] as the release group of the edition this build is. */
internal fun TelegramUiState.withEditionGroup(premiumEdition: Boolean, group: TelegramGroupState): TelegramUiState =
    if (premiumEdition) {
        copy(premiumGroup = group)
    } else {
        copy(openGroup = group)
    }

/** The state with the edition's group shown as checking, or as not configured when the build names no chat. */
internal fun TelegramUiState.withEditionChecking(config: TelegramConfig): TelegramUiState =
    if (config.premiumEdition) {
        copy(premiumGroup = groupPlaceholder(config.premiumChatId))
    } else {
        copy(openGroup = groupPlaceholder(config.openChatId))
    }

private fun groupPlaceholder(chatId: Long): TelegramGroupState =
    if (chatId == 0L) TelegramGroupState.NotConfigured else TelegramGroupState.Checking
