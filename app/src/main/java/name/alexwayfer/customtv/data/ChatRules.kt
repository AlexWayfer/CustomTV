package name.alexwayfer.customtv.data

import org.json.JSONObject
import java.security.MessageDigest

/**
 * A channel's non-blank chat rules from its GQL `user`, empty when it has none, or null when the
 * answer did not include them.
 */
internal fun parseChatRules(user: JSONObject): List<String>? {
    val settings = user.optJSONObject("chatSettings") ?: return null
    val rules = settings.optJSONArray("rules") ?: return emptyList()
    return (0 until rules.length()).mapNotNull { index ->
        if (rules.isNull(index)) null else rules.optString(index).trim().takeIf { it.isNotEmpty() }
    }
}

/** Stands for these exact rules, so a confirmation stops counting once the channel edits them. */
internal fun chatRulesFingerprint(rules: List<String>): String {
    val digest = MessageDigest.getInstance("SHA-256")
    rules.forEach { rule ->
        digest.update(rule.toByteArray(Charsets.UTF_8))
        digest.update(0)
    }
    return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
}
