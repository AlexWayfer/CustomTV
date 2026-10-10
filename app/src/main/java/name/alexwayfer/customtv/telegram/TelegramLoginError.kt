package name.alexwayfer.customtv.telegram

/** A Telegram login rejection the settings screen explains in words instead of TDLib's code. */
internal enum class TelegramLoginError {
    WrongPassword,
    WrongCode,
    CodeExpired,
    InvalidPhone,
    BannedPhone,
    TooManyAttempts,
}

/** The known rejection behind a TDLib error message, or null when the message is shown as is. */
internal fun telegramLoginError(message: String): TelegramLoginError? {
    val code = message.trim()
    return when {
        code == "PASSWORD_HASH_INVALID" -> TelegramLoginError.WrongPassword
        code == "PHONE_CODE_INVALID" -> TelegramLoginError.WrongCode
        code == "PHONE_CODE_EXPIRED" -> TelegramLoginError.CodeExpired
        code == "PHONE_NUMBER_INVALID" -> TelegramLoginError.InvalidPhone
        code == "PHONE_NUMBER_BANNED" -> TelegramLoginError.BannedPhone
        code == "PHONE_NUMBER_FLOOD" || code.startsWith("Too Many Requests") -> TelegramLoginError.TooManyAttempts
        else -> null
    }
}

/** A step with a field shows the rejection under that field; any other step shows it below the section. */
internal fun telegramErrorUnderField(step: TelegramLoginStep): Boolean = when (step) {
    TelegramLoginStep.Phone,
    TelegramLoginStep.Confirm,
    TelegramLoginStep.Code,
    TelegramLoginStep.Password,
    -> true
    TelegramLoginStep.Starting,
    TelegramLoginStep.Ready,
    TelegramLoginStep.LoggingOut,
    TelegramLoginStep.Unsupported,
    -> false
}
