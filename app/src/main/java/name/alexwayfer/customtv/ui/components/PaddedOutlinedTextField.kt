package name.alexwayfer.customtv.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation

/**
 * A one-line outlined field as Material draws it, with [contentPadding] around its text, which Material's own field
 * for a String does not take: a smaller padding makes the field lower than the standard 56dp.
 */
@Composable
internal fun PaddedOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
) {
    val colors = OutlinedTextFieldDefaults.colors()
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val textColor = when {
        !enabled -> colors.disabledTextColor
        isError -> colors.errorTextColor
        focused -> colors.focusedTextColor
        else -> colors.unfocusedTextColor
    }
    // Room above for the label, which rests on the outline once it shrinks, as Material's field keeps.
    val labelRoom = if (label != null) {
        with(LocalDensity.current) { (MaterialTheme.typography.bodySmall.lineHeight / 2).toDp() }
    } else {
        null
    }
    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier
                .then(if (labelRoom != null) Modifier.padding(top = labelRoom) else Modifier)
                .defaultMinSize(minWidth = OutlinedTextFieldDefaults.MinWidth),
            enabled = enabled,
            textStyle = LocalTextStyle.current.merge(TextStyle(color = textColor)),
            singleLine = true,
            interactionSource = interactionSource,
            cursorBrush = SolidColor(if (isError) colors.errorCursorColor else colors.cursorColor),
            decorationBox = { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = value,
                    innerTextField = innerTextField,
                    enabled = enabled,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    isError = isError,
                    label = label,
                    placeholder = placeholder,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    supportingText = supportingText,
                    colors = colors,
                    contentPadding = contentPadding,
                    container = {
                        OutlinedTextFieldDefaults.Container(
                            enabled = enabled,
                            isError = isError,
                            interactionSource = interactionSource,
                            colors = colors,
                        )
                    },
                )
            },
        )
    }
}
