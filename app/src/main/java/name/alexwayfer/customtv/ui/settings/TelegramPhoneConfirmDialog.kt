package name.alexwayfer.customtv.ui.settings

import android.telephony.PhoneNumberUtils
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import name.alexwayfer.customtv.R

/** Shows the number in large, grouped digits before Telegram sends a code to it; No goes back to the field. */
@Composable
internal fun TelegramPhoneConfirmDialog(
    phone: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val country = LocalConfiguration.current.locales[0].country
    // Android groups the digits by the country code: +7 916 123-45-67; an unknown number stays as typed.
    val shown = PhoneNumberUtils.formatNumber(phone, country) ?: phone
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.telegram_phone_confirm_title)) },
        text = {
            Text(
                text = shown,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.telegram_phone_confirm_yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.telegram_phone_confirm_no))
            }
        },
    )
}
