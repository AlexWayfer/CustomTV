package name.alexwayfer.customtv.ui.settings

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.Button
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.components.rememberLastNonNull
import name.alexwayfer.customtv.data.ChatterLabelsCredentials
import name.alexwayfer.customtv.data.ChatterLabelsRepository
import name.alexwayfer.customtv.data.chatterLabelsErrorText
import name.alexwayfer.customtv.ui.UiText
import name.alexwayfer.customtv.ui.asString
import org.json.JSONException
import java.io.IOException
import java.security.GeneralSecurityException

internal val ChatterLabelsSettingsTitle = R.string.chatter_labels

/** The row in the settings that opens the chatter labels page. */
@Composable
internal fun ChatterLabelsSettingsEntry(onOpen: () -> Unit) {
    SettingsPageEntry(
        icon = painterResource(R.drawable.ic_label),
        title = stringResource(R.string.chatter_labels),
        hint = stringResource(R.string.chatter_labels_hint),
        onOpen = onOpen,
    )
}

/** The Gist connection and, once connected, its labels, with Save floating at the bottom while there are changes. */
@Composable
internal fun ChatterLabelsSettings(seed: ChatterLabelFieldSeed, leaveGuard: SettingsLeaveGuard, modifier: Modifier) {
    val saved by ChatterLabelsRepository.credentials.collectAsStateWithLifecycle()
    val editor = remember { ChatterLabelsEditorState() }
    val saveShown = saved != null && editor.ready && (editor.changed || editor.saving)
    // The keyboard lifts Save with the page's end.
    Box(modifier = modifier.fillMaxSize().imePadding()) {
        SettingsPageColumn {
            ChatterLabelCredentialFields(saved = saved, seed = seed)
            val shownCredentials = rememberLastNonNull(saved)
            AnimatedVisibility(
                visible = saved != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    shownCredentials?.let { ChatterLabelsEditor(it, editor, leaveGuard) }
                }
            }
            // Room under the last label, so Save never covers it.
            AnimatedVisibility(visible = saveShown, enter = expandVertically(), exit = shrinkVertically()) {
                Spacer(Modifier.height(SaveButtonClearance))
            }
        }
        ChatterLabelsSaveButton(
            state = editor,
            visible = saveShown,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}

/** The height Save takes with its margins at the bottom of the page. */
private val SaveButtonClearance = 88.dp

@Composable
private fun ChatterLabelCredentialFields(
    saved: ChatterLabelsCredentials?,
    seed: ChatterLabelFieldSeed,
) {
    val scope = rememberCoroutineScope()
    val token = remember { TextFieldState(seed.token) }
    val gistId = remember { TextFieldState(seed.gistId) }
    var checking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<UiText?>(null) }
    var savedSuccessfully by remember { mutableStateOf(false) }
    // An edit to either field clears the last check's result; moving the cursor does not.
    LaunchedEffect(token, gistId) {
        snapshotFlow { token.text.toString() to gistId.text.toString() }.collect {
            error = null
            savedSuccessfully = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        ChatterLabelGistFields(token, gistId, enabled = !checking)
        ChatterLabelCheckResult(error, savedSuccessfully)
        CheckAndSaveButton(
            checking = checking,
            enabled = !checking && token.text.isNotBlank() && gistId.text.isNotBlank(),
            onClick = {
                checking = true
                error = null
                scope.launch {
                    try {
                        val failure = checkAndSaveChatterLabels(token.text.toString(), gistId.text.toString())
                        if (failure == null) savedSuccessfully = true else error = failure
                    } finally {
                        checking = false
                    }
                }
            },
        )
        AnimatedVisibility(
            visible = saved != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            TextButton(
                onClick = {
                    scope.launch {
                        ChatterLabelsRepository.disconnect()
                        token.clearText()
                        gistId.clearText()
                        error = null
                        savedSuccessfully = false
                    }
                },
                enabled = !checking,
            ) {
                Text(stringResource(R.string.chatter_labels_disconnect))
            }
        }
    }
}

/** The intro, the token and Gist ID fields, and how to get each. */
@Composable
private fun ChatterLabelGistFields(token: TextFieldState, gistId: TextFieldState, enabled: Boolean) {
    Text(
        text = textWithLink(R.string.chatter_labels_intro, R.string.chatter_labels_extension_name, EXTENSION_URL),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
    OutlinedSecureTextField(
        state = token,
        label = { Text(stringResource(R.string.chatter_labels_token)) },
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        enabled = enabled,
        textObfuscationMode = TextObfuscationMode.Hidden,
    )
    FieldHint(textWithLink(R.string.chatter_labels_token_hint, R.string.chatter_labels_create_token, NEW_TOKEN_URL))
    OutlinedTextField(
        state = gistId,
        label = { Text(stringResource(R.string.chatter_labels_gist_id)) },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        enabled = enabled,
        lineLimits = TextFieldLineLimits.SingleLine,
    )
    FieldHint(AnnotatedString(stringResource(R.string.chatter_labels_gist_id_hint)))
}

/** Why the last check failed, or that it saved the connection. */
@Composable
private fun ChatterLabelCheckResult(error: UiText?, savedSuccessfully: Boolean) {
    val shownError = rememberLastNonNull(error)
    AnimatedVisibility(
        visible = error != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Text(
            text = shownError?.asString().orEmpty(),
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    AnimatedVisibility(
        visible = savedSuccessfully,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Text(
            text = stringResource(R.string.chatter_labels_saved),
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun CheckAndSaveButton(checking: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        // The button widens smoothly for the spinner and narrows back once the check is done.
        modifier = Modifier.padding(top = 12.dp).animateContentSize(),
        enabled = enabled,
    ) {
        if (checking) {
            CircularProgressIndicator(
                modifier = Modifier.padding(end = 8.dp).size(16.dp),
                strokeWidth = 2.dp,
            )
        }
        Text(stringResource(R.string.chatter_labels_check_save))
    }
}

/** Checks the token and the Gist and saves them; the reason when that failed, or null once saved. */
private suspend fun checkAndSaveChatterLabels(token: String, gistId: String): UiText? = try {
    ChatterLabelsRepository.checkAndSave(token, gistId)
    null
} catch (failure: IOException) {
    checkFailedText(failure)
} catch (failure: JSONException) {
    checkFailedText(failure)
} catch (failure: GeneralSecurityException) {
    // The Keystore refused to encrypt the saved token.
    checkFailedText(failure)
}

/** A line under a field, placed and styled as the field's own supporting text. */
private fun checkFailedText(failure: Exception): UiText =
    failure.chatterLabelsErrorText(UiText.Resource(R.string.chatter_labels_error_check_failed))

@Composable
private fun FieldHint(text: AnnotatedString) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
}

/** The [template] string with its one argument shown as [linkText], a link to [url]. */
@Composable
private fun textWithLink(@StringRes template: Int, @StringRes linkText: Int, url: String): AnnotatedString {
    val text = stringResource(template, LINK_MARK)
    val link = stringResource(linkText)
    val linkColor = MaterialTheme.colorScheme.primary
    return remember(text, link, url, linkColor) {
        val at = text.indexOf(LINK_MARK)
        buildAnnotatedString {
            if (at < 0) {
                append(text)
                return@buildAnnotatedString
            }
            append(text, 0, at)
            val style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
            withLink(LinkAnnotation.Url(url, TextLinkStyles(style = style))) { append(link) }
            append(text, at + LINK_MARK.length, text.length)
        }
    }
}

/** Stands in for the link while the string is formatted, so the link is placed wherever the language puts it. */
private const val LINK_MARK = "￼"
private const val EXTENSION_URL = "https://chromewebstore.google.com/detail/ngobainhmihgaihpbnocjaemgffoiomp"
private const val NEW_TOKEN_URL = "https://github.com/settings/personal-access-tokens/new"
