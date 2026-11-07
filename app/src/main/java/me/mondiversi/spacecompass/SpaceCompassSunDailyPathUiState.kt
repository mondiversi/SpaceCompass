package me.mondiversi.spacecompass

import androidx.compose.runtime.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Absolute minutes distinguish repeated DST hours; live observations have a separate identity. */
internal data class SpaceCompassOrbitPointKey(val body: SpaceCompassCelestialBody, val date: LocalDate,
    val zoneId: String, val minute: Long?, val event: SpaceCompassSunPathEvent?,
    val approachBody: SpaceCompassCelestialBody? = null, val comparisonId: String? = null) {
    val isCurrent: Boolean get() = minute == null
}

internal data class SpaceCompassSelectedOrbitPoint(val key: SpaceCompassOrbitPointKey,
    val point: SpaceCompassSunPathPoint, val path: SpaceCompassSunDailyPath?)

internal fun spaceCompassOrbitPointKey(body: SpaceCompassCelestialBody, point: SpaceCompassSunPathPoint,
    zone: ZoneId, date: LocalDate? = null, current: Boolean = false, comparisonId: String? = null) = SpaceCompassOrbitPointKey(body,
    date ?: Instant.ofEpochMilli(point.timeMs).atZone(zone).toLocalDate(), zone.id,
    if (current) null else Math.floorDiv(point.timeMs, 60_000L), if (current) null else point.event, point.approach?.other, comparisonId)

/** Small refinements of a named event must not erase its pin; repeated satellite events remain distinct. */
internal fun spaceCompassOrbitPointKeysMatch(a: SpaceCompassOrbitPointKey, b: SpaceCompassOrbitPointKey): Boolean =
    a == b || a.comparisonId == b.comparisonId && a.body == b.body && a.date == b.date && a.zoneId == b.zoneId && a.event == b.event && a.approachBody == b.approachBody &&
        a.event != null && a.event != SpaceCompassSunPathEvent.HOUR && a.minute != null && b.minute != null &&
        kotlin.math.abs(a.minute - b.minute) <= 2

