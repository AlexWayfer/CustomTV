package name.alexwayfer.customtv.data

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ActivityIdentityFileTest {
    @Test
    fun identityIsPersistedOutsideBackupsAndSurvivesNewStoreInstances() {
        val context = RuntimeEnvironment.getApplication()
        val storage = ActivityIdentityFile(context)
        val identity = storage.read()
        assertEquals(4, UUID.fromString(identity.installationId).version())
        assertEquals(identity, ActivityIdentityFile(context).read())
        assertTrue(File(context.noBackupFilesDir, "activity-statistics.json").exists())
        assertFalse(File(context.filesDir, "activity-statistics.json").exists())
    }

    @Test
    fun pendingAndCurrentKeysAndEditionReceiptsSurviveRestart() {
        val context = RuntimeEnvironment.getApplication()
        val storage = ActivityIdentityFile(context)
        val key = ActivitySigningKey("key", "digest", listOf("certificate1", "certificate2"), 123)
        val saved = storage.read().copy(key = key, pending = key.copy(alias = "pending"),
            receipts = mapOf("premium" to ActivityReceipt("2026-11-10", "1.16.1", "Android 16")),
            enrollAfter = 456)
        storage.write(saved)
        assertEquals(saved, ActivityIdentityFile(context).read())
    }

    @Test(expected = IOException::class)
    fun damagedStateDoesNotSilentlyCreateAnotherInstallation() {
        val context = RuntimeEnvironment.getApplication()
        File(context.noBackupFilesDir, "activity-statistics.json").writeText("broken")
        ActivityIdentityFile(context).read()
    }
}
