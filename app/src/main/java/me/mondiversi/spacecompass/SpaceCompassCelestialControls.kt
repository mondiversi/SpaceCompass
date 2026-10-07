package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal val LocalSpaceCompassCatalogOpen = staticCompositionLocalOf<MutableState<Boolean>?> { null }

@Composable
internal fun SpaceCompassCelestialSelector(body: SpaceCompassCelestialBody, color: Color, background: Color,
    modifier: Modifier = Modifier, timeMs: Long = System.currentTimeMillis(),
    remote: SpaceCompassCelestialRemoteData = SpaceCompassCelestialRemoteData(),
    selectedBodies: Set<SpaceCompassCelestialBody> = setOf(body),
    onToggleAll: (() -> Unit)? = null, onSelect: (SpaceCompassCelestialBody) -> Unit) {
    val catalogOpen = LocalSpaceCompassCatalogOpen.current
    val label = stringResource(R.string.celestial_select)
    val count = formatSpaceCompassNumber(selectedBodies.size.toDouble(), 0,
        LocalSpaceCompassNumericFormat.current, grouping = false)
    SpaceCompassFloatingControlHitRegion {
        Box(modifier.spaceCompassFloatingControlVisual()) {
            Surface(onClick = { catalogOpen?.value = true }, modifier = Modifier.size(48.dp).testTag("celestial-select").semantics {
                stateDescription = count
                contentDescription = label
            }, shape = CircleShape, color = background.copy(alpha = .94f), contentColor = color,
                border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = .35f)), shadowElevation = 3.dp) {
                Box(contentAlignment = Alignment.Center) {
                    SpaceCompassCelestialCatalogIcon(Modifier.size(26.dp), color, crossed = selectedBodies.isEmpty())
                }
            }
            // A fixed circular counter stays inside the selector's accessible touch target.
            if (selectedBodies.isNotEmpty()) Box(
                Modifier.align(Alignment.TopEnd).size(16.dp)
                    .background(background, CircleShape).border(1.dp, color.copy(alpha = .5f), CircleShape)
                    .testTag("celestial-selection-count").clearAndSetSemantics {},
                contentAlignment = Alignment.Center
            ) { Text(count, color = color, fontSize = 9.sp, lineHeight = 10.sp, maxLines = 1) }

        }
    }
}

/** One familiar ringed planet, using the same neutral toolbar glyph as the other actions. */
@Composable
private fun SpaceCompassCelestialCatalogIcon(modifier: Modifier, tint: Color, crossed: Boolean) {
    Canvas(modifier) {
        val stroke = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(tint, size.minDimension * 0.27f, style = stroke)
        rotate(-28f) {
            val topLeft = Offset(size.width * 0.04f, size.height * 0.33f)
            val ringSize = Size(size.width * 0.92f, size.height * 0.34f)
            // The front half crosses the disk; the rear arc stops before its silhouette.
            drawArc(tint, 0f, 180f, false, topLeft, ringSize, style = stroke)
            drawArc(tint, 180f, 28f, false, topLeft, ringSize, style = stroke)
            drawArc(tint, 332f, 28f, false, topLeft, ringSize, style = stroke)
        }
        if (crossed) drawLine(tint,
            Offset(size.width * .125f, size.height * .125f),
            Offset(size.width * .875f, size.height * .875f),
            strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
    }
}
/** One kilometer unit only, plus AU. Extra Mkm precision keeps live probe distances visible. */
internal fun formatSpaceCompassCelestialDistance(body: SpaceCompassCelestialBody, km: Double?,
    numeric: SpaceCompassNumericFormat): String {
    if (km == null || !km.isFinite() || km < 0) return "—"
    formatSpaceCompassExtrasolarDistance(km, numeric)?.let { return it }
    val near = body.isEarthSatellite || body == SpaceCompassCelestialBody.MOON || body == SpaceCompassCelestialBody.EARTH_CENTER
    val distance = if (body == SpaceCompassCelestialBody.POLARIS) "${formatSpaceCompassNumber(km / SPACE_COMPASS_LIGHT_YEAR_KM, 1, numeric)} ly"
        else if (near) "${formatSpaceCompassNumber(km, 0, numeric)} km"
        else "${formatSpaceCompassNumber(km / 1e6, if (body.isVoyager) 6 else 2, numeric)} Mkm"
    return if (body == SpaceCompassCelestialBody.EARTH_CENTER) distance else "$distance · ${formatSpaceCompassCelestialAu(km, numeric)} AU"
}

/** One compact compass line. Small/live ranges retain enough Mkm precision to visibly update. */
internal fun formatSpaceCompassCelestialTableDistance(body: SpaceCompassCelestialBody, km: Double?,
    numeric: SpaceCompassNumericFormat): String? {
    if (km == null || !km.isFinite() || km < 0) return null
    formatSpaceCompassExtrasolarDistance(km, numeric)?.let { return it }
    val digits = when {
        body.isEarthSatellite || body.isVoyager -> 6
        body == SpaceCompassCelestialBody.MOON -> 4
        else -> 2
    }
    val millions = km / 1e6
    val minimum = Math.pow(10.0, -digits.toDouble())
    val value = if (millions > 0 && millions < minimum)
        "< ${formatSpaceCompassNumber(minimum, digits, numeric, minimumDigits = 0)}"
    else formatSpaceCompassNumber(millions, digits, numeric, minimumDigits = 0)
    return "$value Mkm · ${formatSpaceCompassCelestialAu(km, numeric)} AU"
}

/** Two fixed unit rows for the viewer, independently of the compact compass format. */
internal fun formatSpaceCompassCelestialObserverDistance(km: Double?, numeric: SpaceCompassNumericFormat): String {
    if (km == null || !km.isFinite() || km < 0) return "—"
    formatSpaceCompassExtrasolarDistance(km, numeric)?.let { return it }
    val millions = km / 1e6
    // Short ISS ranges must remain positive even with a two-decimal Mkm limit.
    val mkm = if (millions > 0 && millions < 0.01)
        "< ${formatSpaceCompassNumber(0.01, 2, numeric, minimumDigits = 0)}"
    else formatSpaceCompassNumber(millions, 2, numeric, minimumDigits = 0)
    return "$mkm Mkm\n${formatSpaceCompassCelestialAu(km, numeric)} AU"
}

internal fun formatSpaceCompassCelestialAu(km: Double, numeric: SpaceCompassNumericFormat): String {
    val astronomicalUnits = km / SPACE_COMPASS_AU_KM
    // Do not present a positive ISS range as zero after limiting AU to four decimals.
    val au = if (astronomicalUnits > 0 && astronomicalUnits < 0.0001)
        "< ${formatSpaceCompassNumber(0.0001, 4, numeric, minimumDigits = 0)}"
    else formatSpaceCompassNumber(astronomicalUnits, 4, numeric, minimumDigits = 0)
    return au
}
