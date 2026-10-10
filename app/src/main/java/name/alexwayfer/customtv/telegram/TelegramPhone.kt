package name.alexwayfer.customtv.telegram

/**
 * The number sent to Telegram: its digits after a plus, since Telegram expects the country code first.
 * Spaces, dashes, and brackets the user typed go. Null when there is no digit to send.
 */
internal fun telegramPhoneNumber(raw: String): String? {
    val digits = raw.filter { it in '0'..'9' }
    return if (digits.isEmpty()) null else "+$digits"
}
