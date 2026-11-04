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

internal class SpaceCompassObservationMomentsViewModel : ViewModel() {
    val cache = SpaceCompassObservationMomentsCache()
}

internal data class SpaceCompassObservationMomentsUiState(val moments: List<SpaceCompassObservationMoment>?,
    val complete: Boolean, val hasLocation: Boolean,
    val calculationRequest: SpaceCompassObservationRequest? = null) {
    val totalRows: Int? get() = moments?.let { spaceCompassObservationMomentRows(it).size } ?: 0.takeIf { complete }
}

/** Keep the last same-object/year/zone result visible while changed inputs are evaluated.
 * A stale date must not remain clickable, and a completed unavailable result replaces old data.
 */
@Composable
internal fun rememberSpaceCompassObservationMoments(calendar: SpaceCompassObservationCalendar,
    timeMs: Long, latitude: Double?, longitude: Double?, altitude: Double,
    remote: SpaceCompassCelestialRemoteData): SpaceCompassObservationMomentsUiState {
    val context = LocalContext.current
    val observer = LocalSpaceCompassObserver.current
    val owner = LocalActivity.current as? ViewModelStoreOwner
    val cache = remember(owner) {
        if (owner == null) SpaceCompassObservationMomentsCache()
        else ViewModelProvider(owner)[SpaceCompassObservationMomentsViewModel::class.java].cache
    }
    val plan = observer?.plan
    val fixedPosition = plan?.simulatePosition == true
    val fixedAltitude = plan?.simulateAltitude == true || fixedPosition
    val request = if (latitude == null || longitude == null) null else cache.request(calendar,
        SpaceCompassObservationPlace(latitude, longitude, altitude, fixedPosition, fixedAltitude),
        remote.satelliteOrbit(calendar.body), remote.ephemerides[calendar.body])
    val cached = cache.get(request)
    val computed by produceState<SpaceCompassObservationResult?>(cached, request) {
        if (request == null) return@produceState
        cache.get(request)?.let { value = it; return@produceState }
        val place = request.place
        val moments = withContext(Dispatchers.Default) {
            val job = coroutineContext
            try { calculateSpaceCompassObservationMoments(calendar.body,
                spaceCompassObservationYearReference(timeMs, calendar.year, calendar.zone), calendar.zone,
                place.latitude, place.longitude, place.altitude, remote, includeMonthlySolar = true) { job.ensureActive() } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                SpaceCompassErrorLog.record(context, "observatory:annual_moments", error); null
            }
        }
        coroutineContext.ensureActive()
        val result = SpaceCompassObservationResult(request, moments)
        cache.put(result)
        value = result
    }
    val display = if (request == null) null else cached ?: computed?.takeIf { it.request.calendar == calendar }
    return SpaceCompassObservationMomentsUiState(display?.moments,
        request == null || cached != null || computed?.request == request, request != null, display?.request)
}
