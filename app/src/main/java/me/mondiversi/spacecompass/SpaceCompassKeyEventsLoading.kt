package me.mondiversi.spacecompass

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

internal data class SpaceCompassKeyEventsResult(val request: SpaceCompassObservationRequest,
    val events: List<SpaceCompassKeyEvent>?)

internal class SpaceCompassKeyEventsCache(private val limit: Int = 16) {
    // Reuse the reviewed exact-manual / stable-GPS anchoring policy from Key elevations.
    private val requests = SpaceCompassObservationMomentsCache(limit)
    private val results = LinkedHashMap<SpaceCompassObservationRequest, SpaceCompassKeyEventsResult>(16,.75f,true)
    fun request(calendar: SpaceCompassObservationCalendar, place: SpaceCompassObservationPlace,
        orbit: SpaceCompassSatelliteOrbit? = null, ephemeris: SpaceCompassHorizonsEphemeris? = null) =
        requests.request(calendar, place, orbit, ephemeris)
    fun get(request: SpaceCompassObservationRequest?) = request?.let { results[it] }
    fun put(result: SpaceCompassKeyEventsResult) {
        results[result.request] = result
        while (results.size > limit) results.remove(results.keys.first())
    }
}

internal class SpaceCompassKeyEventsViewModel : ViewModel() { val cache = SpaceCompassKeyEventsCache() }
internal data class SpaceCompassKeyEventsUiState(val events: List<SpaceCompassKeyEvent>?,
    val complete: Boolean, val hasLocation: Boolean, val calculationRequest: SpaceCompassObservationRequest? = null) {
    val totalRows: Int? get() = events?.size ?: 0.takeIf { complete }
}

/** Keyed by body/year/Location/data, never orientation, sensor ticks or the live clock. */
@Composable
internal fun rememberSpaceCompassKeyEvents(calendar: SpaceCompassObservationCalendar,
    latitude: Double?, longitude: Double?, altitude: Double,
    remote: SpaceCompassCelestialRemoteData): SpaceCompassKeyEventsUiState {
    val context = LocalContext.current
    val plan = LocalSpaceCompassObserver.current?.plan
    val owner = LocalActivity.current as? ViewModelStoreOwner
    val cache = remember(owner) {
        if (owner == null) SpaceCompassKeyEventsCache()
        else ViewModelProvider(owner)[SpaceCompassKeyEventsViewModel::class.java].cache
    }
    val request = if (latitude == null || longitude == null) null else cache.request(calendar,
        SpaceCompassObservationPlace(latitude, longitude, altitude, plan?.simulatePosition == true,
            plan?.simulateAltitude == true || plan?.simulatePosition == true),
        remote.satelliteOrbit(calendar.body), remote.ephemerides[calendar.body])
    val cached = cache.get(request)
    val computed by produceState<SpaceCompassKeyEventsResult?>(cached, request) {
        if (request == null) return@produceState
        cache.get(request)?.let { value = it; return@produceState }
        val events = withContext(Dispatchers.Default) {
            val job = coroutineContext
            try { calculateSpaceCompassKeyEvents(calendar, request.place, remote) { job.ensureActive() } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { SpaceCompassErrorLog.record(context, "observatory:key_events", error); null }
        }
        coroutineContext.ensureActive()
        val result = SpaceCompassKeyEventsResult(request, events)
        cache.put(result); value = result
    }
    val display = if (request == null) null else cached ?: computed?.takeIf { it.request.calendar == calendar }
    return SpaceCompassKeyEventsUiState(display?.events,
        request == null || cached != null || computed?.request == request, request != null, display?.request)
}
