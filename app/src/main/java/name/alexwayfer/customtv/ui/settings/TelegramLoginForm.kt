package name.alexwayfer.customtv.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.telegram.TelegramLoginError
import name.alexwayfer.customtv.telegram.TelegramLoginStep
import name.alexwayfer.customtv.telegram.TelegramRequest
import name.alexwayfer.customtv.telegram.TelegramSession
import name.alexwayfer.customtv.telegram.TelegramUiState
import name.alexwayfer.customtv.telegram.telegramErrorUnderField
import name.alexwayfer.customtv.telegram.telegramLoginError
import name.alexwayfer.customtv.telegram.telegramPhoneNumber
import name.alexwayfer.customtv.ui.components.rememberLastNonNull
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary
import name.alexwayfer.customtv.ui.watch.OneShotRequest

/**
 * The Telegram login steps, shared by the settings and the Premium access screen; [ready] follows the login.
 * [centered] puts the buttons under a field in the middle, as the Premium access screen centers its card.
 */
@Composable
internal fun TelegramLogin(centered: Boolean = false, ready: @Composable (TelegramUiState) -> Unit) {
    val ui by TelegramSession.ui.collectAsStateWithLifecycle()
    val error = ui.notice?.let { telegramErrorText(it) }
    val fieldError = error?.takeIf { telegramErrorUnderField(ui.step) }
    // A mistyped number sends the code to someone else, so the number is confirmed first.
    var confirmPhone by rememberSaveable { mutableStateOf<String?>(null) }
    // No keeps the typed number and puts the cursor back, since one wrong digit is quicker to fix than retype.
    var phoneFocus by remember { mutableStateOf<OneShotRequest<Unit>?>(null) }
    val askToConfirm: (String) -> Unit = { typed -> confirmPhone = telegramPhoneNumber(typed) }
    confirmPhone?.let { phone ->
        TelegramPhoneConfirmDialog(
            phone = phone,
            onConfirm = {
                confirmPhone = null
                TelegramSession.login.submitPhone(phone)
            },
            onDismiss = {
                confirmPhone = null
                phoneFocus = OneShotRequest(Unit)
            },
        )
    }
    val phoneField: @Composable (showStartOver: Boolean) -> Unit = { showStartOver ->
        CredentialField(
            label = stringResource(R.string.telegram_phone),
            keyboardType = KeyboardType.Phone,
            busy = ui.busy,
            submitting = ui.request == TelegramRequest.Submit,
            error = fieldError,
            onSubmit = askToConfirm,
            showStartOver = showStartOver,
            focusRequest = phoneFocus,
            centered = centered,
        )
    }
    TelegramLoginSteps(ui, fieldError, centered, phoneField, ready)
    TelegramLoginFooter(ui.step, error, fieldError)
}

