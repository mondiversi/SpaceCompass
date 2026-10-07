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
    latitude: Double? = null, longitude: Double? = null, altitude: Double = 0.0,
    onRefresh: () -> Unit = {}, onRevealHiddenObject: () -> Unit = {}) {
    val preferences = LocalSpaceCompassPreferences.current
    val availableBodies = remember(selectedBodies) { spaceCompassAvailableCelestialCatalog(selectedBodies) }
    var catalogPreferences by remember(preferences) { mutableStateOf(readSpaceCompassCatalogPreferences(preferences)) }
    val sort = catalogPreferences.sort
    val valueField = sort.valueField
    val types = catalogPreferences.types
    val visibility = catalogPreferences.visibility
    fun updateCatalogPreferences(updated: SpaceCompassCatalogPreferences) {
        if (updated == catalogPreferences) return
        catalogPreferences = updated
        saveSpaceCompassCatalogPreferences(preferences, updated)
    }
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val localizedNames = availableBodies.associateWith { body ->
        stringResource(if (body == SpaceCompassCelestialBody.ANDROMEDA_CORE) R.string.celestial_andromeda_short else body.nameResource)
    }
    var solarDistances by remember { mutableStateOf<Map<SpaceCompassCelestialBody, Double?>>(emptyMap()) }
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var pendingHiddenReveal by remember { mutableStateOf(false) }
    val catalogScroll = rememberLazyListState()
    LaunchedEffect(sort, types, visibility, query) { catalogScroll.scrollToItem(0) }
    val handleBack = { if (filtersOpen) filtersOpen = false else onBack() }
    androidx.activity.compose.BackHandler(onBack = handleBack)
    val stringResourceForSelection = stringResource(R.string.select_all)
    val numeric = LocalSpaceCompassNumericFormat.current
    val catalogCount = formatSpaceCompassNumber(availableBodies.size.toDouble(),
        0, numeric, grouping = false)
    val units = LocalSpaceCompassUnits.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    val referencePressure = formatSpaceCompassPressure(SpaceCompassPressureUnit.BAR.pascals, numeric, units.pressure)!!
    val referenceLabel: (Int) -> String = { id ->
        if (id == R.string.catalog_temperature_atmosphere) resources.getString(id, referencePressure)
        else resources.getString(id)
    }
    val distanceUnit = units.distance
    var distances by remember { mutableStateOf<Map<SpaceCompassCelestialBody,Double?>>(emptyMap()) }
    var refreshRevision by remember { mutableIntStateOf(0) }
    var calculating by remember { mutableStateOf(true) }
    // Loading flags alone must not trigger catalog calculations.
    val catalogRemote = remember(remote.iss, remote.starlink, remote.ephemerides, remote.motions) { remote }
    var observations by remember { mutableStateOf<Map<SpaceCompassCelestialBody, SpaceCompassCelestialObservation?>>(emptyMap()) }
    val catalogLatitude = latitude?.let { round(it * 10000) / 10000 }
    val catalogLongitude = longitude?.let { round(it * 10000) / 10000 }
    val catalogAltitude = round(altitude / 10) * 10
    // Distance rows refresh every ten minutes; arriving models/location update immediately.
    val catalogTime = remember(timeMs / SPACE_COMPASS_CELESTIAL_CATALOG_REFRESH_MS, refreshRevision,
        catalogRemote, catalogLatitude, catalogLongitude, catalogAltitude) { timeMs }
    // Keep horizon filtering on its existing minute cadence, independently of distance rows.
    val visibilityTime = remember(timeMs / 60_000L, refreshRevision,
        catalogRemote, catalogLatitude, catalogLongitude, catalogAltitude) { timeMs }
    LaunchedEffect(availableBodies, visibilityTime, refreshRevision, catalogRemote, catalogLatitude, catalogLongitude, catalogAltitude) {
        observations = withContext(Dispatchers.Default) {
            availableBodies.associateWith { body ->
                if (catalogLatitude == null || catalogLongitude == null) null else runCatching {
                    calculateSpaceCompassCelestialObservation(body, visibilityTime, catalogLatitude, catalogLongitude, catalogAltitude, catalogRemote)
                }.getOrNull()
            }
        }
    }
    LaunchedEffect(availableBodies, catalogTime, refreshRevision, catalogRemote, catalogLatitude, catalogLongitude, catalogAltitude) {
        calculating = true
        try {
            val snapshot = withContext(Dispatchers.Default) {
                val ranges = availableBodies.associateWith { body -> runCatching {
                    when {
                        body == SpaceCompassCelestialBody.EARTH_CENTER ->
                            if (catalogLatitude == null || catalogLongitude == null) null else
                                calculateSpaceCompassCelestialObservation(body, catalogTime, catalogLatitude, catalogLongitude, catalogAltitude, catalogRemote)?.distanceKm
                        body.isEarthSatellite || body == SpaceCompassCelestialBody.MOON -> spaceCompassNearbyCatalogDistanceKm(body, catalogTime, catalogRemote)
                        else -> spaceCompassCelestialCatalogDistanceAu(body, catalogTime, catalogRemote)
                    }
                }.getOrNull() }
                ranges to availableBodies.associateWith { body ->
                    runCatching { spaceCompassCelestialCatalogDistanceAu(body, catalogTime, catalogRemote) }.getOrNull()
                }
            }
            distances = snapshot.first
            solarDistances = snapshot.second
        } finally { calculating = false }
    }
    // Newly revealed distant objects enter the correct sort position in the same frame.
    val readySolarDistances = spaceCompassCatalogDistancesWithReferences(solarDistances, availableBodies)
    val readyDistances = spaceCompassCatalogDistancesWithReferences(distances, availableBodies)
    val visibleBodies = spaceCompassSortCatalog(
        spaceCompassSearchCatalog(
            spaceCompassFilterCatalog(types, visibility, observations.mapValues { it.value?.position?.elevationDegrees }, availableBodies),
            localizedNames, query),
        sort, localizedNames, readySolarDistances, locale)
    LaunchedEffect(pendingHiddenReveal, visibleBodies) {
        if (pendingHiddenReveal) {
            val index = visibleBodies.indexOf(SpaceCompassCelestialBody.LV_426)
            if (index >= 0) {
                // Include the table header in the item offset; preserve existing filter/sort preferences.
                catalogScroll.scrollToItem(index + 1)
                pendingHiddenReveal = false
            }
        }
    }
    val checkedCount = visibleBodies.count { it in selectedBodies }
    val checkedState = when {
        checkedCount == 0 -> ToggleableState.Off
        checkedCount == visibleBodies.size -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    Surface(Modifier.fillMaxSize().testTag("celestial-menu"), color = background, contentColor = color) {
        Column(Modifier.fillMaxSize()) {
            SpaceCompassPageToolbar(stringResource(R.string.pc_celestial_object, catalogCount), handleBack,
                titleColor = color, titleModifier = spaceCompassHiddenObjectHold {
                    pendingHiddenReveal = true
                    onRevealHiddenObject()
                }.testTag("catalog-title")) {
                SpaceCompassCatalogSortButton(sort, color) {
                    updateCatalogPreferences(catalogPreferences.copy(sort = it))
                }
                SpaceCompassTitleActionButton(stringResource(R.string.catalog_filters),
                    onClick = { filtersOpen = !filtersOpen }, modifier = Modifier.width(48.dp).testTag("catalog-filter-toggle"),
                    iconColor = if (types.isNotEmpty() || visibility != SpaceCompassCatalogVisibility.ALL || query.isNotBlank())
                        MaterialTheme.colorScheme.primary else color) {
                    Icon(painterResource(R.drawable.ic_filter), contentDescription = null)
                }
                val refreshing = remote.refreshing || calculating
                SpaceCompassTitleActionButton(stringResource(if (refreshing) R.string.catalog_refreshing else R.string.catalog_refresh),
                    onClick = { refreshRevision++; onRefresh() }, enabled = !refreshing,
                    modifier = Modifier.width(48.dp).testTag("catalog-refresh"), iconColor = color) {
                    if (refreshing) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp,
                        color = color.copy(alpha = .65f))
                    else Icon(painterResource(R.drawable.ic_refresh), contentDescription = null)
                }
            }
            AnimatedVisibility(filtersOpen, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                SpaceCompassCatalogFilterBar(types, visibility, visibleCount = visibleBodies.size, searchActive = query.isNotBlank(),
                    onType = { type -> updateCatalogPreferences(catalogPreferences.copy(
                        types = if (type in types) types - type else types + type)) },
                    onAllTypes = { updateCatalogPreferences(catalogPreferences.copy(types = emptySet())) },
                    onVisibility = { updateCatalogPreferences(catalogPreferences.copy(visibility = it)) },
                    onReset = { query = ""; updateCatalogPreferences(catalogPreferences.copy(
                        types = emptySet(), visibility = SpaceCompassCatalogVisibility.ALL)) })
            }
            LazyColumn(Modifier.fillMaxWidth().weight(1f)
                .clipToBounds().lazyScrollbarOverlay(catalogScroll, color.copy(alpha = .58f))
                .padding(end = 8.dp).testTag("celestial-catalog-scroll"), state = catalogScroll) {
            item(key = "catalog-header") {
                BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                    // Enlarge the previous equal-column field itself, excluding its unchanged outer spacing.
                    val previousSearchWidth = ((maxWidth - 24.dp) / 2 - 20.dp).coerceAtLeast(0.dp)
                    val searchWidth = previousSearchWidth * 1.30f + 20.dp
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(24.dp).height(48.dp), contentAlignment = Alignment.Center) {
                            TriStateCheckbox(checkedState, onClick = { onToggleAll(visibleBodies.toSet()) },
                                enabled = visibleBodies.isNotEmpty(), colors = spaceCompassCheckboxColors(),
                                modifier = Modifier.requiredSize(48.dp).testTag("celestial-select-all").semantics {
                                    contentDescription = stringResourceForSelection
                                })
                        }
                        SpaceCompassCatalogSearchField(query, { query = it },
                            Modifier.width(searchWidth).padding(start = 12.dp, end = 8.dp))
                        Text(stringResource(valueField.label), Modifier.weight(1f).testTag("catalog-value-header"),
                            color = color.copy(alpha = 0.65f), fontSize = 11.sp, lineHeight = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    }
                }
            }
            if (visibleBodies.isEmpty()) item(key = "catalog-empty") {
                Text(stringResource(R.string.catalog_no_results), Modifier.fillMaxWidth().padding(16.dp),
                    color = color.copy(alpha = .65f), fontSize = 14.sp)
            }
            items(visibleBodies, key = { it.name }) { candidate ->
                DropdownMenuItem(text = { Text(stringResource(if (candidate == SpaceCompassCelestialBody.ANDROMEDA_CORE)
                    R.string.celestial_andromeda_short else candidate.nameResource), color = color, fontSize = 14.sp,
                    maxLines = if (valueField == SpaceCompassCatalogSortField.DAY_TEMPERATURE ||
                        valueField == SpaceCompassCatalogSortField.NIGHT_TEMPERATURE ||
                        candidate == SpaceCompassCelestialBody.EARTH_CENTER || candidate == SpaceCompassCelestialBody.ANDROMEDA_CORE || candidate == SpaceCompassCelestialBody.TON_618 ||
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
                        Text(if (valueField != SpaceCompassCatalogSortField.DISTANCE)
                            formatSpaceCompassCatalogPhysicalValue(candidate, valueField, numeric, units, referenceLabel)
                            else if (sort.field == SpaceCompassCatalogSortField.DISTANCE)
                            formatSpaceCompassCelestialCatalogDistance(readySolarDistances[candidate], numeric, distanceUnit)
                            else if (readyDistances[candidate] == null && spaceCompassCelestialDataOutsideDate(candidate, catalogTime, catalogRemote))
                            stringResource(R.string.catalog_date_unavailable)
                            else if (candidate.isEarthSatellite || candidate == SpaceCompassCelestialBody.MOON || candidate == SpaceCompassCelestialBody.EARTH_CENTER)
                            (formatSpaceCompassSelectedDistance(candidate, readyDistances[candidate], numeric, distanceUnit, units.feet) ?: "—") + "\n" + (if (candidate == SpaceCompassCelestialBody.EARTH_CENTER) "GPS" else stringResource(R.string.celestial_view_earth))
                            else formatSpaceCompassCelestialCatalogDistance(readyDistances[candidate], numeric, distanceUnit),
                            modifier = Modifier.widthIn(max = 144.dp).testTag("catalog-value-${candidate.name}"), textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            color = color.copy(alpha = 0.60f), fontSize = 11.sp, lineHeight = 14.sp)
                    } })
            }
            }
        }
    }
}
