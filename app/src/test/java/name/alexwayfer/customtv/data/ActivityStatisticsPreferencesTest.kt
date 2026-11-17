package name.alexwayfer.customtv.data

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityStatisticsPreferencesTest {
    @Test
    fun collectionStartsEnabledAndHonoursSavedOptOut() {
        assertTrue(activityStatisticsEnabled(emptyPreferences()))
        assertFalse(activityStatisticsEnabled(mutablePreferencesOf(ACTIVITY_STATISTICS_ENABLED to false)))
        assertTrue(activityStatisticsEnabled(mutablePreferencesOf(ACTIVITY_STATISTICS_ENABLED to true)))
    }
}
