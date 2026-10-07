package me.mondiversi.spacecompass

import java.time.Instant
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassHiddenObjectOrderTest {
    private val hidden = SpaceCompassCelestialBody.LV_426
    private val alpha = SpaceCompassCelestialBody.ALPHA_CENTAURI
    private val trappist = SpaceCompassCelestialBody.TRAPPIST_1_E

    @Test fun defaultCatalogPlacesTheScenicReferenceBetweenItsDistanceNeighbors() {
        val public = spaceCompassAvailableCelestialCatalog(emptySet())
        val revealed = spaceCompassAvailableCelestialCatalog(setOf(hidden))
        assertEquals(public, revealed.filterNot { it == hidden })
        val index = revealed.indexOf(hidden)
        assertEquals(alpha, revealed[index - 1])
        assertEquals(trappist, revealed[index + 1])
        assertEquals(1, revealed.count { it == hidden })
    }

    @Test fun savedNavigationUsesTheSameOrderWithoutChangingActiveOrCheckedObjects() {
        val selection = SpaceCompassCelestialSelection(setOf(alpha, trappist), trappist).revealHiddenObject()
        assertEquals(listOf(alpha, hidden, trappist), selection.ordered)
        val restored = restoreSpaceCompassCelestialSelection(selection.ordered.map { it.name }.toSet(), trappist.name)
        assertEquals(selection, restored)
        assertEquals(hidden, restored.step(-1).active)
    }

    @Test fun revealedDistanceExistsBeforeRefreshAndMatchesTheOfflineCatalogReference() {
        val now = Instant.parse("2026-10-06T12:00:00Z").toEpochMilli()
        val reference = spaceCompassCelestialCatalogReferenceDistanceAu(hidden)!!
        assertEquals(39.3, reference * SPACE_COMPASS_AU_KM / SPACE_COMPASS_LIGHT_YEAR_KM, 1e-9)
        assertEquals(reference, spaceCompassCelestialCatalogDistanceAu(hidden, now)!!, 0.0)
        assertEquals(reference, spaceCompassCatalogDistancesWithReferences(emptyMap(), listOf(hidden))[hidden]!!, 0.0)
        assertEquals(reference, spaceCompassCatalogDistancesWithReferences(mapOf(hidden to null), listOf(hidden))[hidden]!!, 0.0)
    }

    @Test fun explicitDistanceOrdersIntegrateTheRevealImmediatelyInBothDirections() {
        val bodies = listOf(trappist, hidden, alpha)
        val ready = spaceCompassCatalogDistancesWithReferences(emptyMap(), bodies)
        assertEquals(listOf(alpha, hidden, trappist),
            spaceCompassSortCatalog(bodies, SpaceCompassCatalogSort.DISTANCE_ASC, emptyMap(), ready, Locale.ROOT))
        assertEquals(listOf(trappist, hidden, alpha),
            spaceCompassSortCatalog(bodies, SpaceCompassCatalogSort.DISTANCE_DESC, emptyMap(), ready, Locale.ROOT))
    }

    @Test fun distanceFallbackDoesNotInventRemoteDataOrConvertNearbyKmSummariesToAu() {
        val moon = SpaceCompassCelestialBody.MOON
        val voyager = SpaceCompassCelestialBody.VOYAGER_1
        val values = spaceCompassCatalogDistancesWithReferences(mapOf(moon to 375000.0), listOf(moon, voyager, hidden))
        assertEquals(375000.0, values[moon]!!, 0.0)
        assertNull(values[voyager])
        assertNotNull(values[hidden])
        assertNull(spaceCompassCelestialCatalogReferenceDistanceAu(moon))
    }

    @Test fun completedValuesArePreservedAndInvalidDistantReferencesCanRecoverOffline() {
        val current = spaceCompassCelestialCatalogReferenceDistanceAu(alpha)!!
        val values = spaceCompassCatalogDistancesWithReferences(mapOf(alpha to current, hidden to Double.NaN), listOf(alpha, hidden))
        assertEquals(current, values[alpha]!!, 0.0)
        assertEquals(spaceCompassCelestialCatalogReferenceDistanceAu(hidden)!!, values[hidden]!!, 0.0)
        assertEquals(SpaceCompassCelestialSelection().revealHiddenObject(), SpaceCompassCelestialSelection().revealHiddenObject().revealHiddenObject())
    }
}
