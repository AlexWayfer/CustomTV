package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color

internal object IrcUserNotice {
    fun eventKind(messageId: String): ChatEventKind = when (messageId) {
        "announcement" -> ChatEventKind.Announcement
        "sub",
        "resub",
        "subgift",
        "submysterygift",
        "anonsubmysterygift",
        "giftpaidupgrade",
        "rewardgift",
        "anongiftpaidupgrade",
        "primepaidupgrade",
        "standardpayforward",
        "communitypayforward",
        -> ChatEventKind.Subscription
        "raid", "unraid" -> ChatEventKind.Raid
        else -> ChatEventKind.System
    }

    fun watchStreak(tags: Map<String, String>): ChatWatchStreak? {
        if (tags["msg-id"] != "viewermilestone") return null
        if (tags["msg-param-category"] != "watch-streak") return null
        val consecutiveStreams = tags["msg-param-value"]?.toIntOrNull()?.takeIf { it > 0 }
            ?: return null
        val points = (tags["msg-param-copoReward"] ?: tags["msg-param-coporeward"])
            ?.toIntOrNull()
            ?.takeIf { it > 0 }
        return ChatWatchStreak(consecutiveStreams = consecutiveStreams, points = points)
    }

    fun raid(tags: Map<String, String>): ChatRaid? = when (tags["msg-id"]) {
        "unraid" -> ChatRaid(
            viewerCount = 0,
            fromDisplayName = tags["display-name"].orEmpty(),
            canceled = true,
        )
        "raid" -> {
            val count = tags["msg-param-viewerCount"]?.toIntOrNull()
                ?: tags["msg-param-viewercount"]?.toIntOrNull()
                ?: 0
            val name = tags["msg-param-displayName"]?.takeIf { it.isNotBlank() }
                ?: tags["msg-param-displayname"]?.takeIf { it.isNotBlank() }
                ?: tags["display-name"]?.takeIf { it.isNotBlank() }
                ?: tags["msg-param-login"]?.takeIf { it.isNotBlank() }
                ?: return null
            ChatRaid(viewerCount = count, fromDisplayName = name)
        }
        else -> null
    }

    fun communityGift(tags: Map<String, String>): ChatCommunityGift? {
        val messageId = tags["msg-id"]
        if (messageId != "submysterygift" && messageId != "anonsubmysterygift") return null
        val count = tags["msg-param-mass-gift-count"]?.toIntOrNull()?.takeIf { it > 0 }
            ?: return null
        val anonymous = messageId == "anonsubmysterygift" || tags["login"] == "ananonymousgifter"
        val displayName = if (anonymous) {
            null
        } else {
            tags["display-name"]?.takeIf { it.isNotBlank() }
                ?: tags["login"]?.takeIf { it.isNotBlank() }
        }
        val tier = when (tags["msg-param-sub-plan"]) {
            "1000" -> 1
            "2000" -> 2
            "3000" -> 3
            else -> null
        }
        return ChatCommunityGift(
            count = count,
            tier = tier,
            gifterDisplayName = displayName,
            cumulativeCount = tags["msg-param-sender-count"]?.toIntOrNull()?.takeIf { it > 0 },
            anonymous = anonymous,
        )
    }

    fun watchStreakSystemText(displayName: String, consecutiveStreams: Int): String {
        val unit = if (consecutiveStreams == 1) "stream" else "streams"
        return "$displayName watched $consecutiveStreams consecutive $unit"
    }

    fun announcementColor(raw: String?): Color = when (raw?.uppercase()) {
        "BLUE" -> Color(0xFF00D6D6)
        "GREEN" -> Color(0xFF00DB84)
        "ORANGE" -> Color(0xFFFFB31A)
        "PURPLE" -> Color(0xFFBF94FF)
        else -> Color(0xFFADADB8)
    }
}
