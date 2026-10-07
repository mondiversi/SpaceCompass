package me.mondiversi.spacecompass

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection

internal data class SpaceCompassPositionDetailsData(
    val rows: List<SpaceCompassSunDataRow>, val notes: List<String>, val weatherAttribution: Boolean
)

/** The map and tabular readout share one native window, preserving WebView zoom on GPS updates. */
@Composable
internal fun SpaceCompassPositionDetails(hasWeather: Boolean, weatherRow: SpaceCompassSunDataRow,
    rows: List<SpaceCompassSunDataRow>, latitude: Double?, longitude: Double?, scrollbarColor: Color, onBack: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val background = spaceCompassPageBackground()
    val foreground = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary
    val title = stringResource(R.string.celestial_environment_info_title)
    val back = stringResource(R.string.navigate_back)
    val unavailable = stringResource(R.string.position_map_unavailable)
    val retry = stringResource(R.string.sun_finder_retry)
    val zoomIn = stringResource(R.string.celestial_view_zoom_in)
    val zoomOut = stringResource(R.string.celestial_view_zoom_out)
    val data = SpaceCompassPositionDetailsData(rows + weatherRow, listOf(
        stringResource(R.string.celestial_environment_orientation_note),
        stringResource(if (LocalSpaceCompassObserver.current?.plan != null) R.string.observer_limits else R.string.sun_finder_terrain_note),
        stringResource(if (LocalSpaceCompassObserver.current?.plan != null) R.string.observer_privacy else R.string.sun_weather_privacy)), hasWeather)
    val close by rememberUpdatedState(onBack)
    val currentUrl = spaceCompassPositionMapUrl(latitude, longitude)
    var dialog by remember { mutableStateOf<SpaceCompassNativePositionMap?>(null) }
    DisposableEffect(context, background, foreground, accent, scrollbarColor, title, density.fontScale, rtl) {
        val window = SpaceCompassNativePositionMap(context, currentUrl, title, back, unavailable, retry,
            density.density, density.fontScale, rtl, background.toArgb(), foreground.toArgb(), accent.toArgb(), scrollbarColor.toArgb(),
            background.luminance() < .4f, { close() }, data, zoomIn = zoomIn, zoomOut = zoomOut)
        dialog = window
        window.show()
        onDispose { dialog = null; window.releaseMap() }
    }
    SideEffect { dialog?.updateDetails(data, currentUrl) }
}
