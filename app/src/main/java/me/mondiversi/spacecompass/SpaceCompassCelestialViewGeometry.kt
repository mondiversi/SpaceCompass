package me.mondiversi.spacecompass

import io.github.cosinekitty.astronomy.*
import kotlin.math.*

internal data class SpaceCompassViewVector(val x: Double, val y: Double, val z: Double) {
    operator fun plus(b: SpaceCompassViewVector) = SpaceCompassViewVector(x + b.x, y + b.y, z + b.z)
    operator fun minus(b: SpaceCompassViewVector) = SpaceCompassViewVector(x - b.x, y - b.y, z - b.z)
    operator fun times(k: Double) = SpaceCompassViewVector(x * k, y * k, z * k)
    fun dot(b: SpaceCompassViewVector) = x * b.x + y * b.y + z * b.z
    fun cross(b: SpaceCompassViewVector) = SpaceCompassViewVector(y * b.z - z * b.y, z * b.x - x * b.z, x * b.y - y * b.x)
    fun unit(): SpaceCompassViewVector {
        val n = sqrt(dot(this)); require(n.isFinite() && n > 1e-12)
        return this * (1 / n)
    }
}
private fun Vector.viewer() = SpaceCompassViewVector(x, y, z)

/** Column-major, body-fixed x/y/equatorial and z/north to a camera looking down -Z. */
internal data class SpaceCompassCelestialViewGeometry(
    val bodyX: SpaceCompassViewVector, val bodyY: SpaceCompassViewVector, val bodyZ: SpaceCompassViewVector,
    val light: SpaceCompassViewVector, val distanceKm: Double? = null, val illuminatedFraction: Double? = null
) {
    fun toCamera(v: SpaceCompassViewVector) = bodyX * v.x + bodyY * v.y + bodyZ * v.z
    fun matrix() = floatArrayOf(bodyX.x.toFloat(), bodyX.y.toFloat(), bodyX.z.toFloat(),
        bodyY.x.toFloat(), bodyY.y.toFloat(), bodyY.z.toFloat(),
        bodyZ.x.toFloat(), bodyZ.y.toFloat(), bodyZ.z.toFloat())
}

/** Geometric, upright/local-zenith telescope view. UTC time; no phone compass dependence.
 * The IAU pole/prime-meridian model includes lunar libration. Maps are composites, not live images.
 */
internal fun calculateSpaceCompassCelestialViewGeometry(body: SpaceCompassCelestialBody, timeMs: Long,
    latitude: Double, longitude: Double, altitude: Double = 0.0): SpaceCompassCelestialViewGeometry? {
    if (!body.hasPhysicalFace) return null
    val engine = body.engine
    require(latitude.isFinite() && latitude in -90.0..90.0 && longitude.isFinite() && longitude in -180.0..180.0)
    val time = spaceCompassAstronomyTime(timeMs)
    val site = Observer(latitude, longitude, altitude).toVector(time, EquatorEpoch.J2000).viewer()
    val geo = (if (body.isJovianMoon) spaceCompassJovianMoonGeoVector(body, time, Aberration.None)
        else geoVector(requireNotNull(engine), time, Aberration.None)).viewer()
    val towardObserver = (site - geo).unit()
    val distanceKm = sqrt((site - geo).dot(site - geo)) * SPACE_COMPASS_AU_KM
    // Evaluate the visible face at the emission time, not at the arrival time on Earth.
    val emission = spaceCompassAstronomyTime(timeMs - (distanceKm / 299_792.458 * 1000).toLong())
    val axis = if (body.isJovianMoon) spaceCompassJovianMoonAxis(body, emission) else rotationAxis(requireNotNull(engine), emission)
    val ra = Math.toRadians(axis.ra * 15)
    val pole = axis.north.viewer().unit()
    val node = SpaceCompassViewVector(-sin(ra), cos(ra), 0.0)
    val equatorY = pole.cross(node).unit()
    val spin = Math.toRadians(axis.spin % 360)
    val prime = node * cos(spin) + equatorY * sin(spin)
    val east = pole.cross(prime).unit()
    val zenith = site.unit()
    var projectedUp = zenith - towardObserver * zenith.dot(towardObserver)
    if (projectedUp.dot(projectedUp) < 1e-8) {
        val reference = if (abs(towardObserver.z) < 0.9) SpaceCompassViewVector(0.0, 0.0, 1.0) else SpaceCompassViewVector(0.0, 1.0, 0.0)
        projectedUp = reference - towardObserver * reference.dot(towardObserver)
    }
    val up = projectedUp.unit()
    val right = up.cross(towardObserver).unit()
    fun camera(v: SpaceCompassViewVector) = SpaceCompassViewVector(v.dot(right), v.dot(up), v.dot(towardObserver))
    val heliocentric = if (body.isJovianMoon) spaceCompassJovianMoonHelioVector(body, emission).viewer()
        else if (engine == Body.Moon)
        helioVector(Body.Earth, emission).viewer() + geoVector(Body.Moon, emission, Aberration.None).viewer()
        else helioVector(requireNotNull(engine), emission).viewer()
    val light = if (engine == Body.Sun) towardObserver else (heliocentric * -1.0).unit()
    return SpaceCompassCelestialViewGeometry(camera(prime), camera(east), camera(pole), camera(light), distanceKm,
        if (engine == Body.Sun) null else ((1 + light.dot(towardObserver)) / 2).coerceIn(0.0, 1.0))
}

