package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.CheermoteTier

internal object CheermoteMatcher {
    private val token = Regex("(?<![A-Za-z0-9_])([A-Za-z][A-Za-z0-9]*?)([0-9]+)(?![A-Za-z0-9_])")

    fun apply(message: ChatMessage, cheermotes: Map<String, List<CheermoteTier>>): ChatMessage {
        if (message.cheerBits == null || cheermotes.isEmpty()) return message
        val parts = message.parts.flatMap { part ->
            if (part !is ChatPart.Text) return@flatMap listOf(part)
            val result = mutableListOf<ChatPart>()
            var cursor = 0
            token.findAll(part.text).forEach { match ->
                val tiers = cheermotes[match.groupValues[1].lowercase()] ?: return@forEach
                val amount = match.groupValues[2].toIntOrNull() ?: return@forEach
                val image = tiers.lastOrNull { amount >= it.minBits } ?: return@forEach
                if (match.range.first > cursor) {
                    result += ChatPart.Text(part.text.substring(cursor, match.range.first))
                }
                result += ChatPart.Emote(match.value, image.imageUrl)
                result += ChatPart.Text(amount.toString())
                cursor = match.range.last + 1
            }
            if (cursor < part.text.length) result += ChatPart.Text(part.text.substring(cursor))
            if (result.isEmpty()) listOf(part) else result
        }
        return message.copy(parts = parts)
    }
}
