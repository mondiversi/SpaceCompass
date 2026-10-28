package me.mondiversi.spacecompass

import android.content.SharedPreferences

internal data class SpaceCompassCatalogPreferences(
    val sort: SpaceCompassCatalogSort = SpaceCompassCatalogSort.CATALOG,
    val types: Set<SpaceCompassCatalogType> = emptySet(),
    val visibility: SpaceCompassCatalogVisibility = SpaceCompassCatalogVisibility.ALL,
    val origin: SpaceCompassCatalogOrigin = SpaceCompassCatalogOrigin.ALL,
    val pointing: SpaceCompassCatalogPointing = SpaceCompassCatalogPointing.ALL
) {
    val hasFilters: Boolean get() = types.isNotEmpty() || visibility != SpaceCompassCatalogVisibility.ALL ||
        origin != SpaceCompassCatalogOrigin.ALL || pointing != SpaceCompassCatalogPointing.ALL
}

/** Stable enum names survive app/process restarts; removed values fall back safely. */
internal fun restoreSpaceCompassCatalogPreferences(sort: String?, types: Set<String>?, visibility: String?,
    origin: String? = null, pointing: String? = null) =
    SpaceCompassCatalogPreferences(
        SpaceCompassCatalogSort.entries.firstOrNull { it.name == sort } ?: SpaceCompassCatalogSort.CATALOG,
        SpaceCompassCatalogType.entries.filter { it.name in types.orEmpty() }.toSet(),
        SpaceCompassCatalogVisibility.entries.firstOrNull { it.name == visibility } ?: SpaceCompassCatalogVisibility.ALL,
        SpaceCompassCatalogOrigin.entries.firstOrNull { it.name == origin } ?: SpaceCompassCatalogOrigin.ALL,
        SpaceCompassCatalogPointing.entries.firstOrNull { it.name == pointing } ?: SpaceCompassCatalogPointing.ALL
    )

internal fun readSpaceCompassCatalogPreferences(preferences: SharedPreferences?) = restoreSpaceCompassCatalogPreferences(
    preferences?.getString("catalog_sort", null),
    preferences?.getStringSet("catalog_types", null),
    preferences?.getString("catalog_visibility", null),
    preferences?.getString("catalog_origin", null),
    preferences?.getString("catalog_pointing", null)
)

/** Write from the selection event, rather than waiting for composition or page dismissal. */
internal fun saveSpaceCompassCatalogPreferences(preferences: SharedPreferences?, value: SpaceCompassCatalogPreferences) {
    preferences?.edit()
        ?.putString("catalog_sort", value.sort.name)
        ?.putStringSet("catalog_types", value.types.map { it.name }.toSet())
        ?.putString("catalog_visibility", value.visibility.name)
        ?.putString("catalog_origin", value.origin.name)
        ?.putString("catalog_pointing", value.pointing.name)
        ?.apply()
}
