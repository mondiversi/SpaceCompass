package me.mondiversi.spacecompass

import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import java.lang.reflect.Proxy
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCelestialFavoritesTest {
    private val sun = SpaceCompassCelestialBody.SUN
    private val moon = SpaceCompassCelestialBody.MOON

    @Test fun existingInstallsStartWithoutFavoritesAndUnknownIdsAreDiscarded() {
        assertTrue(readSpaceCompassCelestialFavorites(null).isEmpty())
        assertTrue(readSpaceCompassCelestialFavorites(Store().preferences).isEmpty())
        assertEquals(setOf(sun), restoreSpaceCompassCelestialFavorites(setOf("SUN", "REMOVED_OBJECT", "EARTH_CENTER")))
    }

    @Test fun favoritesRestoreFromStoredIdsAndToggleIndependentlyAcrossFreshReaders() {
        val store = Store()
        toggleSpaceCompassCelestialFavorite(store.preferences, sun)
        toggleSpaceCompassCelestialFavorite(store.preferences, moon)
        val restored = Store(store.values)
        assertEquals(setOf(sun, moon), readSpaceCompassCelestialFavorites(restored.preferences))
        assertEquals(setOf(moon), toggleSpaceCompassCelestialFavorite(restored.preferences, sun))
        assertEquals(setOf(moon), readSpaceCompassCelestialFavorites(restored.preferences))
        assertTrue(toggleSpaceCompassCelestialFavorite(restored.preferences, moon).isEmpty())
        assertTrue(readSpaceCompassCelestialFavorites(Store(restored.values).preferences).isEmpty())
    }

    @Test fun markingAnUncheckedObjectDoesNotChangeSceneSelectionActiveObjectOrCatalogPreferences() {
        val scene = mapOf("celestial_selected" to setOf("SUN", "MOON"), "celestial_active" to "MOON",
            "catalog_sort" to "NAME_ASC", "catalog_types" to setOf("PLANET"))
        val store = Store(scene)
        toggleSpaceCompassCelestialFavorite(store.preferences, SpaceCompassCelestialBody.MARS)
        assertEquals(setOf(SpaceCompassCelestialBody.MARS), readSpaceCompassCelestialFavorites(store.preferences))
        for ((key, value) in scene) assertEquals(value, store.values[key])
        assertEquals(scene.keys + SPACE_COMPASS_CELESTIAL_FAVORITES_KEY, store.values.keys)
    }

    @Test fun rapidActionsReadLatestPreferencesAndDoNotMutateTheStoredSetReference() {
        val oldNames = linkedSetOf("SUN", "REMOVED_OBJECT")
        val store = Store(mapOf(SPACE_COMPASS_CELESTIAL_FAVORITES_KEY to oldNames))
        toggleSpaceCompassCelestialFavorite(store.preferences, moon)
        assertEquals(setOf("SUN", "REMOVED_OBJECT"), oldNames)
        toggleSpaceCompassCelestialFavorite(store.preferences, SpaceCompassCelestialBody.MARS)
        assertEquals(setOf(sun, moon, SpaceCompassCelestialBody.MARS), readSpaceCompassCelestialFavorites(store.preferences))
        toggleSpaceCompassCelestialFavorite(store.preferences, moon)
        assertEquals(setOf(sun, SpaceCompassCelestialBody.MARS), readSpaceCompassCelestialFavorites(store.preferences))
    }

    @Test fun retiredObjectsCannotBecomeFavorites() {
        val store = Store(mapOf(SPACE_COMPASS_CELESTIAL_FAVORITES_KEY to setOf("SUN")))
        assertEquals(setOf(sun), toggleSpaceCompassCelestialFavorite(store.preferences, SpaceCompassCelestialBody.EARTH_CENTER))
        assertEquals(0, store.writes)
    }

    @Test fun favoritePaletteIsLegibleInBothThemesAndLighterAtNight() {
        fun contrast(a: Color, b: Color) = (maxOf(a.luminance(), b.luminance()) + .05f) /
            (minOf(a.luminance(), b.luminance()) + .05f)
        val day = spaceCompassCelestialFavoriteColor(false)
        val night = spaceCompassCelestialFavoriteColor(true)
        assertNotEquals(day, night)
        assertTrue(night.luminance() > day.luminance())
        assertTrue(contrast(day, Color.White) >= 4.5f)
        assertTrue(contrast(night, Color(0xFF202124)) >= 4.5f)
    }

    /** Interface-only store: no device storage, personal preferences or Android test runtime. */
    private class Store(initial: Map<String, Any?> = emptyMap()) {
        val values = initial.toMutableMap()
        var writes = 0
        private val pending = mutableMapOf<String, Any?>()
        private val editor = Proxy.newProxyInstance(SharedPreferences.Editor::class.java.classLoader,
            arrayOf(SharedPreferences.Editor::class.java)) { proxy, method, args ->
            when (method.name) {
                "putStringSet" -> { pending[args!![0] as String] = (args[1] as Set<*>).toSet(); proxy }
                "apply" -> { values.putAll(pending); pending.clear(); writes++; null }
                else -> throw AssertionError("Unexpected edit: ${method.name}")
            }
        } as SharedPreferences.Editor
        val preferences = Proxy.newProxyInstance(SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java)) { _, method, args ->
            when (method.name) {
                "getStringSet" -> values[args!![0] as String] ?: args[1]
                "edit" -> editor
                else -> throw AssertionError("Unexpected access: ${method.name}")
            }
        } as SharedPreferences
    }
}
