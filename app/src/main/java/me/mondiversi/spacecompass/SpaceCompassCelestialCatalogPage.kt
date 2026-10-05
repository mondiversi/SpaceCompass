package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.*
import androidx.compose.ui.res.painterResource
import kotlin.math.round
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

@Composable
internal fun SpaceCompassCelestialCatalogPage(timeMs: Long, remote: SpaceCompassCelestialRemoteData,
    selectedBodies: Set<SpaceCompassCelestialBody>, color: Color, background: Color,
    onBack: () -> Unit, onToggleAll: (Set<SpaceCompassCelestialBody>) -> Unit, onSelect: (SpaceCompassCelestialBody) -> Unit,
    latitude: Double? = null, longitude: Double? = null, altitude: Double = 0.0) {
    val preferences = LocalSpaceCompassPreferences.current
    var catalogPreferences by remember(preferences) { mutableStateOf(readSpaceCompassCatalogPreferences(preferences)) }
    val sort = catalogPreferences.sort
    val types = catalogPreferences.types
    val visibility = catalogPreferences.visibility
    fun updateCatalogPreferences(updated: SpaceCompassCatalogPreferences) {
        if (updated == catalogPreferences) return
        catalogPreferences = updated
        saveSpaceCompassCatalogPreferences(preferences, updated)
    }
    val locale = androidx.compose.ui.platform.LocalContext.current.resources.configuration.locales[0]
    val localizedNames = spaceCompassCelestialCatalogOrder.associateWith { body ->
        stringResource(if (body == SpaceCompassCelestialBody.ANDROMEDA_CORE) R.string.celestial_andromeda_short else body.nameResource)
    }
    var solarDistances by remember { mutableStateOf<Map<SpaceCompassCelestialBody, Double?>>(emptyMap()) }
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    val catalogScroll = rememberLazyListState()
    LaunchedEffect(sort, types, visibility) { catalogScroll.scrollToItem(0) }
    val handleBack = { if (filtersOpen) filtersOpen = false else onBack() }
    androidx.activity.compose.BackHandler(onBack = handleBack)
    val stringResourceForSelection = stringResource(R.string.select_all)
    val numeric = LocalSpaceCompassNumericFormat.current
    val units = LocalSpaceCompassUnits.current
    val distanceUnit = units.distance
    var distances by remember { mutableStateOf<Map<SpaceCompassCelestialBody,Double?>>(emptyMap()) }
    // Keep one catalog snapshot while the user is scrolling; refresh on reopening.
    val catalogTime = remember { timeMs }
    val catalogRemote = remember { remote }
    var observations by remember { mutableStateOf<Map<SpaceCompassCelestialBody, SpaceCompassCelestialObservation?>>(emptyMap()) }
    val catalogLatitude = latitude?.let { round(it * 10000) / 10000 }
    val catalogLongitude = longitude?.let { round(it * 10000) / 10000 }
    val catalogAltitude = round(altitude / 10) * 10
    LaunchedEffect(catalogLatitude, catalogLongitude, catalogAltitude) {
        val snapshot = withContext(Dispatchers.Default) {
            val positions = spaceCompassCelestialCatalogOrder.associateWith { body ->
                if (catalogLatitude == null || catalogLongitude == null) null else runCatching {
                    calculateSpaceCompassCelestialObservation(body, catalogTime, catalogLatitude, catalogLongitude, catalogAltitude, catalogRemote)
                }.getOrNull()
            }
            val ranges = spaceCompassCelestialCatalogOrder.associateWith { body -> runCatching {
                when {
                    body == SpaceCompassCelestialBody.EARTH_CENTER -> positions[body]?.distanceKm
                    body.isEarthSatellite || body == SpaceCompassCelestialBody.MOON -> spaceCompassNearbyCatalogDistanceKm(body, catalogTime, catalogRemote)
                    else -> spaceCompassCelestialCatalogDistanceAu(body, catalogTime, catalogRemote)
                }
            }.getOrNull() }
            Triple(ranges, positions, spaceCompassCelestialCatalogOrder.associateWith { body ->
                runCatching { spaceCompassCelestialCatalogDistanceAu(body, catalogTime, catalogRemote) }.getOrNull()
            })
        }
        distances = snapshot.first
        observations = snapshot.second
        solarDistances = snapshot.third
    }
    val visibleBodies = spaceCompassSortCatalog(
        spaceCompassFilterCatalog(types, visibility, observations.mapValues { it.value?.position?.elevationDegrees }),
        sort, localizedNames, solarDistances, locale)
    val checkedCount = visibleBodies.count { it in selectedBodies }
    val checkedState = when {
        checkedCount == 0 -> ToggleableState.Off
        checkedCount == visibleBodies.size -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    Surface(Modifier.fillMaxSize().testTag("celestial-menu"), color = background, contentColor = color) {
        Column(Modifier.fillMaxSize()) {
            SpaceCompassPageToolbar(stringResource(R.string.pc_celestial_object), handleBack,
                titleColor = color) {
                SpaceCompassCatalogSortButton(sort, color) {
                    updateCatalogPreferences(catalogPreferences.copy(sort = it))
                }
                SpaceCompassTitleActionButton(stringResource(R.string.catalog_filters),
                    onClick = { filtersOpen = !filtersOpen }, modifier = Modifier.width(48.dp).testTag("catalog-filter-toggle"),
                    iconColor = if (types.isNotEmpty() || visibility != SpaceCompassCatalogVisibility.ALL)
                        MaterialTheme.colorScheme.primary else color) {
                    Icon(painterResource(R.drawable.ic_filter), contentDescription = null)
                }
            }
            AnimatedVisibility(filtersOpen, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                SpaceCompassCatalogFilterBar(types, visibility,
                    onType = { type -> updateCatalogPreferences(catalogPreferences.copy(
                        types = if (type in types) types - type else types + type)) },
                    onAllTypes = { updateCatalogPreferences(catalogPreferences.copy(types = emptySet())) },
                    onVisibility = { updateCatalogPreferences(catalogPreferences.copy(visibility = it)) },
                    onReset = { updateCatalogPreferences(catalogPreferences.copy(
                        types = emptySet(), visibility = SpaceCompassCatalogVisibility.ALL)) })
            }
            LazyColumn(Modifier.fillMaxWidth().weight(1f)
                .clipToBounds().lazyScrollbarOverlay(catalogScroll, color.copy(alpha = .58f))
                .padding(end = 8.dp).testTag("celestial-catalog-scroll"), state = catalogScroll) {
            item(key = "catalog-header") {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(24.dp).height(48.dp), contentAlignment = Alignment.Center) {
                        TriStateCheckbox(checkedState, onClick = { onToggleAll(visibleBodies.toSet()) },
                            enabled = visibleBodies.isNotEmpty(), colors = spaceCompassCheckboxColors(),
                            modifier = Modifier.requiredSize(48.dp).testTag("celestial-select-all").semantics {
                                contentDescription = stringResourceForSelection
                            })
                    }
                    Text(stringResource(R.string.catalog_distance_from_sun), Modifier.weight(1f),
                        color = color.copy(alpha = 0.65f), fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
            }
            if (visibleBodies.isEmpty()) item(key = "catalog-empty") {
                Text(stringResource(R.string.catalog_no_results), Modifier.fillMaxWidth().padding(16.dp),
                    color = color.copy(alpha = .65f), fontSize = 14.sp)
            }
            items(visibleBodies, key = { it.name }) { candidate ->
                DropdownMenuItem(text = { Text(stringResource(if (candidate == SpaceCompassCelestialBody.ANDROMEDA_CORE)
                    R.string.celestial_andromeda_short else candidate.nameResource), color = color, fontSize = 14.sp,
                    maxLines = if (candidate == SpaceCompassCelestialBody.EARTH_CENTER || candidate == SpaceCompassCelestialBody.ANDROMEDA_CORE || candidate == SpaceCompassCelestialBody.TON_618 ||
                        candidate == SpaceCompassCelestialBody.STEPHENSON_2_18 || candidate == SpaceCompassCelestialBody.RX_J1856 ||
                        candidate == SpaceCompassCelestialBody.PSR_J0437 || candidate == SpaceCompassCelestialBody.PROXIMA_CENTAURI) 2 else 1, lineHeight = 16.sp, overflow = TextOverflow.Ellipsis,
                    fontWeight = if (candidate in selectedBodies) FontWeight.Bold else FontWeight.Normal) },
                    onClick = { onSelect(candidate) },
                    leadingIcon = { Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(candidate in selectedBodies, onCheckedChange = null, colors = spaceCompassCheckboxColors())
                        SpaceCompassCelestialThumbnail(candidate, Modifier.size(24.dp).then(
                        if (candidate in selectedBodies)
                            Modifier.border(2.dp, spaceCompassCelestialPathTint(candidate), CircleShape) else Modifier))
                    } },
                    modifier = Modifier.testTag("celestial-body-${candidate.name}").semantics(mergeDescendants = true) {
                        role = Role.Checkbox
                        toggleableState = if (candidate in selectedBodies) ToggleableState.On else ToggleableState.Off
                    },
                    trailingIcon = { Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (candidate.isEarthSatellite || candidate == SpaceCompassCelestialBody.MOON || candidate == SpaceCompassCelestialBody.EARTH_CENTER)
                            (formatSpaceCompassSelectedDistance(candidate, distances[candidate], numeric, distanceUnit, units.feet) ?: "—") + "\n" + (if (candidate == SpaceCompassCelestialBody.EARTH_CENTER) "GPS" else stringResource(R.string.celestial_view_earth))
                            else formatSpaceCompassCelestialCatalogDistance(distances[candidate], numeric, distanceUnit),
                            modifier = Modifier.widthIn(max = 144.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            color = color.copy(alpha = 0.60f), fontSize = 11.sp, lineHeight = 14.sp)
                    } })
            }
            }
        }
    }
}