/** The current step's field or status; [phoneField] is the number field, with Start over or without. */
@Composable
private fun TelegramLoginSteps(
    ui: TelegramUiState,
    fieldError: String?,
    centered: Boolean,
    phoneField: @Composable (showStartOver: Boolean) -> Unit,
    ready: @Composable (TelegramUiState) -> Unit,
) {
    // Each step fades into the next, and the section below glides to its new place.
    AnimatedContent(
        targetState = ui.step,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        // Confirming the number shows the same field as typing it, so it does not fade over itself.
        contentKey = { step -> if (step == TelegramLoginStep.Confirm) TelegramLoginStep.Phone else step },
        label = "telegramStep",
    ) { step ->
        Column {
            when (step) {
                // Opening the local session takes a fraction of a second; the field or the account follows.
                TelegramLoginStep.Starting -> Unit
                TelegramLoginStep.Phone -> phoneField(false)
                TelegramLoginStep.Confirm -> phoneField(true)
                TelegramLoginStep.Code -> TelegramCodeStep(ui, fieldError, centered)
                TelegramLoginStep.Password -> TelegramPasswordStep(ui, fieldError, centered)
                TelegramLoginStep.Ready -> ready(ui)
                TelegramLoginStep.LoggingOut -> ProgressStatus(
                    text = stringResource(R.string.telegram_logging_out),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                TelegramLoginStep.Unsupported -> TelegramHint(
                    if (ui.registrationRejected) {
                        stringResource(R.string.telegram_not_registered)
                    } else {
                        stringResource(R.string.telegram_unsupported)
                    },
                )
            }
        }
    }
}

@Composable
private fun TelegramCodeStep(ui: TelegramUiState, fieldError: String?, centered: Boolean) {
    TelegramCodeHint(ui.code, ui.busy, resending = ui.request == TelegramRequest.Resend)
    CredentialField(
        label = stringResource(R.string.telegram_code),
        keyboardType = KeyboardType.Number,
        busy = ui.busy,
        submitting = ui.request == TelegramRequest.Submit,
        error = fieldError,
        onSubmit = TelegramSession.login::submitCode,
        centered = centered,
        showStartOver = true,
    )
}

@Composable
private fun TelegramPasswordStep(ui: TelegramUiState, fieldError: String?, centered: Boolean) {
    ui.passwordHint?.let { hint ->
        TelegramHint(stringResource(R.string.telegram_password_hint, hint))
    }
    CredentialField(
        label = stringResource(R.string.telegram_password),
        keyboardType = KeyboardType.Password,
        busy = ui.busy,
        submitting = ui.request == TelegramRequest.Submit,
        error = fieldError,
        onSubmit = TelegramSession.login::submitPassword,
        centered = centered,
        showStartOver = true,
    )
}

/** An [error] that has no field to sit under, and Start over at a step the login cannot go on from. */
@Composable
private fun TelegramLoginFooter(step: TelegramLoginStep, error: String?, fieldError: String?) {
    val shownError = rememberLastNonNull(error?.takeIf { fieldError == null })
    AnimatedVisibility(
        visible = error != null && fieldError == null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Text(
            text = shownError.orEmpty(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    AnimatedVisibility(
        visible = step == TelegramLoginStep.Unsupported,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        StartOverButton(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}

@Composable
private fun CredentialField(
    label: String,
    keyboardType: KeyboardType,
    busy: Boolean,
    submitting: Boolean,
    error: String?,
    onSubmit: (String) -> Unit,
    showStartOver: Boolean = false,
    focusRequest: OneShotRequest<Unit>? = null,
    centered: Boolean = false,
) {
    val supportingText: (@Composable () -> Unit)? = error?.let { { Text(it) } }
    val state = rememberSaveable(label, saver = TextFieldState.Saver) { TextFieldState() }
    val value = state.text.toString()
    val focus = remember { FocusRequester() }
    LaunchedEffect(focusRequest) {
        focusRequest?.take() ?: return@LaunchedEffect
        focus.requestFocus()
    }
    val fieldModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .focusRequester(focus)
        // Lets the autofill service offer the owner's saved number above the keyboard.
        .then(
            if (keyboardType == KeyboardType.Phone) {
                Modifier.semantics { contentType = ContentType.PhoneNumber }
            } else {
                Modifier
            },
        )
    val keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done)
    val onDone = KeyboardActionHandler { if (!busy) onSubmit(state.text.toString()) }
    if (keyboardType == KeyboardType.Password) {
        OutlinedSecureTextField(
            state = state,
            modifier = fieldModifier,
            label = { Text(label) },
            supportingText = supportingText,
            isError = error != null,
            textObfuscationMode = TextObfuscationMode.Hidden,
            keyboardOptions = keyboardOptions,
            onKeyboardAction = onDone,
        )
    } else {
        OutlinedTextField(
            state = state,
            modifier = fieldModifier,
            label = { Text(label) },
            supportingText = supportingText,
            isError = error != null,
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions = keyboardOptions,
            onKeyboardAction = onDone,
        )
    }
    CredentialButtons(
        onSubmit = { onSubmit(value) },
        enabled = !busy && value.isNotBlank(),
        submitting = submitting,
        showStartOver = showStartOver,
        centered = centered,
    )
}

/** Log in under a field, and Start over beside it after the first step. */
@Composable
private fun CredentialButtons(
    onSubmit: () -> Unit,
    enabled: Boolean,
    submitting: Boolean,
    showStartOver: Boolean,
    centered: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(
            8.dp,
            if (centered) Alignment.CenterHorizontally else Alignment.Start,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(onClick = onSubmit, enabled = enabled) {
            ProgressButtonLabel(stringResource(R.string.log_in), inProgress = submitting)
        }
        if (showStartOver) {
            StartOverButton()
        }
    }
}

@Composable
private fun telegramErrorText(message: String): String = when (telegramLoginError(message)) {
    TelegramLoginError.WrongPassword -> stringResource(R.string.telegram_error_wrong_password)
    TelegramLoginError.WrongCode -> stringResource(R.string.telegram_error_wrong_code)
    TelegramLoginError.CodeExpired -> stringResource(R.string.telegram_error_code_expired)
    TelegramLoginError.InvalidPhone -> stringResource(R.string.telegram_error_invalid_phone)
    TelegramLoginError.BannedPhone -> stringResource(R.string.telegram_error_banned_phone)
    TelegramLoginError.TooManyAttempts -> stringResource(R.string.telegram_error_too_many_attempts)
    null -> message
}

@Composable
private fun StartOverButton(modifier: Modifier = Modifier) {
    TextButton(
        onClick = TelegramSession::startOver,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(contentColor = TwitchTextSecondary),
    ) {
        Text(stringResource(R.string.telegram_start_over))
    }
}
