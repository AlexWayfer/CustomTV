package name.alexwayfer.customtv.ui.watch

internal enum class ChatTimeoutAccess {
    Editable,
    Countdown,
    Retry;

    fun canSubmit(explicitRetry: Boolean): Boolean = this == Editable || (this == Retry && explicitRetry)
}

internal fun chatTimeoutAccess(seconds: Long, unresolved: Boolean): ChatTimeoutAccess = when {
    seconds > 0L -> ChatTimeoutAccess.Countdown
    unresolved -> ChatTimeoutAccess.Retry
    else -> ChatTimeoutAccess.Editable
}
