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
        assertEquals(public, revealed)
        val index = revealed.indexOf(hidden)
        assertEquals(SpaceCompassCelestialBody.VEGA, revealed.take(index).last { it.additionalCatalogEntry == null })
        assertEquals(trappist, revealed.drop(index + 1).first { it.largeCatalogEntry == null })
        assertEquals(1, revealed.count { it == hidden })
    }

    @Test fun savedNavigationUsesTheSameOrderWithoutChangingActiveOrCheckedObjects() {
        val selection = SpaceCompassCelestialSelection(setOf(alpha, hidden, trappist), trappist)
        assertEquals(listOf(alpha, hidden, trappist), selection.ordered)
        val restored = restoreSpaceCompassCelestialSelection(selection.ordered.map { it.name }.toSet(), trappist.name)
        assertEquals(selection, restored)
        assertEquals(hidden, restored.step(-1).active)
    }

    @Test fun fictionalDirectionDoesNotBecomeAnUnqualifiedMeasuredDistance() {
        val now = Instant.parse("2026-10-06T12:00:00Z").toEpochMilli()
        assertFalse(hidden.deepSkyReference!!.hasMeasuredDistance)
        assertNull(spaceCompassCelestialCatalogReferenceDistanceAu(hidden))
        assertNull(spaceCompassCelestialCatalogDistanceAu(hidden, now))
        assertNull(spaceCompassCatalogDistancesWithReferences(emptyMap(), listOf(hidden))[hidden])
        assertNull(spaceCompassCatalogDistancesWithReferences(mapOf(hidden to null), listOf(hidden))[hidden])
    }

    @Test fun explicitDistanceOrdersIntegrateTheRevealImmediatelyInBothDirections() {
        val bodies = listOf(trappist, hidden, alpha)
        val ready = spaceCompassCatalogDistancesWithReferences(emptyMap(), bodies)
        assertEquals(listOf(alpha, trappist, hidden),
            spaceCompassSortCatalog(bodies, SpaceCompassCatalogSort.DISTANCE_ASC, emptyMap(), ready, Locale.ROOT))
        assertEquals(listOf(trappist, alpha, hidden),
            spaceCompassSortCatalog(bodies, SpaceCompassCatalogSort.DISTANCE_DESC, emptyMap(), ready, Locale.ROOT))
    }

    @Test fun distanceFallbackDoesNotInventRemoteDataOrConvertNearbyKmSummariesToAu() {
        val moon = SpaceCompassCelestialBody.MOON
        val voyager = SpaceCompassCelestialBody.VOYAGER_1
        val values = spaceCompassCatalogDistancesWithReferences(mapOf(moon to 375000.0), listOf(moon, voyager, hidden))
        assertEquals(375000.0, values[moon]!!, 0.0)
        assertNull(values[voyager])
        assertNull(values[hidden])
        assertNull(spaceCompassCelestialCatalogReferenceDistanceAu(moon))
    }

    @Test fun completedValuesArePreservedAndInvalidDistantReferencesCanRecoverOffline() {
        val current = spaceCompassCelestialCatalogReferenceDistanceAu(alpha)!!
        val sirius = SpaceCompassCelestialBody.SIRIUS
        val values = spaceCompassCatalogDistancesWithReferences(mapOf(alpha to current, hidden to Double.NaN, sirius to Double.NaN), listOf(alpha, hidden, sirius))
        assertEquals(current, values[alpha]!!, 0.0)
        assertNull(values[hidden])
        assertEquals(spaceCompassCelestialCatalogReferenceDistanceAu(sirius)!!, values[sirius]!!, 0.0)
        assertEquals(1009, spaceCompassAvailableCelestialCatalog(emptySet()).size)
    }
}
