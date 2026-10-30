package me.mondiversi.spacecompass

/** A background refresh does not invalidate the user's currently displayed controls. */
internal fun SpaceCompassCatalogQuery.hasSameControlsAs(other: SpaceCompassCatalogQuery): Boolean =
    preferences == other.preferences && search == other.search && selected == other.selected &&
        prioritySelected == other.prioritySelected && favorites == other.favorites

/** Publish complete row snapshots atomically. Keep the last computed facets for panel transitions;
 * closed-panel updates still skip facet calculations altogether. */
internal data class SpaceCompassCatalogViewState(
    val presentation: SpaceCompassCatalogPresentation? = null,
    val filterPresentation: SpaceCompassCatalogPresentation? = null
) {
    fun publish(result: SpaceCompassCatalogPresentation) = SpaceCompassCatalogViewState(result,
        if (result.counts != null) result else filterPresentation)
}
