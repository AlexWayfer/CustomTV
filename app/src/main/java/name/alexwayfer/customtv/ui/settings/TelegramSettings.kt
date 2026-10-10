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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import name.alexwayfer.customtv.BuildConfig
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.account.LogOutConfirmDialog
import name.alexwayfer.customtv.ui.components.rememberLastNonNull
import name.alexwayfer.customtv.telegram.TelegramGroupState
import name.alexwayfer.customtv.telegram.TelegramLoginError
import name.alexwayfer.customtv.telegram.TelegramLoginStep
import name.alexwayfer.customtv.telegram.TelegramRequest
import name.alexwayfer.customtv.telegram.TelegramSession
import name.alexwayfer.customtv.telegram.TelegramUiState
import name.alexwayfer.customtv.telegram.telegramEditionGroupLink
import name.alexwayfer.customtv.telegram.telegramErrorUnderField
import name.alexwayfer.customtv.telegram.telegramLoginError
import name.alexwayfer.customtv.telegram.telegramPhoneNumber
import name.alexwayfer.customtv.ui.asString
import name.alexwayfer.customtv.ui.rememberHeldWhileLoading
import name.alexwayfer.customtv.ui.theme.TwitchBg
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary
import name.alexwayfer.customtv.ui.watch.OneShotRequest
import name.alexwayfer.customtv.update.AppUpdateDownload

