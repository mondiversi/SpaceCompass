package me.mondiversi.spacecompass

import kotlinx.coroutines.CancellationException

internal const val SPACE_COMPASS_ISS_TLE_URL = "https://celestrak.org/NORAD/elements/gp.php?CATNR=25544&FORMAT=TLE"
internal const val SPACE_COMPASS_ISS_SATCAT_URL = "https://www.satcat.com/sats/25544"

/** Extract only the unambiguous public copyable record; never execute page content. */
internal fun spaceCompassSatcatTle(page: String, objectName: String): String {
    require(page.length <= SPACE_COMPASS_SATCAT_MAX_BYTES)
    val blocks = Regex("<pre\\b[^>]*>\\s*<code\\b[^>]*>([\\s\\S]*?)</code>\\s*</pre>", RegexOption.IGNORE_CASE)
        .findAll(page).map { it.groupValues[1].trim().replace("&#10;", "\n").replace("&#13;", "\r") }
        .filter { it.lineSequence().firstOrNull()?.trim() == "0 $objectName" }.toList()
    require(blocks.size == 1) { "Missing or ambiguous public satellite TLE" }
    return blocks.single()
}

internal fun spaceCompassSatcatIssTle(page: String): String = spaceCompassSatcatTle(page, "ISS (ZARYA)")
    .also { SpaceCompassIssOrbit.parse(it) }

/** Provider-specific HTTP stops survive retries; an independent public source can still recover. */
internal class SpaceCompassSatelliteSources(
    private val primary: String, private val alternative: String,
    private val convertAlternative: (String) -> String,
    private val validate: (String, Long) -> Unit
) {
    private var preferAlternative = false
    private val stopped = mutableSetOf<String>()

    suspend fun load(now: Long, fetch: suspend (String, Int) -> String): String {
        val providers = listOf(primary, alternative).let { if (preferAlternative) it.reversed() else it }
        var lastFailure: Exception? = null
        for (url in providers.filterNot { it in stopped }) {
            try {
                val fallback = url == alternative
                val raw = fetch(url, if (fallback) SPACE_COMPASS_SATCAT_MAX_BYTES else 65_536)
                val text = if (fallback) convertAlternative(raw) else raw
                validate(text, now)
                preferAlternative = fallback
                return text
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (error is SpaceCompassCelestialHttpException) stopped += url
                lastFailure = if (error is SpaceCompassCelestialHttpException)
                    IllegalStateException("Satellite provider rejected the request", error) else error
            }
        }
        throw lastFailure ?: IllegalStateException("Satellite providers are stopped for this session")
    }
}

internal class SpaceCompassIssSources {
    private val sources = SpaceCompassSatelliteSources(SPACE_COMPASS_ISS_TLE_URL,
        SPACE_COMPASS_ISS_SATCAT_URL, ::spaceCompassSatcatIssTle) { text, now ->
        require(SpaceCompassIssOrbit.parse(text).usable(now)) { "ISS elements are too old" }
    }
    suspend fun load(now: Long, fetch: suspend (String, Int) -> String): String = sources.load(now, fetch)
}
