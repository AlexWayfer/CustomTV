package name.alexwayfer.customtv.chat

import kotlin.math.abs

object RewardMessageDeduper {
    fun apply(
        current: List<ChatMessage>,
        incoming: ChatMessage,
        maxMessages: Int,
        pending: List<ChatMessage> = emptyList(),
        windowMs: Long = WINDOW_MS,
    ): Result {
        // The chat history loaded on joining can already hold a message that then arrives live: one id is one row.
        if (current.any { it.id == incoming.id }) {
            return Result(current, expire(pending, incoming.timestampMillis, windowMs), colorSource = null)
        }
        if (incoming.eventKind != ChatEventKind.Reward || incoming.reward == null) {
            return Result(
                messages = (current + incoming).takeLast(maxMessages),
                pending = expire(pending, incoming.timestampMillis, windowMs),
                colorSource = incoming,
            )
        }
        val livePending = expire(pending, incoming.timestampMillis, windowMs)
        if (isPubSub(incoming) && incoming.rawText.isNotBlank()) {
            return applyPubSubWithText(current, incoming, livePending, windowMs)
        }
        val withMeta = if (!isPubSub(incoming)) {
            attachPending(incoming, livePending, windowMs)
        } else {
            incoming to livePending
        }
        val message = withMeta.first
        val nextPending = withMeta.second
        val index = indexOfDuplicate(message, current, windowMs)
        if (index < 0) {
            return Result(
                messages = (current + message).takeLast(maxMessages),
                pending = nextPending,
                colorSource = message.takeUnless { isPubSub(it) },
            )
        }
        val merged = mergePreferringIrc(current[index], message)
        if (merged == current[index]) {
            return Result(current, nextPending, colorSource = null)
        }
        return Result(
            messages = current.toMutableList().apply { this[index] = merged },
            pending = nextPending,
            colorSource = merged.takeUnless { isPubSub(it) },
        )
    }

    internal fun isPubSub(message: ChatMessage): Boolean {
        return message.id.startsWith(PUBSUB_ID_PREFIX)
    }

    internal fun mergeReward(preferred: ChatReward?, other: ChatReward?): ChatReward? {
        if (preferred == null) return other
        if (other == null) return preferred
        return preferred.copy(
            title = preferred.title.ifBlank { other.title },
            cost = if (preferred.cost > 0) preferred.cost else other.cost,
            backgroundColorHex = preferred.backgroundColorHex ?: other.backgroundColorHex,
            imageUrl = preferred.imageUrl ?: other.imageUrl,
            prompt = preferred.prompt ?: other.prompt,
        )
    }

    private fun applyPubSubWithText(
        current: List<ChatMessage>,
        incoming: ChatMessage,
        pending: List<ChatMessage>,
        windowMs: Long,
    ): Result {
        val index = indexOfDuplicate(incoming, current, windowMs)
        if (index >= 0) {
            val merged = mergePreferringIrc(current[index], incoming)
            val messages = if (merged == current[index]) {
                current
            } else {
                current.toMutableList().apply { this[index] = merged }
            }
            return Result(
                messages = messages,
                pending = pending,
                colorSource = merged.takeUnless { isPubSub(it) },
            )
        }
        return Result(
            messages = current,
            pending = (pending + incoming).takeLast(PENDING_LIMIT),
            colorSource = null,
        )
    }

    private fun attachPending(
        incoming: ChatMessage,
        pending: List<ChatMessage>,
        windowMs: Long,
    ): Pair<ChatMessage, List<ChatMessage>> {
        val rewardId = incoming.reward?.id ?: return incoming to pending
        val index = pending.indexOfLast { candidate ->
            isSameRedemption(candidate, incoming, rewardId, windowMs)
        }
        if (index < 0) return incoming to pending
        val stored = pending[index]
        val merged = incoming.copy(reward = mergeReward(incoming.reward, stored.reward))
        return merged to pending.filterIndexed { i, _ -> i != index }
    }

    internal fun indexOfDuplicate(
        incoming: ChatMessage,
        current: List<ChatMessage>,
        windowMs: Long = WINDOW_MS,
    ): Int {
        val rewardId = incoming.reward?.id ?: return -1
        val start = (current.size - LOOKBACK).coerceAtLeast(0)
        for (index in current.lastIndex downTo start) {
            val existing = current[index]
            if (!isSameRedemption(existing, incoming, rewardId, windowMs)) continue
            return index
        }
        return -1
    }

    private fun mergePreferringIrc(existing: ChatMessage, incoming: ChatMessage): ChatMessage {
        val irc = when {
            !isPubSub(existing) -> existing
            !isPubSub(incoming) -> incoming
            else -> existing
        }
        val other = if (irc === existing) incoming else existing
        return irc.copy(
            badges = irc.badges.ifEmpty { other.badges },
            reward = mergeReward(irc.reward, other.reward),
        )
    }

    private fun isSameRedemption(
        existing: ChatMessage,
        incoming: ChatMessage,
        rewardId: String,
        windowMs: Long,
    ): Boolean {
        if (existing.eventKind != ChatEventKind.Reward) return false
        if (!existing.userLogin.equals(incoming.userLogin, ignoreCase = true)) return false
        if (!existing.reward?.id.equals(rewardId, ignoreCase = true)) return false
        if (!sameText(existing.rawText, incoming.rawText)) return false
        return abs(existing.timestampMillis - incoming.timestampMillis) < windowMs
    }

    private fun sameText(left: String, right: String): Boolean {
        return left.trim() == right.trim()
    }

    private fun expire(
        pending: List<ChatMessage>,
        now: Long,
        windowMs: Long,
    ): List<ChatMessage> {
        return pending.filter { abs(it.timestampMillis - now) < windowMs }
    }

    data class Result(
        val messages: List<ChatMessage>,
        val pending: List<ChatMessage> = emptyList(),
        val colorSource: ChatMessage?,
    )

    const val WINDOW_MS = 30_000L
    private const val LOOKBACK = 20
    private const val PENDING_LIMIT = 20
    private const val PUBSUB_ID_PREFIX = "reward-"
}
