package name.alexwayfer.customtv.diagnostics

/** What a report holds that its sender may consider personal, listed before it goes out. */
internal sealed interface DiagnosticPrivateItem {
    data class TwitchAccount(val name: String) : DiagnosticPrivateItem
    data class TelegramAccount(val username: String) : DiagnosticPrivateItem

    /** The channels and recordings the log names, with the times. */
    data object Viewing : DiagnosticPrivateItem

    /** The phone model, the languages, and the SIM country from the report's header. */
    data object Device : DiagnosticPrivateItem
}

/** The accounts the report names, by the name the user knows them by, then what every report holds. */
internal fun diagnosticPrivateItems(accounts: DiagnosticAccounts): List<DiagnosticPrivateItem> = listOfNotNull(
    (accounts.twitchLogin?.takeIf { it.isNotBlank() } ?: accounts.twitchUserId?.takeIf { it.isNotBlank() })
        ?.let(DiagnosticPrivateItem::TwitchAccount),
    accounts.telegramUsername?.takeIf { it.isNotBlank() }?.let(DiagnosticPrivateItem::TelegramAccount),
    DiagnosticPrivateItem.Viewing,
    DiagnosticPrivateItem.Device,
)