@Composable
internal fun TelegramSettings() {
    SettingsSection(stringResource(R.string.settings_section_updates))
    val ui by TelegramSession.ui.collectAsStateWithLifecycle()
    AnimatedVisibility(
        visible = telegramUpdatesHintVisible(ui.step),
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Hint(stringResource(R.string.telegram_updates))
    }
    if (BuildConfig.TELEGRAM_API_HASH.isBlank()) {
        Hint(stringResource(R.string.telegram_missing_api))
        return
    }
    var confirmLogOut by remember { mutableStateOf(false) }
    TelegramLogin { ui ->
        ui.accountLabel?.let { label ->
            val loggedIn = stringResource(R.string.telegram_account, label)
            val nameStart = loggedIn.indexOf(label)
            Text(
                text = buildAnnotatedString {
                    append(loggedIn)
                    if (nameStart >= 0) {
                        addStyle(
                            SpanStyle(fontWeight = FontWeight.Bold),
                            nameStart,
                            nameStart + label.length,
                        )
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        // Without a background, so the account action does not draw the eye from the updates below.
        TextButton(
            onClick = { confirmLogOut = true },
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            Text(stringResource(R.string.log_out))
        }
        TelegramGroup(if (BuildConfig.PREMIUM) ui.premiumGroup else ui.openGroup, ui.download)
    }
    if (confirmLogOut) {
        LogOutConfirmDialog(
            onConfirm = {
                confirmLogOut = false
                TelegramSession.logOut()
            },
            onDismiss = { confirmLogOut = false },
        )
    }
}

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
                TelegramSession.submitPhone(phone)
            },
            onDismiss = {
                confirmPhone = null
                phoneFocus = OneShotRequest(Unit)
            },
        )
    }
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
                TelegramLoginStep.Phone -> CredentialField(
                    label = stringResource(R.string.telegram_phone),
                    keyboardType = KeyboardType.Phone,
                    busy = ui.busy,
                    submitting = ui.request == TelegramRequest.Submit,
                    error = fieldError,
                    onSubmit = askToConfirm,
                    focusRequest = phoneFocus,
                    centered = centered,
                )
                TelegramLoginStep.Confirm -> CredentialField(
                    label = stringResource(R.string.telegram_phone),
                    keyboardType = KeyboardType.Phone,
                    busy = ui.busy,
                    submitting = ui.request == TelegramRequest.Submit,
                    error = fieldError,
                    onSubmit = askToConfirm,
                    focusRequest = phoneFocus,
                    centered = centered,
                    showStartOver = true,
                )
                TelegramLoginStep.Code -> {
                    TelegramCodeHint(ui.code, ui.busy, resending = ui.request == TelegramRequest.Resend)
                    CredentialField(
                        label = stringResource(R.string.telegram_code),
                        keyboardType = KeyboardType.Number,
                        busy = ui.busy,
                        submitting = ui.request == TelegramRequest.Submit,
                        error = fieldError,
                        onSubmit = TelegramSession::submitCode,
                        centered = centered,
                        showStartOver = true,
                    )
                }
                TelegramLoginStep.Password -> {
                    ui.passwordHint?.let { hint ->
                        Hint(stringResource(R.string.telegram_password_hint, hint))
                    }
                    CredentialField(
                        label = stringResource(R.string.telegram_password),
                        keyboardType = KeyboardType.Password,
                        busy = ui.busy,
                        submitting = ui.request == TelegramRequest.Submit,
                        error = fieldError,
                        onSubmit = TelegramSession::submitPassword,
                        centered = centered,
                        showStartOver = true,
                    )
                }
                TelegramLoginStep.Ready -> ready(ui)
                TelegramLoginStep.LoggingOut -> ProgressStatus(
                    text = stringResource(R.string.telegram_logging_out),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                TelegramLoginStep.Unsupported -> {
                    Hint(
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
        visible = ui.step == TelegramLoginStep.Unsupported,
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
        Button(
            onClick = { onSubmit(value) },
            enabled = !busy && value.isNotBlank(),
        ) {
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

@Composable
private fun TelegramGroup(state: TelegramGroupState, download: AppUpdateDownload) {
    // A quick check keeps its progress for the minimum, so the status and the button do not blink.
    val shown = rememberHeldWhileLoading(state, loading = state is TelegramGroupState.Checking)
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        val currentVersion = if (BuildConfig.PREMIUM) R.string.telegram_current_version_premium else R.string.telegram_current_version_free
        GroupStatus(stringResource(currentVersion, BuildConfig.VERSION_NAME))
        // The check fades into its result, and back into progress on the next check.
        AnimatedContent(
            targetState = shown,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            contentKey = { it.javaClass },
            label = "telegramGroupStatus",
        ) { status ->
            when (status) {
                TelegramGroupState.Checking -> ProgressStatus(
                    text = stringResource(R.string.telegram_group_checking),
                    modifier = Modifier.padding(top = 4.dp),
                )
                TelegramGroupState.Unavailable -> GroupStatus(stringResource(R.string.telegram_group_unavailable))
                TelegramGroupState.NotConfigured -> GroupStatus(stringResource(R.string.telegram_group_not_configured))
                TelegramGroupState.TopicNotConfigured -> GroupStatus(stringResource(R.string.telegram_topic_not_configured))
                is TelegramGroupState.Failed -> GroupStatus(status.message.asString())
                is TelegramGroupState.Open -> {
                    val update = status.update
                    if (update == null) {
                        GroupStatus(stringResource(R.string.telegram_no_new_versions))
                    } else {
                        AppUpdateBlock(update, download)
                    }
                }
            }
        }
        val context = LocalContext.current
        val groupLink = telegramEditionGroupLink(
            premium = BuildConfig.PREMIUM,
            openChatId = BuildConfig.TELEGRAM_OPEN_CHAT_ID,
            openTopicId = BuildConfig.TELEGRAM_OPEN_TOPIC_ID,
            premiumChatId = BuildConfig.TELEGRAM_PREMIUM_CHAT_ID,
            premiumTopicId = BuildConfig.TELEGRAM_PREMIUM_TOPIC_ID,
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SmallButton(
                text = stringResource(R.string.telegram_check_updates),
                onClick = TelegramSession::checkAgain,
                enabled = shown !is TelegramGroupState.Checking,
            )
            if (groupLink != null) {
                SmallButton(
                    text = stringResource(R.string.telegram_open_group),
                    onClick = { openLink(context, groupLink) },
                    colors = secondaryButtonColors(),
                )
            }
        }
    }
}

@Composable
internal fun secondaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = TwitchTextSecondary,
    contentColor = TwitchBg,
)

@Composable
internal fun SmallButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
internal fun GroupStatus(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 4.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

/** A status that lasts a while, such as a check or a log out: a spinner and one announcement. */
@Composable
internal fun ProgressStatus(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}
