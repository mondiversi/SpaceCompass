package me.mondiversi.spacecompass

import androidx.compose.runtime.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Absolute minutes distinguish repeated DST hours; live observations have a separate identity. */
internal data class SpaceCompassOrbitPointKey(val body: SpaceCompassCelestialBody, val date: LocalDate,
    val zoneId: String, val minute: Long?, val event: SpaceCompassSunPathEvent?) {
    val isCurrent: Boolean get() = minute == null
}

internal data class SpaceCompassSelectedOrbitPoint(val key: SpaceCompassOrbitPointKey,
    val point: SpaceCompassSunPathPoint, val path: SpaceCompassSunDailyPath?)

internal fun spaceCompassOrbitPointKey(body: SpaceCompassCelestialBody, point: SpaceCompassSunPathPoint,
    zone: ZoneId, date: LocalDate? = null, current: Boolean = false) = SpaceCompassOrbitPointKey(body,
    date ?: Instant.ofEpochMilli(point.timeMs).atZone(zone).toLocalDate(), zone.id,
    if (current) null else Math.floorDiv(point.timeMs, 60_000L), if (current) null else point.event)

/** Small refinements of a named event must not erase its pin; repeated satellite events remain distinct. */
internal fun spaceCompassOrbitPointKeysMatch(a: SpaceCompassOrbitPointKey, b: SpaceCompassOrbitPointKey): Boolean =
    a == b || a.body == b.body && a.date == b.date && a.zoneId == b.zoneId && a.event == b.event &&
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
    val selectedPoints: List<SpaceCompassSelectedOrbitPoint> get() = selection.values.toList()

    fun openMenu(path: SpaceCompassSunDailyPath, currentPoint: SpaceCompassSunPathPoint? = null) {
        menuPath = path
        menuCurrentPoint = currentPoint
        menuFocusPointId = lastSelectedKey?.takeIf { it in selection && it.body == path.body }?.let { key ->
            spaceCompassSelectedDailyPathEntries(path, currentPoint, selectedPoints).firstOrNull { row ->
                spaceCompassOrbitPointKeysMatch(key, spaceCompassOrbitPointKey(path.body, row.point,
                    path.zone, path.date.takeUnless { row.isCurrent }, row.isCurrent))
            }?.pointId
        }
    }
    fun closeMenu() { menuPath = null; menuCurrentPoint = null; menuFocusPointId = null }

    fun isSelected(path: SpaceCompassSunDailyPath, point: SpaceCompassSunPathPoint, current: Boolean = false): Boolean =
        matchingKey(spaceCompassOrbitPointKey(path.body, point, path.zone, path.date.takeUnless { current }, current)) != null

    fun isCurrentSelected(body: SpaceCompassCelestialBody, point: SpaceCompassSunPathPoint, zone: ZoneId): Boolean =
        spaceCompassOrbitPointKey(body, point, zone, current = true) in selection

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
        }
        // Only explicit point toggles produce feedback; refresh/navigation/reset remain silent.
        onPointToggled(existing == null)
    }
    fun select(path: SpaceCompassSunDailyPath, point: SpaceCompassSunPathPoint) {
        if (valid(point)) add(SpaceCompassSelectedOrbitPoint(
            spaceCompassOrbitPointKey(path.body, point, path.zone, path.date), point, path))
    }
    fun togglePoint(path: SpaceCompassSunDailyPath, point: SpaceCompassSunPathPoint) {
        if (valid(point)) toggle(SpaceCompassSelectedOrbitPoint(
            spaceCompassOrbitPointKey(path.body, point, path.zone, path.date), point, path))
    }
    fun selectCurrent(body: SpaceCompassCelestialBody, point: SpaceCompassSunPathPoint,
        path: SpaceCompassSunDailyPath? = null, zone: ZoneId = path?.zone ?: ZoneId.systemDefault()) {
        val normalized = spaceCompassCurrentPathPoint(body, point.timeMs, point.position) ?: return
        add(SpaceCompassSelectedOrbitPoint(spaceCompassOrbitPointKey(body, normalized, zone, current = true), normalized, path))
    }
    fun toggleCurrent(body: SpaceCompassCelestialBody, point: SpaceCompassSunPathPoint,
        path: SpaceCompassSunDailyPath? = null, zone: ZoneId = path?.zone ?: ZoneId.systemDefault()) {
        val normalized = spaceCompassCurrentPathPoint(body, point.timeMs, point.position) ?: return
        toggle(SpaceCompassSelectedOrbitPoint(spaceCompassOrbitPointKey(body, normalized, zone, current = true), normalized, path))
    }
    fun toggleScenePoint(target: SpaceCompassCelestialSceneTarget, zone: ZoneId) {
        if (target.isCurrent) toggleCurrent(target.body, target.point, target.path, target.path?.zone ?: zone)
        else target.path?.let { togglePoint(it, target.point) }
    }
    fun toggleMenuPoint(pointId: String) {
        val path = menuPath ?: return
        if (pointId == "current") menuCurrentPoint?.let { toggleCurrent(path.body, it, path) }
        else pointId.toIntOrNull()?.let { path.markers.getOrNull(it) }?.let { togglePoint(path, it) }
    }
    fun clearSelection() { selection.clear(); lastSelectedKey = null }

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

@Composable
internal fun rememberSpaceCompassSunDailyPathUiState(): SpaceCompassSunDailyPathUiState {
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
    perspective: SpaceCompassPerspective? = null): List<SpaceCompassCelestialSceneTarget> {
    if (orientation == null || width <= 0 || height <= 0 || !width.isFinite() || !height.isFinite()) return emptyList()
    return selected.sortedWith(compareBy({ it.key.body.ordinal }, { it.point.timeMs }, { it.key.event?.ordinal ?: -1 })).mapNotNull { saved ->
        val body = saved.key.body
        val path = paths[body]
        val compatiblePath = path?.takeIf { it.date == saved.key.date && it.zone.id == saved.key.zoneId }
        val refreshedMarker = compatiblePath?.markers?.firstOrNull {
            spaceCompassOrbitPointKeysMatch(spaceCompassOrbitPointKey(body, it, compatiblePath.zone, compatiblePath.date), saved.key)
        }
        val point = if (saved.key.isCurrent) current[body]?.takeIf {
            spaceCompassOrbitPointKey(body, it, path?.zone ?: ZoneId.of(saved.key.zoneId), current = true) == saved.key
        } else compatiblePath?.let { refreshedMarker ?: saved.point }
        point ?: return@mapNotNull null
        projectSpaceCompassSun(point.position, orientation, width, height, perspective).takeIf { it.visible }?.let {
            SpaceCompassCelestialSceneTarget(body, point, SpaceCompassSunScenePoint(it.x, it.y), if (!saved.key.isCurrent && refreshedMarker == null) saved.path else path ?: saved.path, saved.key.isCurrent)
        }
    }
}
