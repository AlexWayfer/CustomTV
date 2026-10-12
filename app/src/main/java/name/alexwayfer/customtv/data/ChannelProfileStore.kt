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
            return raw.toJsonArrayOrNull()?.objects()?.mapNotNull(::parseProfile).orEmpty()
        }

        /** A saved profile; one without its login or ID is dropped. */
        fun parseProfile(item: JSONObject): ChannelProfile? {
            val login = item.optString("login").takeIf { it.isNotBlank() } ?: return null
            val id = item.text("id") ?: return null
            return ChannelProfile(
                login = login.lowercase(),
                displayName = item.text("displayName") ?: login,
                avatarUrl = item.text("avatarUrl"),
                id = id,
                highlightColorHex = item.text("highlightColorHex"),
                primaryColorHex = item.text("primaryColorHex"),
                highlightRewardCost = item.intOrNull("highlightRewardCost"),
                channelPointsIconUrl = item.text("channelPointsIconUrl"),
            )
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
