package me.mondiversi.spacecompass

/** Alphabetical/default order retain the usual distance summary; numeric orders expose their key. */
internal val SpaceCompassCatalogSort.valueField: SpaceCompassCatalogSortField
    get() = field?.takeUnless { it == SpaceCompassCatalogSortField.NAME } ?: SpaceCompassCatalogSortField.DISTANCE

/** Same reference and uncertainty notation in object details and the catalog value column. */
internal fun formatSpaceCompassCelestialDiameter(facts: SpaceCompassCelestialFacts,
    numeric: SpaceCompassNumericFormat, feet: Boolean): String {
    val diameter = facts.diameterKm?.takeIf { it.isFinite() && it >= 0 } ?: return "—"
    fun length(km: Double?, digits: Int) = formatSpaceCompassPhysicalLength(km?.times(1000), digits, numeric, feet, large = true)
    return (if (facts.diameterEstimated) "≈ " else "") + length(diameter, if (facts.diameterEstimated) 2 else 0) +
        (facts.diameterErrorPlusKm?.let { " (+${length(it, 2)} / −${length(facts.diameterErrorMinusKm, 2)})" }
            ?: facts.diameterErrorKm?.let { " (±${length(it, 0)})" } ?: "")
}

/** The displayed quantity and numeric sort key share one reference; fallback labels are mandatory. */
internal fun formatSpaceCompassCatalogPhysicalValue(body: SpaceCompassCelestialBody,
    field: SpaceCompassCatalogSortField, numeric: SpaceCompassNumericFormat, units: SpaceCompassUnits,
    referenceLabel: (Int) -> String): String {
    if (spaceCompassCatalogPhysicalSortValue(body, field) == null) return "—"
    val facts = spaceCompassCelestialFacts(body)
    return when (field) {
        SpaceCompassCatalogSortField.MASS -> formatSpaceCompassCelestialMass(body, facts, numeric, units.pounds)
        SpaceCompassCatalogSortField.DIAMETER -> formatSpaceCompassCelestialDiameter(facts, numeric, units.feet)
        SpaceCompassCatalogSortField.PRESSURE -> spaceCompassAtmosphericPressure(body)?.let {
            formatSpaceCompassAtmosphericPressure(it, numeric, units.pressure)
        } ?: "—"
        SpaceCompassCatalogSortField.GRAVITY -> formatSpaceCompassCelestialGravity(facts.gravity, numeric,
            fractionDigits = if (body == SpaceCompassCelestialBody.POLARIS) 2 else 1, feet = units.feet)
        SpaceCompassCatalogSortField.DAY_TEMPERATURE, SpaceCompassCatalogSortField.NIGHT_TEMPERATURE -> {
            spaceCompassCatalogTemperatureReference(body, field)?.let { reference ->
                val temperature = reference.temperature
                val value = formatSpaceCompassCelestialTemperature(temperature, numeric, units.fahrenheit)
                if (reference.exactDayNight) value else value + "\n" +
                    referenceLabel(temperature.kind.catalogLabelResource) +
                    (temperature.component?.let { " ($it)" } ?: "")
            } ?: "—"
        }
        SpaceCompassCatalogSortField.NAME, SpaceCompassCatalogSortField.DISTANCE -> "—"
    }
}
