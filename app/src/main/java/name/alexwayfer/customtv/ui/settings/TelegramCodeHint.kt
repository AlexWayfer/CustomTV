package name.alexwayfer.customtv.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.telegram.TelegramCodeDelivery
import name.alexwayfer.customtv.telegram.TelegramCodeInfo
import name.alexwayfer.customtv.telegram.TelegramSession

/** Where the login code went, and Send again once Telegram allows it the next way. */
@Composable
internal fun TelegramCodeHint(code: TelegramCodeInfo?, busy: Boolean, resending: Boolean) {
    val sentTo = code?.sentTo
    val resendTo = code?.resendTo
    Column {
        if (sentTo != null) {
            Text(
                text = stringResource(sentTextOf(sentTo)),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        // Telegram names how long to wait before it sends the code again.
        var resendReady by remember(code) { mutableStateOf(false) }
        LaunchedEffect(code) {
            if (code?.resendTo == null) return@LaunchedEffect
            delay(code.resendAfterSeconds.seconds)
            resendReady = true
        }
        AnimatedVisibility(
            visible = resendReady && resendTo != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            TextButton(
                onClick = TelegramSession::resendCode,
                modifier = Modifier.padding(horizontal = 4.dp),
                enabled = !busy,
            ) {
                ProgressButtonLabel(
                    text = stringResource(resendTextOf(resendTo ?: TelegramCodeDelivery.Sms)),
                    inProgress = resending,
                )
            }
        }
    }
}

private fun sentTextOf(delivery: TelegramCodeDelivery): Int = when (delivery) {
    TelegramCodeDelivery.TelegramApp -> R.string.telegram_code_sent_app
    TelegramCodeDelivery.Sms -> R.string.telegram_code_sent_sms
    TelegramCodeDelivery.Call -> R.string.telegram_code_sent_call
    TelegramCodeDelivery.Fragment -> R.string.telegram_code_sent_fragment
}

private fun resendTextOf(delivery: TelegramCodeDelivery): Int = when (delivery) {
    TelegramCodeDelivery.TelegramApp -> R.string.telegram_code_resend_app
    TelegramCodeDelivery.Sms -> R.string.telegram_code_resend_sms
    TelegramCodeDelivery.Call -> R.string.telegram_code_resend_call
    TelegramCodeDelivery.Fragment -> R.string.telegram_code_resend_fragment
}
