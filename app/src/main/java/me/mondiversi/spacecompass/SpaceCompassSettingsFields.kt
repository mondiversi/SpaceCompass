package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** UVIR-sized descriptions, with Space Compass's own theme and resources. */
@Composable
internal fun SpaceCompassSettingsDescription(text: String, modifier: Modifier = Modifier,
    pageDescription: Boolean = false) {
    Text(text, modifier, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f),
        fontSize = if (pageDescription) 13.sp else 12.sp,
        lineHeight = if (pageDescription) 18.sp else 16.sp)
}

/** Standard Material field geometry; labels shrink to fit instead of changing row height. */
@Composable
internal fun SpaceCompassSettingsTextField(value: String, onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier, label: String, enabled: Boolean = true,
    placeholder: String? = null, trailingIcon: (@Composable () -> Unit)? = null) {
    val scheme = MaterialTheme.colorScheme
    val secondary = scheme.onSurface.copy(alpha = .72f)
    OutlinedTextField(value, onValueChange, modifier, enabled = enabled,
        label = { SpaceCompassSettingsFieldLabel(label) },
        placeholder = { placeholder?.let { Text(it) } }, trailingIcon = trailingIcon,
        singleLine = true, textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = scheme.onSurface, unfocusedTextColor = scheme.onSurface,
            disabledTextColor = secondary.copy(alpha = .65f), cursorColor = scheme.primary,
            focusedBorderColor = scheme.primary, unfocusedBorderColor = scheme.outline,
            disabledBorderColor = scheme.outline.copy(alpha = .55f),
            focusedLabelColor = scheme.onSurface, unfocusedLabelColor = secondary,
            disabledLabelColor = secondary.copy(alpha = .65f),
            focusedPlaceholderColor = secondary, unfocusedPlaceholderColor = secondary,
            disabledPlaceholderColor = secondary.copy(alpha = .65f)))
}

@Composable
private fun SpaceCompassSettingsFieldLabel(text: String) {
    BoxWithConstraints {
        val style = LocalTextStyle.current
        val fontSize = style.fontSize.takeUnless { it == TextUnit.Unspecified } ?: 12.sp
        val measured = rememberTextMeasurer().measure(text,
            style = style.merge(TextStyle(fontSize = fontSize)), maxLines = 1, softWrap = false).size.width
        val scale = if (constraints.maxWidth > 0 && measured > constraints.maxWidth)
            (constraints.maxWidth.toFloat() / measured).coerceIn(.72f, 1f) else 1f
        Text(text, fontSize = (fontSize.value * scale).sp, maxLines = 1, softWrap = false)
    }
}