/** One selection owner for every orbit; opening/closing a page never clears pinned points. */
@Stable
internal class SpaceCompassSunDailyPathUiState(
    private val onPointToggled: (selected: Boolean) -> Unit = {}
) {
    var menuPath by mutableStateOf<SpaceCompassSunDailyPath?>(null)
        private set
    var menuCurrentPoint by mutableStateOf<SpaceCompassSunPathPoint?>(null)
        private set
    var menuFocusPointId by mutableStateOf<String?>(null)
        private set
    private val selection = mutableStateMapOf<SpaceCompassOrbitPointKey, SpaceCompassSelectedOrbitPoint>()
    private var lastSelectedKey: SpaceCompassOrbitPointKey? = null
    private var observationDate: LocalDate? = null
    val selectedPoints: List<SpaceCompassSelectedOrbitPoint> get() = selection.values.toList()
    var antipodesVisible by mutableStateOf(false)
        private set

    fun toggleAntipodes() { antipodesVisible = selection.isNotEmpty() && !antipodesVisible }

    /** A new effective civil day expires only live-track pins, never fixed comparison moments. */
    fun refreshObservationDate(date: LocalDate) {
        val changed = observationDate?.let { it != date } == true
        observationDate = date
        if (!changed) return
        selection.keys.filter { it.comparisonId == null }.forEach(selection::remove)
        if (lastSelectedKey !in selection) lastSelectedKey = selection.keys.lastOrNull()
        if (menuPath?.comparison == null) menuFocusPointId = null
        if (selection.isEmpty()) antipodesVisible = false
    }

    fun openMenu(path: SpaceCompassSunDailyPath, currentPoint: SpaceCompassSunPathPoint? = null) {
        menuPath = path
        menuCurrentPoint = currentPoint
        menuFocusPointId = lastSelectedKey?.takeIf { it in selection && it.body == path.body && it.comparisonId == path.comparison?.id }?.let { key ->
            spaceCompassSelectedDailyPathEntries(path, currentPoint, selectedPoints).firstOrNull { row ->
                spaceCompassOrbitPointKeysMatch(key, spaceCompassOrbitPointKey(path.body, row.point,
                    path.zone, spaceCompassPathSelectionDate(path,row.point).takeUnless { row.isCurrent }, row.isCurrent, path.comparison?.id))
            }?.pointId
        }
    }
    fun closeMenu() { menuPath = null; menuCurrentPoint = null; menuFocusPointId = null }
    fun refreshMenu(path: SpaceCompassSunDailyPath?, current: SpaceCompassSunPathPoint?) {
        if (menuPath != null && path?.body == menuPath?.body && path?.comparison?.id == menuPath?.comparison?.id) { menuPath = path; menuCurrentPoint = current }
    }

    fun isSelected(path: SpaceCompassSunDailyPath, point: SpaceCompassSunPathPoint, current: Boolean = false): Boolean =
        matchingKey(spaceCompassOrbitPointKey(path.body, point, path.zone, spaceCompassPathSelectionDate(path,point).takeUnless { current }, current, path.comparison?.id)) != null

    fun isCurrentSelected(body: SpaceCompassCelestialBody, point: SpaceCompassSunPathPoint, zone: ZoneId, comparisonId: String? = null): Boolean =
        spaceCompassOrbitPointKey(body, point, zone, current = true, comparisonId = comparisonId) in selection

    private fun valid(point: SpaceCompassSunPathPoint) = point.position.azimuthDegrees.isFinite() &&
        point.position.elevationDegrees.isFinite() && point.position.elevationDegrees in -90.0..90.0

    private fun matchingKey(key: SpaceCompassOrbitPointKey): SpaceCompassOrbitPointKey? =
        if (key in selection) key else selection.keys.firstOrNull { spaceCompassOrbitPointKeysMatch(it, key) }

    private fun add(record: SpaceCompassSelectedOrbitPoint) {
        matchingKey(record.key)?.takeUnless { it == record.key }?.let(selection::remove)
        selection[record.key] = record
        lastSelectedKey = record.key
    }
    private fun toggle(record: SpaceCompassSelectedOrbitPoint) {
        val existing = matchingKey(record.key)
        if (existing == null) add(record)
        else {
            selection.remove(existing)
            if (lastSelectedKey == existing) lastSelectedKey = selection.keys.lastOrNull()
            if (selection.isEmpty()) antipodesVisible = false
        }
        // Only explicit point toggles produce feedback; refresh/navigation/reset remain silent.
        onPointToggled(existing == null)
    }
    fun select(path: SpaceCompassSunDailyPath, point: SpaceCompassSunPathPoint) {
        if (valid(point)) add(SpaceCompassSelectedOrbitPoint(
            spaceCompassOrbitPointKey(path.body, point, path.zone, spaceCompassPathSelectionDate(path,point), comparisonId = path.comparison?.id), point, path))
    }
    fun togglePoint(path: SpaceCompassSunDailyPath, point: SpaceCompassSunPathPoint) {
        if (valid(point)) toggle(SpaceCompassSelectedOrbitPoint(
            spaceCompassOrbitPointKey(path.body, point, path.zone, spaceCompassPathSelectionDate(path,point), comparisonId = path.comparison?.id), point, path))
    }
    fun selectCurrent(body: SpaceCompassCelestialBody, point: SpaceCompassSunPathPoint,
        path: SpaceCompassSunDailyPath? = null, zone: ZoneId = path?.zone ?: ZoneId.systemDefault()) {
        val normalized = spaceCompassCurrentPathPoint(body, point.timeMs, point.position) ?: return
        add(SpaceCompassSelectedOrbitPoint(spaceCompassOrbitPointKey(body, normalized, zone, current = true, comparisonId = path?.comparison?.id), normalized, path))
    }
    fun toggleCurrent(body: SpaceCompassCelestialBody, point: SpaceCompassSunPathPoint,
        path: SpaceCompassSunDailyPath? = null, zone: ZoneId = path?.zone ?: ZoneId.systemDefault()) {
        val normalized = spaceCompassCurrentPathPoint(body, point.timeMs, point.position) ?: return
        toggle(SpaceCompassSelectedOrbitPoint(spaceCompassOrbitPointKey(body, normalized, zone, current = true, comparisonId = path?.comparison?.id), normalized, path))
    }
    fun toggleScenePoint(target: SpaceCompassCelestialSceneTarget, zone: ZoneId) {
        val source = target.antipodeOf ?: target.point
        val path = target.path ?: target.comparison?.let { c -> SpaceCompassSunDailyPath(
            Instant.ofEpochMilli(c.timeMs).atZone(c.zone).toLocalDate(), c.zone, listOf(source), emptyList(), c.body, comparison = c) }
        if (target.isCurrent) toggleCurrent(target.body, source, path, path?.zone ?: zone)
        else path?.let { togglePoint(it, source) }
    }
    fun toggleMenuPoint(pointId: String) {
        val path = menuPath ?: return
        if (pointId == "current") menuCurrentPoint?.let { toggleCurrent(path.body, it, path) }
        else pointId.toIntOrNull()?.let { path.markers.getOrNull(it) }?.let { togglePoint(path, it) }
    }
    fun removeComparison(id: String) {
        selection.keys.filter { it.comparisonId == id }.forEach(selection::remove)
        if (lastSelectedKey !in selection) lastSelectedKey = selection.keys.lastOrNull()
        if (selection.isEmpty()) antipodesVisible = false
        if (menuPath?.comparison?.id == id) closeMenu()
    }
    fun restoreAntipodes(visible: Boolean) { antipodesVisible = visible && selection.isNotEmpty() }
    fun clearSelection() { selection.clear(); lastSelectedKey = null; antipodesVisible = false }

    /** Missing legacy payloads preserve pins; an explicit list replaces them without haptic feedback. */
    fun restoreImportedPoints(points: List<SpaceCompassSelectedOrbitPoint>?) {
        if (points == null) return
        points.forEach(::spaceCompassCheckLocationPoint)
        require(points.map { it.key }.toSet().size == points.size)
        clearSelection()
        closeMenu()
        points.forEach { add(it.copy(path = null)) }
    }

    /** Explicit navigation helpers: sky taps use toggleScenePoint instead. */
    fun openPoint(path: SpaceCompassSunDailyPath, point: SpaceCompassSunPathPoint,
        currentPoint: SpaceCompassSunPathPoint? = null) {
        select(path, point)
        openMenu(path, currentPoint)
    }
    fun openCurrent(body: SpaceCompassCelestialBody, point: SpaceCompassSunPathPoint,
        path: SpaceCompassSunDailyPath? = null, zone: ZoneId = path?.zone ?: ZoneId.systemDefault()) {
        val normalized = spaceCompassCurrentPathPoint(body, point.timeMs, point.position) ?: return
        selectCurrent(body, normalized, path, zone)
        val usable = path?.takeIf { it.body == body && spaceCompassDailyPathListEntries(it, normalized).any { row -> row.isCurrent } }
            ?: SpaceCompassSunDailyPath(Instant.ofEpochMilli(point.timeMs).atZone(zone).toLocalDate(), zone,
                listOf(normalized), emptyList(), body)
        openMenu(usable, normalized)
    }
}

