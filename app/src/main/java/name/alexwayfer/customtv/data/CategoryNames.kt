package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

private const val CATEGORY_NAMES_TAG = "CategoryNames"

/** A category's ID is a number; anything else is never sent. */
private val categoryIdPattern = Regex("\\d+")

/**
 * One GQL payload asking the [selection] of each category of [ids], one alias and one variable each, such as
 * `c0:game(id:$c0){displayName}`; the answer has each under its alias, `c0` and on.
 */
internal fun categoryAliasPayload(ids: List<String>, selection: String): JSONObject {
    val variables = JSONObject()
    val declarations = ids.indices.joinToString(",") { index -> $$"$c$$index:ID!" }
    val fields = ids.indices.joinToString(" ") { index -> $$"c$$index:game(id:$c$$index)$$selection" }
    ids.forEachIndexed { index, id -> variables.put("c$index", id) }
    return JSONObject()
        .put("query", "query($declarations){$fields}")
        .put("variables", variables)
}

/** One GQL payload asking the display names of [ids]. */
internal fun categoryNamesPayload(ids: List<String>): JSONObject = categoryAliasPayload(ids, "{displayName}")

/** The display names of [ids] by ID from a [categoryNamesPayload] answer; a category gone or unnamed is left out. */
internal fun parseCategoryNames(body: String, ids: List<String>): Map<String, String> {
    val data = body.toJsonObjectOrNull()?.optJSONObject("data") ?: return emptyMap()
    return ids.mapIndexedNotNull { index, id ->
        data.optJSONObject("c$index")?.trimmedText("displayName")?.let { id to it }
    }.toMap()
}

/** Only the IDs worth a request: numbers, each once. */
internal fun categoryIdsToAsk(ids: Collection<String>): List<String> =
    ids.map(String::trim).filter { categoryIdPattern.matches(it) }.distinct()

/**
 * Category names in the app's language by category ID, for the lists that come from Helix or EventSub, which name
 * a category only in English. Each name is asked once per language and kept for the process.
 */
internal object CategoryNames {
    private val known = ConcurrentHashMap<String, String>()

    private fun key(language: String, id: String) = "$language:$id"

    /**
     * The names of [ids] in the app's language: the known ones at once, the rest in one GQL request per batch. A
     * name Twitch did not answer is left out, so the caller keeps its English one. Blocks, so call it off the main
     * thread.
     */
    fun localized(ids: Collection<String>): Map<String, String> {
        val language = TwitchLanguage.current()
        val asked = categoryIdsToAsk(ids)
        val missing = asked.filter { known[key(language, it)] == null }
        gqlAliasBatches(missing).forEach { batch -> fetch(batch, language) }
        return asked.mapNotNull { id -> known[key(language, id)]?.let { id to it } }.toMap()
    }

    private fun fetch(ids: List<String>, language: String) {
        val request = twitchGqlRequest(categoryNamesPayload(ids).toString(), language = language).build()
        try {
            sharedHttpClient.newCall(request).execute().use { response ->
                val text = response.body.string()
                Diagnostics.reportGql(CATEGORY_NAMES_TAG, "CategoryNames", response.code, text)
                if (response.code !in HTTP_SUCCESS) {
                    AppLog.w(CATEGORY_NAMES_TAG, "names failed HTTP ${response.code}")
                    return
                }
                parseCategoryNames(text, ids).forEach { (id, name) -> known[key(language, id)] = name }
            }
        } catch (error: IOException) {
            AppLog.w(CATEGORY_NAMES_TAG, "names failed: ${error.javaClass.simpleName}")
        }
    }
}
