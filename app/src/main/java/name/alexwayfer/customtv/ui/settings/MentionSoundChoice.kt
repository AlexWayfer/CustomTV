package name.alexwayfer.customtv.ui.settings

import android.app.Activity
import android.content.ActivityNotFoundException
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
        val picked = pickedRingtoneUri(result.data)
        val stored = storedMentionSound(picked, defaultUri?.toString().orEmpty())
            ?: return@rememberLauncherForActivityResult
        if (stored.isNotBlank() && result.data.hasReadGrant()) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    stored.toUri(),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }
        val previous = mentionSoundPermissionToRelease(currentUri, stored)
        if (previous != null) {
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    previous.toUri(),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }
        onUriChange(stored)
    }
    var title by remember { mutableStateOf<String?>(null) }
    val defaultText = defaultUri?.toString().orEmpty()
    LaunchedEffect(storedUri) {
        val resolved = mentionSoundUri(storedUri, defaultText)
        title = if (resolved.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    RingtoneManager.getRingtone(context, resolved.toUri())?.getTitle(context)
                }.getOrNull()
            }
        }
    }
    val pickerTitle = stringResource(R.string.mention_sound_choice)
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) {
                val existing = mentionSoundUri(storedUri, defaultText)
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                    .putExtra(
                        RingtoneManager.EXTRA_RINGTONE_TITLE,
                        pickerTitle,
                    )
                if (defaultUri != null) {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, defaultUri)
                }
                if (existing.isNotBlank()) {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing.toUri())
                }
                try {
                    launcher.launch(intent)
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
