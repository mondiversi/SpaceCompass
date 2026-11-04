package me.mondiversi.spacecompass

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import java.io.StringReader
import java.io.StringWriter
import java.time.ZoneId
import java.util.Properties
import kotlinx.coroutines.flow.collectLatest

@Stable
internal class SpaceCompassTemporalComparisonState(
    private val points: SpaceCompassSunDailyPathUiState,
    private val onActiveComparisonRemoved: (SpaceCompassCelestialBody) -> Unit = {}
) {
    var entries by mutableStateOf<List<SpaceCompassTemporalComparison>>(emptyList())
        private set
    var activeId by mutableStateOf<String?>(null)
    var personalMoments by mutableStateOf<List<SpaceCompassPersonalMoment>>(emptyList())
        private set

    fun sceneEntries(zone: ZoneId): List<SpaceCompassTemporalComparison> =
        entries + personalMoments.filter { it.enabled }.sortedWith(spaceCompassPersonalMomentOrder).map { it.resolve(zone) }

    fun visibleEntries(selectedBodies: Set<SpaceCompassCelestialBody>, zone: ZoneId = ZoneId.systemDefault()) =
        sceneEntries(zone).filter { it.body in selectedBodies }

    fun canAddPersonalMoment(moment: SpaceCompassPersonalMoment) =
        personalMoments.size < SPACE_COMPASS_PERSONAL_MOMENT_LIMIT && personalMoments.none { it.id == moment.id }

    fun addPersonalMoment(moment: SpaceCompassPersonalMoment) {
        if (canAddPersonalMoment(moment)) personalMoments = personalMoments + moment.copy(enabled = false)
    }

    fun togglePersonalMoment(id: String) {
        val moment = personalMoments.firstOrNull { it.id == id } ?: return
        if (moment.enabled) remove(id) else {
            personalMoments = personalMoments.map { if (it.id == id) it.copy(enabled = true) else it }
            activeId = id
        }
    }

    fun toggleAllPersonalMoments(body: SpaceCompassCelestialBody) {
        val moments = personalMoments.filter { it.body == body }
        if (moments.isEmpty()) return
        val enabled = moments.filter { it.enabled }
        if (enabled.isNotEmpty()) disablePersonalMoments(enabled) else {
            personalMoments = personalMoments.map { if (it.body == body) it.copy(enabled = true) else it }
            activeId = moments.maxWithOrNull(spaceCompassPersonalMomentOrder)?.id
        }
    }

    fun importPersonalMoments(moments: List<SpaceCompassPersonalMoment>): Int {
        val merged = mergeSpaceCompassPersonalMoments(personalMoments, moments)
        val added = merged.size - personalMoments.size
        personalMoments = merged
        return added
    }

    fun deletePersonalMoment(id: String) {
        remove(id)
        personalMoments = personalMoments.filterNot { it.id == id }
    }

    fun count(body: SpaceCompassCelestialBody, source: SpaceCompassComparisonSource) =
        entries.count { it.body == body && it.source == source }
    fun selected(body: SpaceCompassCelestialBody, source: SpaceCompassComparisonSource, origin: String) =
        entries.any { it.body == body && it.source == source && it.origin == origin }
    fun toggle(body: SpaceCompassCelestialBody, timeMs: Long, zone: ZoneId,
        source: SpaceCompassComparisonSource, origin: String, annualExtreme: Boolean = false) {
        val old = entries.firstOrNull { it.body == body && it.source == source && it.origin == origin }
        if (old != null) remove(old.id) else {
            if (entries.size >= 512) return
            val added = SpaceCompassTemporalComparison.create(body, timeMs, zone, source, origin, annualExtreme)
            entries = entries + added
            activeId = added.id
        }
    }
    /** One page/year is toggled atomically; pins belonging to removed comparisons are pruned together. */
    fun toggleAll(candidates: List<SpaceCompassTemporalComparison>) {
        val choices = candidates.distinctBy { it.id }
        if (choices.isEmpty()) return
        val ids = choices.map { it.id }.toSet()
        val removed = entries.filter { it.id in ids }
        if (removed.isNotEmpty()) {
            removeComparisons(removed)
        } else if (entries.size + choices.size <= 512) {
            entries = entries + choices
            activeId = choices.last().id
        }
    }
    fun canToggleAll(candidates: List<SpaceCompassTemporalComparison>): Boolean {
        val ids = candidates.map { it.id }.toSet()
        return ids.isNotEmpty() && (entries.any { it.id in ids } || entries.size + ids.size <= 512)
    }
    fun remove(id: String) {
        val comparison = entries.firstOrNull { it.id == id }
        if (comparison != null) {
            removeComparisons(listOf(comparison))
        } else {
            val personal = personalMoments.firstOrNull { it.id == id && it.enabled } ?: return
            disablePersonalMoments(listOf(personal))
        }
    }

    private fun disablePersonalMoments(enabled: List<SpaceCompassPersonalMoment>) {
        val ids = enabled.mapTo(hashSetOf()) { it.id }
        val active = enabled.firstOrNull { it.id == activeId }
        personalMoments = personalMoments.map { if (it.id in ids) it.copy(enabled = false) else it }
        enabled.forEach { points.removeComparison(it.id) }
        if (active != null) {
            activeId = null
            onActiveComparisonRemoved(active.body)
        }
    }

    private fun removeComparisons(removed: List<SpaceCompassTemporalComparison>) {
        val ids = removed.map { it.id }.toSet()
        val active = removed.firstOrNull { it.id == activeId }
        entries = entries.filterNot { it.id in ids }
        removed.forEach { points.removeComparison(it.id) }
        if (active != null) {
            activeId = null
            onActiveComparisonRemoved(active.body)
        }
    }
    fun restore(values: List<SpaceCompassTemporalComparison>?, active: String?,
        moments: List<SpaceCompassPersonalMoment>? = null) {
        if (values == null && moments == null) return
        val merged = mergeSpaceCompassPersonalMoments(personalMoments, moments)
        if (values != null) {
            require(values.size <= 512 && values.distinctBy { it.id }.size == values.size)
            entries.filterNot { old -> values.any { it.id == old.id } }.forEach { points.removeComparison(it.id) }
            entries = values
        }
        personalMoments = merged
        activeId = active?.takeIf { id -> entries.any { it.id == id } || merged.any { it.id == id && it.enabled } }
    }
}

