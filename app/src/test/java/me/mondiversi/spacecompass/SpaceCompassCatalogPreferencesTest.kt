package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCatalogPreferencesTest {
    @Test fun allSortAndVisibilityModesRestoreAlongsideMultipleTypesAndExplicitlyEmptyFilters() {
        val types = setOf(SpaceCompassCatalogType.STAR, SpaceCompassCatalogType.BLACK_HOLE)
        for (sort in SpaceCompassCatalogSort.entries) for (visibility in SpaceCompassCatalogVisibility.entries) {
            assertEquals(SpaceCompassCatalogPreferences(sort, types, visibility),
                restoreSpaceCompassCatalogPreferences(sort.name, types.map { it.name }.toSet(), visibility.name))
            assertEquals(SpaceCompassCatalogPreferences(sort, emptySet(), visibility),
                restoreSpaceCompassCatalogPreferences(sort.name, emptySet(), visibility.name))
        }
    }

    @Test fun existingInstallsKeepTheirSortWhileMissingOrRemovedFiltersUseSafeDefaults() {
        assertEquals(SpaceCompassCatalogPreferences(), restoreSpaceCompassCatalogPreferences(null, null, null))
        assertEquals(SpaceCompassCatalogPreferences(sort = SpaceCompassCatalogSort.DISTANCE_DESC),
            restoreSpaceCompassCatalogPreferences("DISTANCE_DESC", null, null))
        assertEquals(SpaceCompassCatalogPreferences(types = setOf(SpaceCompassCatalogType.STAR)),
            restoreSpaceCompassCatalogPreferences("REMOVED_SORT", setOf("STAR", "REMOVED_TYPE"), "REMOVED_VISIBILITY"))
    }
}
