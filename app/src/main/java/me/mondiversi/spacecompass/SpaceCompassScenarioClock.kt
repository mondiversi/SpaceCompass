package me.mondiversi.spacecompass

/** Persistence policy is independent from acceleration and from the date/time switches. */
internal enum class SpaceCompassScenarioResume {
    RESTART, ACTIVE, ALWAYS;
    companion object { val DEFAULT = RESTART }
}
private val spaceCompassScenarioRateMagnitudes = listOf(1, 10, 30, 60, 300, 900, 3600, 21600, 43200, 86400, 1296000)
// Accept older saved settings and archives without silently increasing their speed.
private val spaceCompassLegacyScenarioRates = setOf(2, 3, 5, 6, 120, 600, 1800, 7200)
    .flatMap { listOf(it, -it) }.toSet()
internal fun spaceCompassSupportedScenarioRate(rate: Int): Boolean =
    rate in spaceCompassScenarioRates || rate in spaceCompassLegacyScenarioRates
internal val spaceCompassScenarioRates = spaceCompassScenarioRateMagnitudes.asReversed().map { -it } +
    listOf(0) + spaceCompassScenarioRateMagnitudes
internal data class SpaceCompassScenarioTimeSettings(val enabled: Boolean = false, val rate: Int = 1,
    val resume: SpaceCompassScenarioResume = SpaceCompassScenarioResume.DEFAULT) {
    val multiplier: Int get() = if (enabled) rate else 1
    val accelerated: Boolean get() = multiplier !in -1..1
    fun normalized(): SpaceCompassScenarioTimeSettings = when (rate) {
        in spaceCompassScenarioRates -> this
        in spaceCompassLegacyScenarioRates -> copy(rate = spaceCompassScenarioRateMagnitudes
            .last { it <= kotlin.math.abs(rate) } * if (rate < 0) -1 else 1)
        else -> copy(rate = 1)
    }
}

internal data class SpaceCompassScenarioCheckpoint(val signature: String, val instant: Long, val wall: Long) {
    fun encode() = "$signature\t$instant\t$wall"
    companion object {
        fun decode(value: String?): SpaceCompassScenarioCheckpoint? = runCatching {
            val fields = requireNotNull(value).split('\t'); require(fields.size == 3)
            SpaceCompassScenarioCheckpoint(fields[0], fields[1].toLong(), fields[2].toLong())
        }.getOrNull()
    }
}

private const val scenarioFirstInstant = -2_208_988_800_000L // 1900-01-01 UTC
private const val scenarioLastInstant = 4_133_980_799_999L // 2100-12-31 UTC

/** No worker or sensor is needed while closed. Active-only time uses the monotonic clock. */
internal class SpaceCompassScenarioClock(val settings: SpaceCompassScenarioTimeSettings, val signature: String,
    initialInstant: Long, wallNow: Long, elapsedNow: Long,
    checkpoint: SpaceCompassScenarioCheckpoint? = null,
    private val elapsed: () -> Long = { android.os.SystemClock.elapsedRealtime() }) {
    private var anchor = checkpoint?.takeIf { it.signature == signature && settings.resume != SpaceCompassScenarioResume.RESTART }
        ?.let { if (settings.resume == SpaceCompassScenarioResume.ALWAYS)
            advance(it.instant, wallNow - it.wall) else it.instant } ?: initialInstant
    private var anchorWall = wallNow
    private var anchorElapsed = elapsedNow
    private var active = false
    private var heldInstant: Long? = null

    private fun advance(base: Long, delta: Long): Long =
        (base.toDouble() + delta.coerceAtLeast(0).toDouble() * settings.multiplier)
            .coerceIn(scenarioFirstInstant.toDouble(), scenarioLastInstant.toDouble()).toLong()

    @Synchronized fun at(wallNow: Long, elapsedNow: Long = elapsed()): Long = when {
        heldInstant != null -> requireNotNull(heldInstant)
        settings.resume == SpaceCompassScenarioResume.ALWAYS -> advance(anchor, wallNow - anchorWall)
        active -> advance(anchor, elapsedNow - anchorElapsed)
        else -> anchor
    }

    /** A photograph's exposure time may precede its async completion, especially at high time speeds. */
    @Synchronized fun atWallTime(requestedWall: Long, currentWall: Long = System.currentTimeMillis(),
        elapsedNow: Long = elapsed()): Long = when {
        heldInstant != null -> requireNotNull(heldInstant)
        settings.resume == SpaceCompassScenarioResume.ALWAYS -> at(requestedWall,elapsedNow)
        active -> advance(anchor,(elapsedNow-anchorElapsed)+(requestedWall-currentWall))
        else -> anchor
    }

    @Synchronized fun setActive(value: Boolean, wallNow: Long, elapsedNow: Long = elapsed()) {
        if (value == active) return
        anchor = at(wallNow, elapsedNow); anchorWall = wallNow; anchorElapsed = elapsedNow; active = value
    }

    /** Editor previews never consume time, including the otherwise wall-clock-based Always mode. */
    @Synchronized fun hold(wallNow: Long, elapsedNow: Long = elapsed()) {
        if (heldInstant == null) heldInstant = at(wallNow, elapsedNow)
    }

    @Synchronized fun release(wallNow: Long, elapsedNow: Long = elapsed()) {
        val instant = heldInstant ?: return
        anchor = instant; anchorWall = wallNow; anchorElapsed = elapsedNow; heldInstant = null
    }

    @Synchronized fun checkpoint(wallNow: Long, elapsedNow: Long = elapsed()) =
        SpaceCompassScenarioCheckpoint(signature, at(wallNow, elapsedNow), wallNow)
}

/** Position-only edits must not restart the temporal scenario. Zone changes do reinterpret civil fields. */
internal fun spaceCompassScenarioClockSignature(plan: SpaceCompassObserverPlan, settings: SpaceCompassScenarioTimeSettings,
    deviceZone: java.time.ZoneId): String = listOf(plan.simulateDate, plan.simulateTime,
    plan.timeMs.takeIf { plan.hasCustomMoment }, plan.momentZoneId.takeIf { plan.hasCustomMoment },
    plan.observationZone(deviceZone).id.takeIf { plan.simulateZone || plan.simulatePosition },
    plan.advanceTime, plan.clockOffsetMs, settings.enabled, settings.rate, settings.resume).joinToString(";")
