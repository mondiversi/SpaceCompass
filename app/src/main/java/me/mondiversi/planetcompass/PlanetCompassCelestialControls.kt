package me.mondiversi.planetcompass

import androidx.compose.foundation.layout.*
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
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

@Composable
internal fun PlanetCompassCelestialSelector(body: PlanetCompassCelestialBody, color: Color, background: Color,
    modifier: Modifier = Modifier, timeMs: Long = System.currentTimeMillis(),
    remote: PlanetCompassCelestialRemoteData = PlanetCompassCelestialRemoteData(),
    selectedBodies: Set<PlanetCompassCelestialBody> = setOf(body),
    onToggleAll: (() -> Unit)? = null, onSelect: (PlanetCompassCelestialBody) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val numeric = LocalPlanetCompassNumericFormat.current
    val label = stringResource(R.string.celestial_select)
    val density = LocalDensity.current
    val windowSize = LocalWindowInfo.current.containerSize
    val menuWidth = (with(density) { windowSize.width.toDp() } - 20.dp).coerceIn(48.dp, 420.dp)
    var distances by remember { mutableStateOf<Map<PlanetCompassCelestialBody,Double?>>(emptyMap()) }
    LaunchedEffect(expanded, timeMs/86_400_000, remote.motions, remote.ephemerides) {
        if (expanded) distances = withContext(Dispatchers.Default) {
            planetCompassCelestialCatalogOrder.associateWith { runCatching { planetCompassCelestialCatalogDistanceAu(it,timeMs,remote) }.getOrNull() }
        }
    }
    Box(modifier) {
        PlanetCompassTitleActionButton(label, { expanded = true }, Modifier.testTag("celestial-select").semantics {
            stateDescription = selectedBodies.size.toString()
        }, iconColor = color) {
            PlanetCompassCelestialCatalogIcon(Modifier.size(PlanetCompassTitleActionIconSize), color)
        }
        // Same count bubble as saved acquisitions, outside the toolbar glyph's circular clip.
        // Count checked objects, even when their ephemeris is unavailable or another is inspected.
        if (selectedBodies.isNotEmpty()) PlanetCompassAnimatedCountBadge(
            countText = selectedBodies.size.toString(), indicatorColor = color,
            containerColor = background, contentColor = color, syncInProgress = false,
            modifier = Modifier.align(Alignment.TopEnd).defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                .testTag("celestial-selection-count")
                .pointerInput(Unit) { detectTapGestures { expanded = true } }
                .clearAndSetSemantics {},
            fontSize = 9.sp, horizontalPadding = 4.dp, shadowElevation = 2.dp)
        PlanetCompassAdaptiveDropdownMenu(expanded, { expanded = false }, Modifier.heightIn(max = 420.dp).width(menuWidth)
            .testTag("celestial-menu"), containerColor = background) {
            if (onToggleAll != null) DropdownMenuItem(text = { Text(stringResource(R.string.select_all), color = color) },
                onClick = onToggleAll, modifier = Modifier.testTag("celestial-select-all").semantics(mergeDescendants = true) {
                    role = Role.Checkbox
                    toggleableState = when (selectedBodies.size) {
                        0 -> ToggleableState.Off
                        planetCompassCelestialCatalogOrder.size -> ToggleableState.On
                        else -> ToggleableState.Indeterminate
                    }
                },
                trailingIcon = { TriStateCheckbox(
                    state = when (selectedBodies.size) {
                        0 -> androidx.compose.ui.state.ToggleableState.Off
                        planetCompassCelestialCatalogOrder.size -> androidx.compose.ui.state.ToggleableState.On
                        else -> androidx.compose.ui.state.ToggleableState.Indeterminate
                    }, onClick = null, colors = planetCompassCheckboxColors()) })
            HorizontalDivider(color = color.copy(alpha = 0.12f))
            Text(stringResource(R.string.celestial_catalog_distance), Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = color.copy(alpha = 0.65f), fontSize = 11.sp)
            planetCompassCelestialCatalogOrder.forEach { candidate ->
                DropdownMenuItem(text = { Text(stringResource(candidate.nameResource), color = color, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontWeight = if (candidate in selectedBodies) FontWeight.Bold else FontWeight.Normal) },
                    onClick = { if (onToggleAll == null) expanded = false; onSelect(candidate) },
                    leadingIcon = { PlanetCompassCelestialThumbnail(candidate, Modifier.size(24.dp).then(
                        if (candidate in selectedBodies)
                            Modifier.border(2.dp, planetCompassCelestialPathTint(candidate), CircleShape) else Modifier)) },
                    modifier = Modifier.testTag("celestial-body-${candidate.name}").semantics(mergeDescendants = true) {
                        role = Role.Checkbox
                        toggleableState = if (candidate in selectedBodies) ToggleableState.On else ToggleableState.Off
                    },
                    trailingIcon = { Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(formatPlanetCompassCelestialCatalogDistance(distances[candidate],numeric), color = color.copy(alpha = 0.60f), fontSize = 11.sp)
                        Checkbox(candidate in selectedBodies, onCheckedChange = null, colors = planetCompassCheckboxColors())
                    } })
            }
        }
    }
}

