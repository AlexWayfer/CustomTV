package name.alexwayfer.customtv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.channelProfilesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "channel_profiles",
)

class ChannelProfileStore(context: Context) {
    private val dataStore = context.applicationContext.channelProfilesDataStore

    suspend fun loadAll(): List<ChannelProfile> {
        val raw = dataStore.data.first()[KEY] ?: return emptyList()
        return parse(raw)
    }

    /** Keyed by channel ID, so a renamed channel replaces its old profile. */
    suspend fun upsert(profile: ChannelProfile) {
        val id = profile.id?.takeIf { it.isNotBlank() } ?: return
        dataStore.edit { prefs ->
            val current = parse(prefs[KEY].orEmpty()).filter { it.id != id }
            prefs[KEY] = serialize((current + profile).takeLast(MAX_PROFILES))
        }
    }

    private companion object {
        val KEY = stringPreferencesKey("profiles")
        const val MAX_PROFILES = 50

        fun parse(raw: String): List<ChannelProfile> {
            if (raw.isBlank()) return emptyList()
            return runCatching {
                val array = JSONArray(raw)
                buildList {
                    for (index in 0 until array.length()) {
                        val item = array.optJSONObject(index) ?: continue
                        val login = item.optString("login").takeIf { it.isNotBlank() } ?: continue
                        val id = item.optString("id").takeIf { it.isNotBlank() && it != "null" } ?: continue
                        add(
                            ChannelProfile(
                                login = login.lowercase(),
                                displayName = item.optString("displayName").takeIf {
                                    it.isNotBlank() && it != "null"
                                } ?: login,
                                avatarUrl = item.optString("avatarUrl").takeIf {
                                    it.isNotBlank() && it != "null"
                                },
                                id = id,
                                highlightColorHex = item.optString("highlightColorHex").takeIf {
                                    it.isNotBlank() && it != "null"
                                },
                                primaryColorHex = item.optString("primaryColorHex").takeIf {
                                    it.isNotBlank() && it != "null"
                                },
                                highlightRewardCost = item.takeIf {
                                    it.has("highlightRewardCost") && !it.isNull("highlightRewardCost")
                                }?.optInt("highlightRewardCost"),
                                channelPointsIconUrl = item.optString("channelPointsIconUrl").takeIf {
                                    it.isNotBlank() && it != "null"
                                },
                            ),
                        )
                    }
                }
            }.getOrDefault(emptyList())
        }

        fun serialize(profiles: List<ChannelProfile>): String {
            val array = JSONArray()
            profiles.forEach { profile ->
                array.put(
                    JSONObject()
                        .put("login", profile.login)
                        .put("displayName", profile.displayName)
                        .put("avatarUrl", profile.avatarUrl ?: JSONObject.NULL)
                        .put("id", profile.id ?: JSONObject.NULL)
                        .put("highlightColorHex", profile.highlightColorHex ?: JSONObject.NULL)
                        .put("primaryColorHex", profile.primaryColorHex ?: JSONObject.NULL)
                        .put(
                            "highlightRewardCost",
                            profile.highlightRewardCost ?: JSONObject.NULL,
                        )
                        .put(
                            "channelPointsIconUrl",
                            profile.channelPointsIconUrl ?: JSONObject.NULL,
                        ),
                )
            }
            return array.toString()
        }
    }
}
