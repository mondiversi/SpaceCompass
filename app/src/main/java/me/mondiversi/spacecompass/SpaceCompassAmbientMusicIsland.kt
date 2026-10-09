package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.core.content.edit

@Composable
internal fun SpaceCompassAmbientMusicIsland() {
    val preferences = LocalSpaceCompassPreferences.current ?: return
    val settings = LocalSpaceCompassAmbientMusic.current
    val preview = LocalSpaceCompassMusicVolumePreview.current
    val context = LocalContext.current
    val saved = stringResource(R.string.settings_saved)
    val title = stringResource(R.string.music_title)
    val volumeTitle = stringResource(R.string.music_volume)
    var draft by remember(settings.volumePercent) { mutableFloatStateOf(settings.volumePercent.toFloat()) }
    DisposableEffect(preferences) { onDispose { preview(null) } }
    LaunchedEffect(settings.enabled) {
        if (!settings.enabled) {
            draft = settings.volumePercent.toFloat()
            preview(null)
        }
    }
    SpaceCompassSettingsToggleIsland(title, "ambient_music", settings.enabled, title,
        onCheckedChange = { enabled ->
            if (enabled != settings.enabled) {
                preferences.edit { putBoolean(SPACE_COMPASS_MUSIC_ENABLED_KEY, enabled) }
                showSpaceCompassBottomMessage(context, saved)
            }
        }, modifier = Modifier.testTag("music-enabled")) {
        SpaceCompassSettingsDescription(stringResource(R.string.music_description))
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(volumeTitle, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(stringResource(R.string.music_volume_value,
                    formatSpaceCompassNumber(draft.roundToInt().toDouble(), 0,
                        LocalSpaceCompassNumericFormat.current, systemLocale = LocalSpaceCompassDeviceLocale.current)),
                    Modifier.testTag("music-volume-value"), fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f),
                    style = TextStyle(textDirection = TextDirection.ContentOrLtr))
            }
            Slider(draft, onValueChange = { draft = it; preview(it.roundToInt()) }, valueRange = 0f..100f, steps = 9,
                onValueChangeFinished = {
                    val value = draft.roundToInt().coerceIn(0, 100)
                    if (value != settings.volumePercent) {
                        preferences.edit { putInt(SPACE_COMPASS_MUSIC_VOLUME_KEY, value) }
                        showSpaceCompassBottomMessage(context, saved)
                    }
                    preview(null)
                }, colors = spaceCompassSliderColors(), modifier = Modifier.fillMaxWidth()
                    .testTag("music-volume").semantics { contentDescription = volumeTitle })
        }
    }
}
