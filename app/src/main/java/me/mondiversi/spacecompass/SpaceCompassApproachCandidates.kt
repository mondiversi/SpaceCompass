package me.mondiversi.spacecompass

import kotlin.math.floor
import kotlin.math.sin

/** A 3-D unit-vector grid avoids an all-pairs angular scan for large checked catalogues.
 * Every pair closer than the angular limit lies in the same or an adjacent grid cell,
 * including at the poles and across the azimuth seam. Exact separation is checked later. */
internal fun spaceCompassApproachCandidates(positions: Map<SpaceCompassCelestialBody, SpaceCompassSunPosition>):
    List<Pair<SpaceCompassCelestialBody, SpaceCompassCelestialBody>> {
    val bodies = positions.keys.filterNot { it.isFictional }.sortedBy { it.ordinal }
    if (bodies.size < 16) return buildList {
        for (i in bodies.indices) for (j in i + 1 until bodies.size) add(bodies[i] to bodies[j])
    }
    val chord = 2 * sin(Math.toRadians(SPACE_COMPASS_APPROACH_LIMIT_DEGREES) / 2)
    data class Cell(val x: Int, val y: Int, val z: Int)
    val cells = mutableMapOf<Cell, MutableList<SpaceCompassCelestialBody>>()
    return buildList {
        for (body in bodies) {
            val position = positions.getValue(body)
            if (!position.azimuthDegrees.isFinite() || !position.elevationDegrees.isFinite()) continue
            val vector = spaceCompassSunCompassDirection(position.azimuthDegrees, position.elevationDegrees)
            val cell = Cell(floor(vector.east / chord).toInt(), floor(vector.north / chord).toInt(), floor(vector.up / chord).toInt())
            for (x in -1..1) for (y in -1..1) for (z in -1..1)
                cells[Cell(cell.x + x, cell.y + y, cell.z + z)]?.forEach { add(it to body) }
            cells.getOrPut(cell) { mutableListOf() }.add(body)
        }
    }
}
