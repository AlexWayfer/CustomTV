package name.alexwayfer.customtv.ui.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.diagnostics.AppLog

@Composable
internal fun MentionSoundChoice(
    storedUri: String,
    onUriChange: (String) -> Unit,
) {
    val context = LocalContext.current
    val defaultUri = remember {
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }
    val currentUri by rememberUpdatedState(storedUri)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val stored = keepPickedMentionSound(context, result.data, defaultUri, currentUri)
            ?: return@rememberLauncherForActivityResult
        onUriChange(stored)
    }
    val defaultText = defaultUri?.toString().orEmpty()
    val title by produceState<String?>(null, storedUri) {
        value = mentionSoundTitle(context, mentionSoundUri(storedUri, defaultText))
    }
    val pickerTitle = stringResource(R.string.mention_sound_choice)
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) {
                val existing = mentionSoundUri(storedUri, defaultText)
                try {
                    launcher.launch(ringtonePickerIntent(pickerTitle, defaultUri, existing))
                } catch (_: ActivityNotFoundException) {
                    AppLog.w(TAG, "ringtone picker unavailable")
                }
            },
        headlineContent = { Text(stringResource(R.string.mention_sound_choice)) },
        supportingContent = {
            Column {
                Text(title ?: stringResource(R.string.mention_sound_default))
                Text(stringResource(R.string.mention_sound_choice_hint))
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

/**
 * The sound the picker returned, as the setting stores it, with lasting access taken to it and given back for the
 * [current] one; null when nothing changes.
 */
private fun keepPickedMentionSound(context: Context, data: Intent?, defaultUri: Uri?, current: String): String? {
    val stored = storedMentionSound(pickedRingtoneUri(data), defaultUri?.toString().orEmpty()) ?: return null
    if (stored.isNotBlank() && data.hasReadGrant()) {
        try {
            context.contentResolver.takePersistableUriPermission(
                stored.toUri(),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: SecurityException) {
            // The picker granted no lasting access; the sound plays while the grant lasts.
        }
    }
    val previous = mentionSoundPermissionToRelease(current, stored)
    if (previous != null) {
        try {
            context.contentResolver.releasePersistableUriPermission(
                previous.toUri(),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: SecurityException) {
            // The previous sound held no lasting access to give back.
        }
    }
    return stored
}

/** The sound's name as the system shows it, or null for none or a file the app can no longer read. */
private suspend fun mentionSoundTitle(context: Context, uri: String): String? {
    if (uri.isBlank()) return null
    return withContext(Dispatchers.IO) {
        try {
            RingtoneManager.getRingtone(context, uri.toUri())?.getTitle(context)
        } catch (_: SecurityException) {
            // The sound's file is no longer readable to the app.
            null
        }
    }
}

private fun ringtonePickerIntent(title: String, defaultUri: Uri?, existing: String): Intent {
    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, title)
    if (defaultUri != null) {
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, defaultUri)
    }
    if (existing.isNotBlank()) {
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing.toUri())
    }
    return intent
}

private fun Intent?.hasReadGrant(): Boolean {
    val flags = this?.flags ?: return false
    return (flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0
}

private fun pickedRingtoneUri(data: Intent?): String? {
    val intent = data ?: return null
    return IntentCompat.getParcelableExtra(intent, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        ?.toString()
}

private const val TAG = "MentionSound"