internal val LocalSpaceCompassComparisons = staticCompositionLocalOf<SpaceCompassTemporalComparisonState?> { null }

/** Capture the selected group for this visit while keeping the checkbox state live. */
@Composable
internal fun rememberSpaceCompassComparisonPriority(body: SpaceCompassCelestialBody,
    source: SpaceCompassComparisonSource): Set<String> {
    val comparisons = LocalSpaceCompassComparisons.current
    val origins = rememberSaveable(body, source) {
        comparisons?.entries.orEmpty().filter { it.body == body && it.source == source }.map { it.origin }
    }
    return remember(origins) { origins.toSet() }
}

/** Only canonical values/pins persist; calculated curves are rebuilt for the current observer. */
@Composable
internal fun rememberSpaceCompassTemporalComparisons(points: SpaceCompassSunDailyPathUiState): SpaceCompassTemporalComparisonState {
    val context = LocalContext.current
    val preferences = LocalSpaceCompassPreferences.current ?: remember(context) {
        context.getSharedPreferences(SPACE_COMPASS_PREFERENCES_NAME, android.content.Context.MODE_PRIVATE)
    }
    val state = remember(preferences, points) {
        SpaceCompassTemporalComparisonState(points) { parent ->
            val selection = readSpaceCompassCelestialSelection(preferences)
            if (parent in selection.selected && parent != selection.active) {
                preferences.edit().putSpaceCompassCelestialSelection(selection.copy(active = parent)).apply()
            }
        }.also { state ->
            runCatching {
                val p = Properties().apply { load(StringReader(preferences.getString("temporal_comparisons_v1", "").orEmpty())) }
                val saved = p.readSpaceCompassComparisons().orEmpty()
                val moments = p.readSpaceCompassPersonalMoments()
                val active = p.getProperty("activePersonalMoment") ?: p.getProperty("activeComparison")
                val pins = p.readSpaceCompassComparisonPoints(saved) + p.readSpaceCompassPersonalPins(moments.orEmpty())
                state.restore(saved, active, moments)
                if (pins.isNotEmpty()) points.restoreImportedPoints(points.selectedPoints + pins)
            }
        }
    }
    LaunchedEffect(state, preferences) {
        snapshotFlow {
            Triple(state.entries, state.activeId, points.selectedPoints.filter { it.key.comparisonId != null }) to state.personalMoments
        }.collectLatest { (legacy, moments) ->
            val (entries, active, pins) = legacy
            val legacyIds = entries.mapTo(hashSetOf()) { it.id }
            val properties = Properties().apply {
                putSpaceCompassComparisons(entries, pins.filter { it.key.comparisonId in legacyIds },
                    active.takeIf { it in legacyIds }, null)
                putSpaceCompassPersonalMoments(moments, pins, active)
            }
            val text = StringWriter().also { properties.store(it, null) }.toString()
            preferences.edit().putString("temporal_comparisons_v1", text).apply()
        }
    }
    return state
}
