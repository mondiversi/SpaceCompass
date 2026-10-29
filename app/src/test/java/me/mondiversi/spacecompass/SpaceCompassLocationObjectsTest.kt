package me.mondiversi.spacecompass

import android.content.SharedPreferences
import java.lang.reflect.Proxy
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassLocationObjectsTest {
    private val moment = Instant.parse("2027-04-06T12:00:00Z").toEpochMilli()
    private val plan = spaceCompassObserverPlan(28.35, -14.1, 102.5, moment, "Atlantic/Canary",
        simulateTime = false, simulateDate = false)!!
    private val base = SpaceCompassScenarioArchive("Location", plan, SpaceCompassScenarioTimeSettings())
    private val sun = SpaceCompassCelestialBody.SUN
    private val moon = SpaceCompassCelestialBody.MOON

    @Test fun olderArchivesHaveNoSceneSelectionAndKeepTheRecipientsChecks() {
        val text = base.encode()
        assertFalse(text.contains("objectsIncluded="))
        assertNull(SpaceCompassScenarioArchive.decode(text).celestialSelection)
        val store = Store(mapOf("celestial_selected" to setOf("MOON"), "celestial_active" to "MOON"))
        SpaceCompassObserverState(store.preferences, elapsedClock = { 0L }).importArchive(SpaceCompassScenarioArchive.decode(text))
        assertEquals(SpaceCompassCelestialSelection(setOf(moon), moon), readSpaceCompassCelestialSelection(store.preferences))
    }

    @Test fun includedObjectsAndActiveObjectRoundTripWithoutChangingTheirOrderOrFlags() {
        val selection = SpaceCompassCelestialSelection(setOf(sun, moon, SpaceCompassCelestialBody.MARS), moon)
        val archive = base.copy(celestialSelection = selection)
        assertEquals(archive, SpaceCompassScenarioArchive.decode(archive.encode()))
        assertEquals(base.configuration, archive.configuration)
    }

    @Test fun includingAnExplicitlyEmptySelectionClearsTheRecipientScene() {
        val empty = SpaceCompassCelestialSelection(emptySet(), null)
        val archive = SpaceCompassScenarioArchive.decode(base.copy(celestialSelection = empty).encode())
        val store = Store(mapOf("celestial_selected" to setOf("SUN"), "celestial_active" to "SUN"))
        SpaceCompassObserverState(store.preferences, elapsedClock = { 0L }).importArchive(archive)
        assertEquals(empty, readSpaceCompassCelestialSelection(store.preferences))
        assertEquals(emptySet<String>(), store.values["celestial_selected"])
    }

    @Test fun theEntireCatalogCanBeIncludedWithinTheBoundedArchiveLimit() {
        val selection = SpaceCompassCelestialSelection(spaceCompassAllCelestialOrder.toSet(), moon)
        val archive = base.copy(celestialSelection = selection)
        val text = archive.encode()
        assertTrue(text.toByteArray(Charsets.UTF_8).size <= SPACE_COMPASS_SCENARIO_FILE_LIMIT)
        assertEquals(selection, SpaceCompassScenarioArchive.decode(text).celestialSelection)
    }

    @Test fun unknownOrRetiredIdsAreDroppedWithoutInventingACheckedObject() {
        val text = base.encode() + "objectsIncluded=true\nobjects=SUN,FUTURE_OBJECT,EARTH_CENTER\nactiveObject=FUTURE_OBJECT\n"
        assertEquals(SpaceCompassCelestialSelection(setOf(sun), sun), SpaceCompassScenarioArchive.decode(text).celestialSelection)
        val unknown = base.encode() + "objectsIncluded=true\nobjects=FUTURE_OBJECT\nactiveObject=FUTURE_OBJECT\n"
        assertEquals(SpaceCompassCelestialSelection(emptySet(), null), SpaceCompassScenarioArchive.decode(unknown).celestialSelection)
    }

    @Test fun invalidInclusionFlagsAndMissingObjectFieldsAreRejectedBeforeImport() {
        for (suffix in listOf("objectsIncluded=1\n", "objectsIncluded=true\n", "objectsIncluded=true\nobjects=SUN\n"))
            assertThrows(IllegalArgumentException::class.java) { SpaceCompassScenarioArchive.decode(base.encode() + suffix) }
    }

    @Test fun locationAndObjectsAreWrittenTogetherWhileFavoritesAndCatalogFiltersStayIntact() {
        val selected = SpaceCompassCelestialSelection(setOf(sun, moon), moon)
        val other = mapOf<String, Any?>(SPACE_COMPASS_CELESTIAL_FAVORITES_KEY to setOf("MARS"),
            "catalog_favorites_only" to true, "catalog_sort" to "NAME_DESC")
        val store = Store(other)
        SpaceCompassObserverState(store.preferences, elapsedClock = { 0L }).importArchive(base.copy(celestialSelection = selected))
        assertEquals(selected, readSpaceCompassCelestialSelection(Store(store.values).preferences))
        for ((key, value) in other) assertEquals(value, store.values[key])
        assertEquals(1, store.writes)
    }

    /** No Android disk, current device location, network or personal preferences. */
    private class Store(initial: Map<String, Any?> = emptyMap()) {
        val values = initial.toMutableMap()
        var writes = 0
        private val pending = mutableMapOf<String, Any?>()
        private val editor = Proxy.newProxyInstance(SharedPreferences.Editor::class.java.classLoader,
            arrayOf(SharedPreferences.Editor::class.java)) { proxy, method, args -> when (method.name) {
            "putString", "putStringSet", "putBoolean", "putInt" -> { pending[args!![0] as String] = args[1]; proxy }
            "remove" -> { pending[args!![0] as String] = null; proxy }
            "apply", "commit" -> {
                pending.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
                pending.clear(); writes++; if (method.name == "commit") true else null
            }
            else -> throw AssertionError("Unexpected edit: ${method.name}")
        } } as SharedPreferences.Editor
        val preferences = Proxy.newProxyInstance(SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java)) { _, method, args -> when (method.name) {
            "getString", "getStringSet", "getBoolean", "getInt" -> values[args!![0] as String] ?: args[1]
            "edit" -> editor
            else -> throw AssertionError("Unexpected access: ${method.name}")
        } } as SharedPreferences
    }
}
