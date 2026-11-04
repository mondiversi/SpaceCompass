package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** UVIR-sized descriptions, with Space Compass's own theme and resources. */
@Composable
internal fun SpaceCompassSettingsDescription(text: String, modifier: Modifier = Modifier,
    pageDescription: Boolean = false, compact: Boolean = false) {
    Text(text, modifier, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f),
        fontSize = when { compact -> 10.sp; pageDescription -> 13.sp; else -> 12.sp },
        lineHeight = when { compact -> 13.sp; pageDescription -> 18.sp; else -> 16.sp })
}

internal val SpaceCompassCompactFieldContentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
internal val SpaceCompassCompactFieldMinHeight = 44.dp
internal val SpaceCompassCompactFieldIconSize = 40.dp

/** Settings inputs share the compact year-field geometry without changing their input behavior. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SpaceCompassSettingsTextField(value: String, onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier, label: String, enabled: Boolean = true,
    placeholder: String? = null, trailingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default, isError: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    val secondary = scheme.onSurface.copy(alpha = .72f)
    val colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = scheme.onSurface, unfocusedTextColor = scheme.onSurface,
        disabledTextColor = secondary.copy(alpha = .65f), cursorColor = scheme.primary,
        focusedBorderColor = scheme.primary, unfocusedBorderColor = scheme.outline,
        disabledBorderColor = scheme.outline.copy(alpha = .55f),
        focusedLabelColor = scheme.onSurface, unfocusedLabelColor = secondary,
        disabledLabelColor = secondary.copy(alpha = .65f),
        focusedPlaceholderColor = secondary, unfocusedPlaceholderColor = secondary,
        disabledPlaceholderColor = secondary.copy(alpha = .65f))
    val interaction = remember { MutableInteractionSource() }
    // DecorationBox keeps Material's floating label, cursor alignment, shape and focus border.
    // The compact frame is 44 dp; its smaller arrow keeps Compose's expanded touch target.
    BasicTextField(value, onValueChange,
        modifier.semantics(mergeDescendants = true) { }.padding(top = 8.dp)
            .defaultMinSize(minHeight = SpaceCompassCompactFieldMinHeight),
        enabled = enabled, singleLine = true, interactionSource = interaction,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = if (enabled) scheme.onSurface else secondary.copy(alpha = .65f)),
        cursorBrush = SolidColor(if (isError) scheme.error else scheme.primary),
        keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
        decorationBox = { input ->
            // Material also reserves height around trailing icons; limit that visual reservation
            // locally while Foundation still expands small clickable targets for touch input.
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides SpaceCompassCompactFieldIconSize) {
                OutlinedTextFieldDefaults.DecorationBox(value, input, enabled = enabled, singleLine = true,
                    visualTransformation = VisualTransformation.None, interactionSource = interaction,
                    isError = isError, label = { SpaceCompassSettingsFieldLabel(label) },
                    placeholder = { placeholder?.let { Text(it) } }, trailingIcon = trailingIcon,
                    colors = colors, contentPadding = SpaceCompassCompactFieldContentPadding)
            }
        })
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