internal val LocalSpaceCompassOrbitSelection = staticCompositionLocalOf<SpaceCompassSunDailyPathUiState?> { null }

@Composable
internal fun rememberSpaceCompassSunDailyPathUiState(): SpaceCompassSunDailyPathUiState {
    LocalSpaceCompassOrbitSelection.current?.let { return it }
    val haptics = rememberSpaceCompassHapticController()
    return remember(haptics) {
        SpaceCompassSunDailyPathUiState { selected ->
            haptics.spaceCompassHaptic(if (selected) SpaceCompassHapticCue.TOGGLE_ON else SpaceCompassHapticCue.TOGGLE_OFF)
        }
    }
}

/** Resolve refreshed data without changing selection identity or retaining a stale live location. */
internal fun projectSpaceCompassSelectedOrbitTargets(selected: List<SpaceCompassSelectedOrbitPoint>,
    paths: Map<SpaceCompassCelestialBody, SpaceCompassSunDailyPath>, current: Map<SpaceCompassCelestialBody, SpaceCompassSunPathPoint>,
    orientation: SpaceCompassSunOrientation?, width: Double, height: Double,
    perspective: SpaceCompassPerspective? = null, antipodes: Boolean = false,
    comparisons: List<SpaceCompassComparisonOverlay> = emptyList()): List<SpaceCompassCelestialSceneTarget> {
    if (orientation == null || width <= 0 || height <= 0 || !width.isFinite() || !height.isFinite()) return emptyList()
    return selected.sortedWith(compareBy({ it.key.body.ordinal }, { it.point.timeMs }, { it.key.event?.ordinal ?: -1 })).mapNotNull { saved ->
        val body = saved.key.body
        val comparison = saved.key.comparisonId?.let { id -> comparisons.firstOrNull { it.comparison.id == id } }
        if (saved.key.comparisonId != null && comparison == null) return@mapNotNull null
        val path = if (comparison == null) paths[body] else comparison.overlay.path
        val livePoint = if (comparison == null) current[body] else comparison.overlay.observation?.position?.let {
            SpaceCompassSunPathPoint(comparison.comparison.timeMs, it)
        }
        val compatiblePath = path?.takeIf { spaceCompassPathAcceptsSelection(it,saved) }
        val refreshedMarker = compatiblePath?.markers?.firstOrNull {
            spaceCompassOrbitPointKeysMatch(spaceCompassOrbitPointKey(body, it, compatiblePath.zone, spaceCompassPathSelectionDate(compatiblePath,it), comparisonId = saved.key.comparisonId), saved.key)
        }
        val point = if (saved.key.isCurrent) livePoint?.takeIf {
            spaceCompassOrbitPointKey(body, it, path?.zone ?: ZoneId.of(saved.key.zoneId), current = true, comparisonId = saved.key.comparisonId) == saved.key
        } else compatiblePath?.let { refreshedMarker ?: saved.point }
        point ?: return@mapNotNull null
        if (!point.position.azimuthDegrees.isFinite() || !point.position.elevationDegrees.isFinite() ||
            point.position.elevationDegrees !in -90.0..90.0) return@mapNotNull null
        // Mirror before projecting: a source behind the observer can have a visible antipode.
        val displayed = if (antipodes) point.antipodalDirection() else point
        projectSpaceCompassSun(displayed.position, orientation, width, height, perspective).takeIf { it.visible }?.let {
            SpaceCompassCelestialSceneTarget(body, displayed, SpaceCompassSunScenePoint(it.x, it.y),
                if (!saved.key.isCurrent && refreshedMarker == null) saved.path ?: compatiblePath else path ?: saved.path,
                saved.key.isCurrent, point.takeIf { antipodes }, comparison?.comparison)
        }
    }
}