/** Camera-space orientation: no Euler pole/clamp, so a drag can cross either pole freely. */
internal data class SpaceCompassViewQuaternion(val w: Double = 1.0, val x: Double = 0.0,
    val y: Double = 0.0, val z: Double = 0.0) {
    operator fun times(b: SpaceCompassViewQuaternion) = SpaceCompassViewQuaternion(
        w*b.w-x*b.x-y*b.y-z*b.z, w*b.x+x*b.w+y*b.z-z*b.y,
        w*b.y-x*b.z+y*b.w+z*b.x, w*b.z+x*b.y-y*b.x+z*b.w)
    fun unit(): SpaceCompassViewQuaternion {
        val length = sqrt(w*w+x*x+y*y+z*z)
        return SpaceCompassViewQuaternion(w/length, x/length, y/length, z/length)
    }
    fun rotate(v: SpaceCompassViewVector): SpaceCompassViewVector {
        val axis = SpaceCompassViewVector(x, y, z)
        return v + axis.cross(v) * (2*w) + axis.cross(axis.cross(v)) * 2.0
    }
    /** Shortest-arc spherical interpolation to the canonical orientation. */
    fun returning(progress: Double): SpaceCompassViewQuaternion {
        val amount = progress.coerceIn(0.0, 1.0)
        if (amount <= 0) return this
        if (amount >= 1) return SpaceCompassViewQuaternion()
        val normalized = unit()
        val q = if (normalized.w < 0) SpaceCompassViewQuaternion(-normalized.w, -normalized.x, -normalized.y, -normalized.z) else normalized
        val angle = acos(q.w.coerceIn(-1.0, 1.0))
        if (angle < 1e-6) return SpaceCompassViewQuaternion()
        val a = sin((1-amount)*angle)/sin(angle)
        val b = sin(amount*angle)/sin(angle)
        return SpaceCompassViewQuaternion(q.w*a+b, q.x*a, q.y*a, q.z*a).unit()
    }
    companion object {
        fun axisAngle(axis: SpaceCompassViewVector, degrees: Double): SpaceCompassViewQuaternion {
            val half = Math.toRadians(degrees % 360) / 2
            val direction = axis.unit() * sin(half)
            return SpaceCompassViewQuaternion(cos(half), direction.x, direction.y, direction.z)
        }
    }
}

/** Canonical spin stays separate from manual orientation, independent of the live ephemeris. */
internal data class SpaceCompassCelestialRotation(val yaw: Double = 0.0, val pitch: Double = 15.0,
    val automatic: Boolean = true, val manualOrientation: SpaceCompassViewQuaternion = SpaceCompassViewQuaternion(),
    val returning: Boolean = false) {
    fun drag(dx: Double, dy: Double, extent: Double): SpaceCompassCelestialRotation {
        if (!dx.isFinite() || !dy.isFinite() || !extent.isFinite() || extent <= 0) return this
        val horizontal = dx / extent * 180
        val vertical = dy / extent * 180
        if (!horizontal.isFinite() || !vertical.isFinite()) return this
        // Positive screen X/Y move the visible surface right/down, respectively.
        val turn = SpaceCompassViewQuaternion.axisAngle(SpaceCompassViewVector(1.0, 0.0, 0.0), vertical) *
            SpaceCompassViewQuaternion.axisAngle(SpaceCompassViewVector(0.0, 1.0, 0.0), horizontal)
        return copy(manualOrientation = (turn * manualOrientation).unit(), automatic = false, returning = false)
    }
    fun advance(seconds: Double, retrograde: Boolean = false) = if (!automatic || !seconds.isFinite() || seconds < 0) this
        else copy(yaw = ((yaw + seconds.coerceAtMost(0.1) * (if (retrograde) -12 else 12)) % 360 + 360) % 360)
    fun resume() = copy(automatic = true, manualOrientation = SpaceCompassViewQuaternion(), returning = false)
    fun beginReturn() = if (automatic && !returning) this else copy(automatic = false, returning = true)
    fun geometry(): SpaceCompassCelestialViewGeometry {
        val a = Math.toRadians(yaw); val b = Math.toRadians(pitch)
        // Pole is upright in this illustrative view; turn about the body's own north axis.
        return SpaceCompassCelestialViewGeometry(manualOrientation.rotate(SpaceCompassViewVector(cos(a), -sin(a) * sin(b), sin(a) * cos(b))),
            manualOrientation.rotate(SpaceCompassViewVector(sin(a), cos(a) * sin(b), -cos(a) * cos(b))),
            manualOrientation.rotate(SpaceCompassViewVector(0.0, cos(b), sin(b))), SpaceCompassViewVector(-0.4, 0.3, 1.0).unit())
    }
}
