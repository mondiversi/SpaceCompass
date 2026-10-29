package me.mondiversi.spacecompass

import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Identity tracks freshness without comparing catalogue-sized maps on the UI thread. */
internal class SpaceCompassCatalogQuery(
    val preferences: SpaceCompassCatalogPreferences,
    val search: String,
    val selected: Set<SpaceCompassCelestialBody>,
    val prioritySelected: Set<SpaceCompassCelestialBody>,
    val favorites: Set<SpaceCompassCelestialBody>,
    val observations: Map<SpaceCompassCelestialBody, SpaceCompassCelestialObservation?>,
    val distances: SpaceCompassCatalogDistances)

internal class SpaceCompassCatalogPresentation(
    val query: SpaceCompassCatalogQuery,
    val names: Map<SpaceCompassCelestialBody, String>,
    val visible: List<SpaceCompassCelestialBody>,
    val checkedCount: Int,
    val displayedDistances: Map<SpaceCompassCelestialBody, Double?>,
    val solarDistances: Map<SpaceCompassCelestialBody, Double?>,
    val counts: SpaceCompassCatalogFilterCounts?,
    val typeOrder: List<SpaceCompassCatalogTypeCount>)

/** One visit owns its immutable index and bounded caches. No catalogue-sized work runs in composition.
 * The mutex also keeps cancelled/rapidly replaced queries from racing over the derived caches. */
internal class SpaceCompassCatalogPresenter private constructor(
    private val bodies: List<SpaceCompassCelestialBody>,
    private val names: Map<SpaceCompassCelestialBody, String>,
    private val locale: Locale,
    private val typeLabels: Map<SpaceCompassCatalogType, String>) {
    private val searchIndex = SpaceCompassCatalogSearchIndex(bodies, names)
    private val mutex = Mutex()
    private var lastSearch: String? = null
    private var searchMatches: Set<SpaceCompassCelestialBody> = emptySet()
    private var lastDistances: SpaceCompassCatalogDistances? = null
    private var displayed: Map<SpaceCompassCelestialBody, Double?> = emptyMap()
    private var solar: Map<SpaceCompassCelestialBody, Double?> = emptyMap()
    private var lastSort: SpaceCompassCatalogSort? = null
    private var lastSortDistances: Map<SpaceCompassCelestialBody, Double?>? = null
    private var sorted: List<SpaceCompassCelestialBody> = emptyList()
    private var lastObservations: Map<SpaceCompassCelestialBody, SpaceCompassCelestialObservation?>? = null
    private var elevations: Map<SpaceCompassCelestialBody, Double?> = emptyMap()
    private var pointing: Map<SpaceCompassCelestialBody, Boolean> = emptyMap()
    private var lastTypeScope: List<SpaceCompassCelestialBody>? = null
    private var typeOrder: List<SpaceCompassCatalogTypeCount> = emptyList()

    suspend fun present(query: SpaceCompassCatalogQuery, countsNeeded: Boolean): SpaceCompassCatalogPresentation =
        withContext(Dispatchers.Default) { mutex.withLock {
            currentCoroutineContext().ensureActive()
            val preferences = query.preferences
            if (lastDistances !== query.distances) {
                displayed = spaceCompassCatalogDistancesWithReferences(query.distances.displayed, bodies)
                solar = spaceCompassCatalogDistancesWithReferences(query.distances.solar, bodies)
                lastDistances = query.distances
            }
            val sortDistances = when (preferences.sort.field) {
                SpaceCompassCatalogSortField.DISTANCE -> solar
                SpaceCompassCatalogSortField.EARTH_DISTANCE -> query.distances.earth
                else -> null
            }
            if (lastSort != preferences.sort || lastSortDistances !== sortDistances) {
                sorted = spaceCompassSortCatalog(bodies, preferences.sort, names, solar, locale, query.distances.earth)
                lastSort = preferences.sort
                lastSortDistances = sortDistances
            }
            currentCoroutineContext().ensureActive()
            if (lastSearch != query.search) {
                searchMatches = searchIndex.search(query.search).toSet()
                lastSearch = query.search
            }
            if (lastObservations !== query.observations) {
                elevations = query.observations.mapValues { it.value?.position?.elevationDegrees }
                pointing = query.observations.mapValues { spaceCompassCatalogHasPointing(it.value) }
                lastObservations = query.observations
            }
            val matching = spaceCompassFilterCatalog(preferences.types, preferences.visibility, elevations,
                bodies, preferences.origin, preferences.pointing, pointing).filter { it in searchMatches }.toSet()
            val visible = spaceCompassCatalogDisplayOrder(sorted, matching, query.selected, query.prioritySelected,
                query.favorites.takeIf { preferences.favoritesOnly })
            currentCoroutineContext().ensureActive()
            val counts = if (countsNeeded) {
                val scope = if (preferences.favoritesOnly) bodies.filter { it in query.favorites } else bodies
                if (lastTypeScope != scope) {
                    typeOrder = spaceCompassCatalogTypeCounts(scope, typeLabels, locale)
                    lastTypeScope = scope
                }
                spaceCompassCatalogFilterCounts(scope, query.selected, preferences.types, preferences.visibility,
                    preferences.origin, preferences.pointing, elevations, pointing, searchMatches)
            } else null
            currentCoroutineContext().ensureActive()
            SpaceCompassCatalogPresentation(query, names, visible, visible.count { it in query.selected },
                displayed, solar, counts, if (countsNeeded) typeOrder else emptyList())
        } }

    companion object {
        /** Resolve Android resources and normalize multilingual aliases on a worker as well. */
        suspend fun create(bodies: List<SpaceCompassCelestialBody>, locale: Locale,
            name: (SpaceCompassCelestialBody) -> String,
            typeLabel: (SpaceCompassCatalogType) -> String): SpaceCompassCatalogPresenter = withContext(Dispatchers.Default) {
            val names = buildMap {
                bodies.forEachIndexed { index, body ->
                    if (index % 128 == 0) currentCoroutineContext().ensureActive()
                    put(body, name(body))
                }
            }
            SpaceCompassCatalogPresenter(bodies, names, locale, SpaceCompassCatalogType.entries.associateWith(typeLabel))
        }
    }
}