/** One familiar ringed planet, using the same neutral toolbar glyph as the other actions. */
@Composable
private fun PlanetCompassCelestialCatalogIcon(modifier: Modifier, tint: Color) {
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
    }
}
/** One kilometer unit only, plus AU. Extra Mkm precision keeps live probe distances visible. */
internal fun formatPlanetCompassCelestialDistance(body: PlanetCompassCelestialBody, km: Double?,
    numeric: PlanetCompassNumericFormat): String {
    if (km == null || !km.isFinite() || km < 0) return "—"
    val near = body.isEarthSatellite || body == PlanetCompassCelestialBody.MOON
    val distance = if (body == PlanetCompassCelestialBody.POLARIS) "${formatPlanetCompassNumber(km / PLANET_COMPASS_LIGHT_YEAR_KM, 1, numeric)} ly"
        else if (near) "${formatPlanetCompassNumber(km, 0, numeric)} km"
        else "${formatPlanetCompassNumber(km / 1e6, if (body.isVoyager) 6 else 2, numeric)} Mkm"
    return "$distance · ${formatPlanetCompassCelestialAu(km, numeric)} AU"
}

/** One compact compass line. Small/live ranges retain enough Mkm precision to visibly update. */
internal fun formatPlanetCompassCelestialTableDistance(body: PlanetCompassCelestialBody, km: Double?,
    numeric: PlanetCompassNumericFormat): String? {
    if (km == null || !km.isFinite() || km < 0) return null
    val digits = when {
        body.isEarthSatellite || body.isVoyager -> 6
        body == PlanetCompassCelestialBody.MOON -> 4
        else -> 2
    }
    val millions = km / 1e6
    val minimum = Math.pow(10.0, -digits.toDouble())
    val value = if (millions > 0 && millions < minimum)
        "< ${formatPlanetCompassNumber(minimum, digits, numeric, minimumDigits = 0)}"
    else formatPlanetCompassNumber(millions, digits, numeric, minimumDigits = 0)
    return "$value Mkm · ${formatPlanetCompassCelestialAu(km, numeric)} AU"
}

/** Two fixed unit rows for the viewer, independently of the compact compass format. */
internal fun formatPlanetCompassCelestialObserverDistance(km: Double?, numeric: PlanetCompassNumericFormat): String {
    if (km == null || !km.isFinite() || km < 0) return "—"
    val millions = km / 1e6
    // Short ISS ranges must remain positive even with a two-decimal Mkm limit.
    val mkm = if (millions > 0 && millions < 0.01)
        "< ${formatPlanetCompassNumber(0.01, 2, numeric, minimumDigits = 0)}"
    else formatPlanetCompassNumber(millions, 2, numeric, minimumDigits = 0)
    return "$mkm Mkm\n${formatPlanetCompassCelestialAu(km, numeric)} AU"
}

private fun formatPlanetCompassCelestialAu(km: Double, numeric: PlanetCompassNumericFormat): String {
    val astronomicalUnits = km / PLANET_COMPASS_AU_KM
    // Do not present a positive ISS range as zero after limiting AU to four decimals.
    val au = if (astronomicalUnits > 0 && astronomicalUnits < 0.0001)
        "< ${formatPlanetCompassNumber(0.0001, 4, numeric, minimumDigits = 0)}"
    else formatPlanetCompassNumber(astronomicalUnits, 4, numeric, minimumDigits = 0)
    return au
}
