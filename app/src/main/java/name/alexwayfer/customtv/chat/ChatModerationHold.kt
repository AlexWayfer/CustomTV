package name.alexwayfer.customtv.chat

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** A message only moderators see until one of them allows or denies it. */
data class ChatModerationHold(
    /** The Twitch message ID a decision is sent for. The row's own ID differs from it. */
    val messageId: String,
    val reason: ModerationHoldReason,
    /** The AutoMod category, such as `swearing`, when AutoMod held the message. */
    val category: String? = null,
    val status: ModerationHoldStatus = ModerationHoldStatus.Pending,
    val resolvedBy: String? = null,
    /** The words AutoMod or a blocked term caught, shown highlighted in the message. */
    val flaggedTerms: List<String> = emptyList(),
)

enum class ModerationHoldReason {
    AutoMod,
    BlockedTerm,
}

enum class ModerationHoldStatus {
    Pending,
    Approved,
    Denied,
    Expired,
}

/** How Twitch treats a suspicious chatter in the open channel. */
enum class LowTrustStatus {
    /** Their messages reach chat, marked for moderators. */
    Monitored,

    /** Only moderators see their messages. */
    Restricted,
}

/** Suspicious chatters of the open live channel by Twitch user ID. Empty unless the user moderates it. */
object SuspiciousChatters {
    private val _byUserId = MutableStateFlow<Map<String, LowTrustStatus>>(emptyMap())
    val byUserId: StateFlow<Map<String, LowTrustStatus>> = _byUserId

    fun set(userId: String, status: LowTrustStatus?) {
        _byUserId.update { current -> suspiciousChattersAfter(current, userId, status) }
    }

    fun clear() {
        _byUserId.value = emptyMap()
    }
}

internal fun suspiciousChattersAfter(
    current: Map<String, LowTrustStatus>,
    userId: String,
    status: LowTrustStatus?,
): Map<String, LowTrustStatus> = when {
    userId.isBlank() -> current
    status == null -> if (userId in current) current - userId else current
    current[userId] == status -> current
    else -> current + (userId to status)
}

/** Where each flagged term appears in [text] at or after [from], longest first where two overlap. */
internal fun flaggedTermRanges(text: String, terms: List<String>, from: Int = 0): List<IntRange> {
    val ranges = mutableListOf<IntRange>()
    terms.filter { it.isNotBlank() }.sortedByDescending { it.length }.forEach { term ->
        var index = text.indexOf(term, from.coerceAtLeast(0), ignoreCase = true)
        while (index >= 0) {
            val range = index until index + term.length
            if (ranges.none { it.first <= range.last && range.first <= it.last }) ranges += range
            index = text.indexOf(term, index + term.length, ignoreCase = true)
        }
    }
    return ranges.sortedBy { it.first }
}
