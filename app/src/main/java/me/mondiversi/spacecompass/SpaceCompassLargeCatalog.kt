package me.mondiversi.spacecompass

/** Bundled, typed science registry. Graphics are deliberately not bundled here.
 * Stable enum identities are append-only; all lookups are O(1) after one lazy initialization. */
internal data class SpaceCompassLargeCatalogEntry(
    val body: SpaceCompassCelestialBody,
    val entry: SpaceCompassAdditionalCatalogEntry,
    val facts: SpaceCompassCelestialFacts,
    val aliases: List<String>,
    val source: String,
    val sourceUrl: String,
    val galaxy: String,
    val note: Int,
    val temperatures: List<SpaceCompassCelestialTemperature> = emptyList(),
    val satellite: SpaceCompassCatalogSatellite? = null,
    val pattern: SpaceCompassConstellationPattern? = null,
    val classification: Int? = null
)
internal data class SpaceCompassConstellationPattern(val stars: List<SpaceCompassConstellationStar>,
    val edges: List<Pair<Int, Int>>)

internal val spaceCompassLargeCatalogEntries: List<SpaceCompassLargeCatalogEntry> by lazy {
    buildList {
        addAll(spaceCompassLargeCatalogData01())
        addAll(spaceCompassLargeCatalogData02())
        addAll(spaceCompassLargeCatalogData03())
        addAll(spaceCompassLargeCatalogData04())
        addAll(spaceCompassLargeCatalogData05())
        addAll(spaceCompassLargeCatalogData06())
        addAll(spaceCompassLargeCatalogData07())
        addAll(spaceCompassLargeCatalogData08())
        addAll(spaceCompassLargeCatalogData09())
        addAll(spaceCompassLargeCatalogData10())
        addAll(spaceCompassLargeCatalogData11())
        addAll(spaceCompassLargeCatalogData12())
        addAll(spaceCompassLargeCatalogData13())
        addAll(spaceCompassLargeCatalogData14())
        addAll(spaceCompassLargeCatalogData15())
        addAll(spaceCompassLargeCatalogData16())
        addAll(spaceCompassLargeCatalogData17())
        addAll(spaceCompassLargeCatalogData18())
        addAll(spaceCompassLargeCatalogData19())
        addAll(spaceCompassLargeCatalogData20())
        addAll(spaceCompassLargeCatalogData21())
        addAll(spaceCompassLargeCatalogData22())
        addAll(spaceCompassLargeCatalogData23())
        addAll(spaceCompassLargeCatalogData24())
        addAll(spaceCompassLargeCatalogData25())
        addAll(spaceCompassLargeCatalogData26())
    }.also { entries ->
        check(entries.size == 894 && entries.map { it.body }.toSet().size == entries.size)
    }
}
private val largeCatalogByOrdinal: Array<SpaceCompassLargeCatalogEntry?> by lazy {
    arrayOfNulls<SpaceCompassLargeCatalogEntry>(SpaceCompassCelestialBody.entries.size).also { index ->
        spaceCompassLargeCatalogEntries.forEach { index[it.body.ordinal] = it }
    }
}
internal val SpaceCompassCelestialBody.largeCatalogEntry: SpaceCompassLargeCatalogEntry?
    get() = largeCatalogByOrdinal[ordinal]

/** Retain all previous relative ordering, placing additions after their existing scientific family. */
internal fun spaceCompassIncludeLargeCatalog(previous: List<SpaceCompassCelestialBody>): List<SpaceCompassCelestialBody> {
    val additions = spaceCompassLargeCatalogEntries.groupBy { it.entry.type }
    val last = previous.withIndex().associate { it.value.catalogType to it.index }
    return buildList {
        previous.forEachIndexed { index, body ->
            add(body)
            if (last[body.catalogType] == index) addAll(additions[body.catalogType].orEmpty().map { it.body })
        }
    }.also { check(it.size == 999 && it.toSet().size == it.size) }
}
