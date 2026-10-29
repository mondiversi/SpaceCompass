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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun SpaceCompassCelestialCatalogPage(timeMs: Long, remote: SpaceCompassCelestialRemoteData,
    selectedBodies: Set<SpaceCompassCelestialBody>, color: Color, background: Color,
    onBack: () -> Unit, onToggleAll: (Set<SpaceCompassCelestialBody>) -> Unit, onSelect: (SpaceCompassCelestialBody) -> Unit,
    latitude: Double? = null, longitude: Double? = null, altitude: Double = 0.0,
    onObserve: (SpaceCompassCelestialBody) -> Unit = {}) {
    val preferences = LocalSpaceCompassPreferences.current
    // Capture grouping for this catalogue visit, including its independent Observatory round trips.
    // Closing the catalogue back to the main view discards the snapshot; checks save immediately.
    val prioritySelectedNames = rememberSaveable { selectedBodies.map { it.name } }
    val prioritySelected = remember(prioritySelectedNames) {
        spaceCompassAllCelestialOrder.filter { it.name in prioritySelectedNames }.toSet()
    }
    val availableBodies = remember(selectedBodies) { spaceCompassAvailableCelestialCatalog(selectedBodies) }
    var catalogPreferences by remember(preferences) { mutableStateOf(readSpaceCompassCatalogPreferences(preferences)) }
    val sort = catalogPreferences.sort
    val valueField = sort.valueField
    val types = catalogPreferences.types
    val visibility = catalogPreferences.visibility
    val origin = catalogPreferences.origin
    val pointing = catalogPreferences.pointing
    val favoritesOnly = catalogPreferences.favoritesOnly
    val favorites = rememberSpaceCompassCelestialFavorites()
    // Category totals describe the current catalogue scope, including the independent heart filter.
    val filterAvailableBodies = remember(availableBodies, favoritesOnly, favorites.bodies) {
        if (favoritesOnly) availableBodies.filter { it in favorites.bodies } else availableBodies
    }
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
    var earthDistances by remember { mutableStateOf<Map<SpaceCompassCelestialBody, Double?>>(emptyMap()) }
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val catalogScroll = rememberLazyListState()
    var searchFocused by remember { mutableStateOf(false) }
    val quickHeader = rememberSpaceCompassQuickReturnHeader(pinned = searchFocused)
    val catalogScope = rememberCoroutineScope()
    val titleAction = Modifier.testTag("catalog-title").clickable(
        interactionSource = remember { MutableInteractionSource() }, indication = null,
        onClickLabel = stringResource(R.string.catalog_scroll_to_top)
    ) { quickHeader.show(); catalogScope.launch { catalogScroll.animateScrollToItem(0) } }
    LaunchedEffect(sort, types, visibility, origin, pointing, query, favoritesOnly) { quickHeader.show(); catalogScroll.scrollToItem(0) }
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
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    // Loading flags alone must not trigger catalog calculations.
    val catalogRemote = remember(remote.iss, remote.starlink, remote.satellites, remote.ephemerides, remote.motions) { remote }
    var observationSnapshot by remember { mutableStateOf<Pair<Any, Map<SpaceCompassCelestialBody, SpaceCompassCelestialObservation?>>?>(null) }
    val catalogLatitude = latitude?.let { round(it * 10000) / 10000 }
    val catalogLongitude = longitude?.let { round(it * 10000) / 10000 }
    val catalogAltitude = round(altitude / 10) * 10
    // Distance rows refresh every ten minutes; arriving models/location update immediately.
    val catalogTime = remember(timeMs / SPACE_COMPASS_CELESTIAL_CATALOG_REFRESH_MS,
        catalogRemote, catalogLatitude, catalogLongitude, catalogAltitude) { timeMs }
    // Keep horizon filtering on its existing minute cadence, independently of distance rows.
    val visibilityTime = remember(timeMs / 60_000L,
        catalogRemote, catalogLatitude, catalogLongitude, catalogAltitude) { timeMs }
    val observationKey = remember(availableBodies, visibilityTime, catalogRemote,
        catalogLatitude, catalogLongitude, catalogAltitude) { Any() }
    val observations = observationSnapshot?.takeIf { it.first === observationKey }?.second.orEmpty()
    LaunchedEffect(observationKey) {
        val calculated = withContext(Dispatchers.Default) {
            availableBodies.associateWith { body ->
                if (catalogLatitude == null || catalogLongitude == null) null else runCatching {
                    calculateSpaceCompassCelestialObservation(body, visibilityTime, catalogLatitude, catalogLongitude, catalogAltitude, catalogRemote)
                }.getOrNull()
            }
        }
        observationSnapshot = observationKey to calculated
    }
    LaunchedEffect(availableBodies, catalogTime, catalogRemote, catalogLatitude, catalogLongitude, catalogAltitude) {
        val snapshot = withContext(Dispatchers.Default) {
            val ranges = availableBodies.associateWith { body -> runCatching {
                when {
                    body == SpaceCompassCelestialBody.EARTH_CENTER ->
                        if (catalogLatitude == null || catalogLongitude == null) null else
                            calculateSpaceCompassCelestialObservation(body, catalogTime, catalogLatitude, catalogLongitude, catalogAltitude, catalogRemote)?.distanceKm
                    body.isEarthSatellite || body == SpaceCompassCelestialBody.MOON -> spaceCompassNearbyCatalogDistanceKm(body, catalogTime, catalogRemote)
                    else -> spaceCompassCatalogDisplaySolarDistanceAu(body, catalogTime, catalogRemote)
                }
            }.getOrNull() }
            val solar = availableBodies.associateWith { body ->
                runCatching { spaceCompassCatalogDisplaySolarDistanceAu(body, catalogTime, catalogRemote) }.getOrNull()
            }
            val earth = availableBodies.associateWith { body ->
                runCatching { spaceCompassCatalogDisplayEarthDistanceKm(body, catalogTime, catalogRemote) }.getOrNull()
            }
            Triple(ranges, solar, earth)
        }
        distances = snapshot.first
        solarDistances = snapshot.second
        earthDistances = snapshot.third
    }
    // Static distant references enter the correct sort position before network refresh.
    val readySolarDistances = spaceCompassCatalogDistancesWithReferences(solarDistances, availableBodies)
    val readyDistances = spaceCompassCatalogDistancesWithReferences(distances, availableBodies)
    val filteredBodies = remember(types, visibility, origin, pointing, observations, availableBodies) {
        spaceCompassFilterCatalog(types, visibility, observations.mapValues { it.value?.position?.elevationDegrees }, availableBodies,
            origin, pointing, observations.mapValues { spaceCompassCatalogHasPointing(it.value) })
    }
    val matchingBodies = remember(filteredBodies, localizedNames, query) {
        spaceCompassSearchCatalog(filteredBodies, localizedNames, query)
    }
    val sortedBodies = remember(availableBodies, sort, localizedNames, readySolarDistances, locale, earthDistances) {
        spaceCompassSortCatalog(availableBodies, sort, localizedNames, readySolarDistances, locale, earthDistances)
    }
    val visibleBodies = remember(sortedBodies, matchingBodies, selectedBodies, prioritySelected, favoritesOnly, favorites.bodies) {
        spaceCompassCatalogDisplayOrder(sortedBodies, matchingBodies.toSet(), selectedBodies, prioritySelected,
            favorites.bodies.takeIf { favoritesOnly })
    }
    val elevations = remember(observations) { observations.mapValues { it.value?.position?.elevationDegrees } }
    val pointingAvailability = remember(observations) { observations.mapValues { spaceCompassCatalogHasPointing(it.value) } }
    val searchScope = remember(availableBodies,localizedNames,query) {
        spaceCompassSearchCatalog(availableBodies,localizedNames,query).toSet()
    }
    val filterCounts = remember(filterAvailableBodies,selectedBodies,types,visibility,origin,pointing,
        elevations,pointingAvailability,searchScope) {
        spaceCompassCatalogFilterCounts(filterAvailableBodies,selectedBodies,types,visibility,origin,pointing,
            elevations,pointingAvailability,searchScope)
    }
    val checkedCount = visibleBodies.count { it in selectedBodies }
    val checkedState = when {
        checkedCount == 0 -> ToggleableState.Off
        checkedCount == visibleBodies.size -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    val darkTheme = background.luminance() < .4f
    val listBackground = spaceCompassSurfacePalette(darkTheme).island
    Surface(Modifier.fillMaxSize().testTag("celestial-menu"), color = listBackground, contentColor = color) {
        Column(Modifier.fillMaxSize()) {
            SpaceCompassPageToolbar(stringResource(R.string.pc_celestial_object, catalogCount), handleBack,
                modifier = Modifier.background(spaceCompassPageBackground()),
                titleColor = color, titleModifier = titleAction) {
                SpaceCompassCatalogSortButton(sort, color) {
                    updateCatalogPreferences(catalogPreferences.copy(sort = it))
                }
                SpaceCompassTitleActionButton(stringResource(R.string.catalog_filters),
                    onClick = { filtersOpen = !filtersOpen; quickHeader.show() },
                    modifier = Modifier.testTag("catalog-filter-toggle"),
                    iconColor = if (catalogPreferences.hasFilters || query.isNotBlank())
                        MaterialTheme.colorScheme.primary else color) {
                    Icon(painterResource(R.drawable.ic_filter), contentDescription = null)
                }
                SpaceCompassTitleActionButton(stringResource(if (favoritesOnly) R.string.catalog_favorites_off else R.string.catalog_favorites_only),
                    onClick = { updateCatalogPreferences(catalogPreferences.copy(favoritesOnly = !favoritesOnly)) },
                    modifier = Modifier.testTag("catalog-favorites"), selected = favoritesOnly,
                    iconColor = if (favoritesOnly) spaceCompassCelestialFavoriteColor(darkTheme) else color) {
                    SpaceCompassCelestialFavoriteIcon(favoritesOnly)
                }
            }
            // The open filter panel stays fixed; only the search/value header follows list gestures.
            AnimatedVisibility(filtersOpen, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                SpaceCompassCatalogFilterBar(types, visibility, origin, pointing, visibleCount = visibleBodies.size, counts = filterCounts, searchActive = query.isNotBlank(),
                    availableBodies = filterAvailableBodies, favoritesOnly = favoritesOnly,
                    onType = { type -> updateCatalogPreferences(catalogPreferences.copy(
                        types = if (type in types) types - type else types + type)) },
                    onAllTypes = { updateCatalogPreferences(catalogPreferences.copy(types = emptySet())) },
                    onVisibility = { updateCatalogPreferences(catalogPreferences.copy(visibility = it)) },
                    onOrigin = { updateCatalogPreferences(catalogPreferences.copy(origin = it)) },
                    onPointing = { updateCatalogPreferences(catalogPreferences.copy(pointing = it)) },
                    onReset = { query = ""; updateCatalogPreferences(catalogPreferences.copy(
                        types = emptySet(), visibility = SpaceCompassCatalogVisibility.ALL,
                        origin = SpaceCompassCatalogOrigin.ALL, pointing = SpaceCompassCatalogPointing.ALL, favoritesOnly = false)) })
            }
            SpaceCompassQuickReturnHeader(quickHeader, Modifier.testTag("catalog-quick-return-header")) {
            BoxWithConstraints(Modifier.fillMaxWidth().padding(end = 16.dp).testTag("catalog-fixed-header").padding(start = 12.dp, top = 4.dp, bottom = 4.dp)) {
                // Enlarge the previous equal-column field itself, excluding its unchanged outer spacing.
                val previousSearchWidth = ((maxWidth - 24.dp) / 2 - 20.dp).coerceAtLeast(0.dp)
                // Leave a readable value heading beside the search field on narrow phones.
                val searchWidth = (previousSearchWidth * 1.30f + 20.dp)
                    .coerceAtMost((maxWidth - 24.dp - 64.dp).coerceAtLeast(100.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(24.dp).height(48.dp), contentAlignment = Alignment.Center) {
                        TriStateCheckbox(checkedState, onClick = { onToggleAll(visibleBodies.toSet()) },
                            enabled = visibleBodies.isNotEmpty(), colors = spaceCompassCheckboxColors(),
                            modifier = Modifier.requiredSize(48.dp).testTag("celestial-select-all").semantics {
                                contentDescription = stringResourceForSelection
                            })
                    }
                    SpaceCompassCatalogSearchField(query, { query = it },
                        Modifier.width(searchWidth).padding(start = 12.dp, end = 8.dp)
                            .onFocusChanged { searchFocused = it.hasFocus })
                    Text(stringResource(valueField.label), Modifier.weight(1f).testTag("catalog-value-header"),
                        color = color.copy(alpha = 0.65f), fontSize = 11.sp, lineHeight = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
            }
            }
            LazyColumn(Modifier.fillMaxWidth().weight(1f).nestedScroll(quickHeader.connection)
                .clipToBounds().lazyScrollbarOverlay(catalogScroll, color.copy(alpha = .58f))
                .testTag("celestial-catalog-scroll"), state = catalogScroll) {
            if (visibleBodies.isEmpty()) item(key = "catalog-empty") {
                Text(stringResource(R.string.catalog_no_results), Modifier.fillMaxWidth().padding(16.dp),
                    color = color.copy(alpha = .65f), fontSize = 14.sp)
            }
            items(visibleBodies, key = { it.name }) { candidate ->
                val value = if (valueField == SpaceCompassCatalogSortField.EARTH_DISTANCE)
                    formatSpaceCompassCelestialCatalogEarthDistance(candidate, earthDistances[candidate], numeric, distanceUnit, units.feet)
                    else if (valueField != SpaceCompassCatalogSortField.DISTANCE)
                    formatSpaceCompassCatalogPhysicalValue(candidate, valueField, numeric, units, referenceLabel)
                    else if (sort.field == SpaceCompassCatalogSortField.DISTANCE)
                    formatSpaceCompassCatalogDisplayDistance(candidate, readySolarDistances[candidate], numeric, distanceUnit)
                    else if (readyDistances[candidate] == null && spaceCompassCelestialDataOutsideDate(candidate, catalogTime, catalogRemote))
                    stringResource(R.string.catalog_date_unavailable)
                    else if (candidate.isEarthSatellite || candidate == SpaceCompassCelestialBody.MOON || candidate == SpaceCompassCelestialBody.EARTH_CENTER)
                    (formatSpaceCompassSelectedDistance(candidate, readyDistances[candidate], numeric, distanceUnit, units.feet) ?: "—") + "\n" + (if (candidate == SpaceCompassCelestialBody.EARTH_CENTER) "GPS" else stringResource(R.string.celestial_view_earth))
                    else formatSpaceCompassCatalogDisplayDistance(candidate, readyDistances[candidate], numeric, distanceUnit)
                SpaceCompassCatalogObjectRow(candidate, localizedNames.getValue(candidate),
                    checked = candidate in selectedBodies, value = value, color = color,
                    rowBackground = listBackground,
                    favoriteTint = if (candidate in favorites.bodies) spaceCompassCelestialFavoriteColor(darkTheme) else null,
                    onToggle = { onSelect(candidate) },
                    onObserve = { focusManager.clearFocus(force = true); keyboard?.hide(); onObserve(candidate) })
            }
            }
        }
    }
}
