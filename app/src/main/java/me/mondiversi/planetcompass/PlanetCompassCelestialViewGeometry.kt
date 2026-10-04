package me.mondiversi.planetcompass

import io.github.cosinekitty.astronomy.*
import kotlin.math.*

internal data class PlanetCompassViewVector(val x: Double, val y: Double, val z: Double) {
    operator fun plus(b: PlanetCompassViewVector) = PlanetCompassViewVector(x + b.x, y + b.y, z + b.z)
    operator fun minus(b: PlanetCompassViewVector) = PlanetCompassViewVector(x - b.x, y - b.y, z - b.z)
    operator fun times(k: Double) = PlanetCompassViewVector(x * k, y * k, z * k)
    fun dot(b: PlanetCompassViewVector) = x * b.x + y * b.y + z * b.z
    fun cross(b: PlanetCompassViewVector) = PlanetCompassViewVector(y * b.z - z * b.y, z * b.x - x * b.z, x * b.y - y * b.x)
    fun unit(): PlanetCompassViewVector {
        val n = sqrt(dot(this)); require(n.isFinite() && n > 1e-12)
        return this * (1 / n)
    }
}
private fun Vector.viewer() = PlanetCompassViewVector(x, y, z)

/** Column-major, body-fixed x/y/equatorial and z/north to a camera looking down -Z. */
internal data class PlanetCompassCelestialViewGeometry(
    val bodyX: PlanetCompassViewVector, val bodyY: PlanetCompassViewVector, val bodyZ: PlanetCompassViewVector,
    val light: PlanetCompassViewVector, val distanceKm: Double? = null, val illuminatedFraction: Double? = null
) {
    fun toCamera(v: PlanetCompassViewVector) = bodyX * v.x + bodyY * v.y + bodyZ * v.z
    fun matrix() = floatArrayOf(bodyX.x.toFloat(), bodyX.y.toFloat(), bodyX.z.toFloat(),
        bodyY.x.toFloat(), bodyY.y.toFloat(), bodyY.z.toFloat(),
        bodyZ.x.toFloat(), bodyZ.y.toFloat(), bodyZ.z.toFloat())
}

/** Geometric, upright/local-zenith telescope view. UTC time; no phone compass dependence.
 * The IAU pole/prime-meridian model includes lunar libration. Maps are composites, not live images.
 */
internal fun calculatePlanetCompassCelestialViewGeometry(body: PlanetCompassCelestialBody, timeMs: Long,
    latitude: Double, longitude: Double, altitude: Double = 0.0): PlanetCompassCelestialViewGeometry? {
    if (!body.hasPhysicalFace) return null
    val engine = body.engine
    require(latitude.isFinite() && latitude in -90.0..90.0 && longitude.isFinite() && longitude in -180.0..180.0)
    val time = planetCompassAstronomyTime(timeMs)
    val site = Observer(latitude, longitude, altitude).toVector(time, EquatorEpoch.J2000).viewer()
    val geo = (if (body.isJovianMoon) planetCompassJovianMoonGeoVector(body, time, Aberration.None)
        else geoVector(requireNotNull(engine), time, Aberration.None)).viewer()
    val towardObserver = (site - geo).unit()
    val distanceKm = sqrt((site - geo).dot(site - geo)) * PLANET_COMPASS_AU_KM
    // Evaluate the visible face at the emission time, not at the arrival time on Earth.
    val emission = planetCompassAstronomyTime(timeMs - (distanceKm / 299_792.458 * 1000).toLong())
    val axis = if (body.isJovianMoon) planetCompassJovianMoonAxis(body, emission) else rotationAxis(requireNotNull(engine), emission)
    val ra = Math.toRadians(axis.ra * 15)
    val pole = axis.north.viewer().unit()
    val node = PlanetCompassViewVector(-sin(ra), cos(ra), 0.0)
    val equatorY = pole.cross(node).unit()
    val spin = Math.toRadians(axis.spin % 360)
    val prime = node * cos(spin) + equatorY * sin(spin)
    val east = pole.cross(prime).unit()
    val zenith = site.unit()
    var projectedUp = zenith - towardObserver * zenith.dot(towardObserver)
    if (projectedUp.dot(projectedUp) < 1e-8) {
        val reference = if (abs(towardObserver.z) < 0.9) PlanetCompassViewVector(0.0, 0.0, 1.0) else PlanetCompassViewVector(0.0, 1.0, 0.0)
        projectedUp = reference - towardObserver * reference.dot(towardObserver)
    }
    val up = projectedUp.unit()
    val right = up.cross(towardObserver).unit()
    fun camera(v: PlanetCompassViewVector) = PlanetCompassViewVector(v.dot(right), v.dot(up), v.dot(towardObserver))
    val heliocentric = if (body.isJovianMoon) planetCompassJovianMoonHelioVector(body, emission).viewer()
        else if (engine == Body.Moon)
        helioVector(Body.Earth, emission).viewer() + geoVector(Body.Moon, emission, Aberration.None).viewer()
        else helioVector(requireNotNull(engine), emission).viewer()
    val light = if (engine == Body.Sun) towardObserver else (heliocentric * -1.0).unit()
    return PlanetCompassCelestialViewGeometry(camera(prime), camera(east), camera(pole), camera(light), distanceKm,
        if (engine == Body.Sun) null else ((1 + light.dot(towardObserver)) / 2).coerceIn(0.0, 1.0))
}

/** Camera-space orientation: no Euler pole/clamp, so a drag can cross either pole freely. */
internal data class PlanetCompassViewQuaternion(val w: Double = 1.0, val x: Double = 0.0,
    val y: Double = 0.0, val z: Double = 0.0) {
    operator fun times(b: PlanetCompassViewQuaternion) = PlanetCompassViewQuaternion(
        w*b.w-x*b.x-y*b.y-z*b.z, w*b.x+x*b.w+y*b.z-z*b.y,
        w*b.y-x*b.z+y*b.w+z*b.x, w*b.z+x*b.y-y*b.x+z*b.w)
    fun unit(): PlanetCompassViewQuaternion {
        val length = sqrt(w*w+x*x+y*y+z*z)
        return PlanetCompassViewQuaternion(w/length, x/length, y/length, z/length)
    }
    fun rotate(v: PlanetCompassViewVector): PlanetCompassViewVector {
        val axis = PlanetCompassViewVector(x, y, z)
        return v + axis.cross(v) * (2*w) + axis.cross(axis.cross(v)) * 2.0
    }
    /** Shortest-arc spherical interpolation to the canonical orientation. */
    fun returning(progress: Double): PlanetCompassViewQuaternion {
        val amount = progress.coerceIn(0.0, 1.0)
        if (amount <= 0) return this
        if (amount >= 1) return PlanetCompassViewQuaternion()
        val normalized = unit()
        val q = if (normalized.w < 0) PlanetCompassViewQuaternion(-normalized.w, -normalized.x, -normalized.y, -normalized.z) else normalized
        val angle = acos(q.w.coerceIn(-1.0, 1.0))
        if (angle < 1e-6) return PlanetCompassViewQuaternion()
        val a = sin((1-amount)*angle)/sin(angle)
        val b = sin(amount*angle)/sin(angle)
        return PlanetCompassViewQuaternion(q.w*a+b, q.x*a, q.y*a, q.z*a).unit()
    }
    companion object {
        fun axisAngle(axis: PlanetCompassViewVector, degrees: Double): PlanetCompassViewQuaternion {
            val half = Math.toRadians(degrees % 360) / 2
            val direction = axis.unit() * sin(half)
            return PlanetCompassViewQuaternion(cos(half), direction.x, direction.y, direction.z)
        }
    }
}

/** Canonical spin stays separate from manual orientation, independent of the live ephemeris. */
internal data class PlanetCompassCelestialRotation(val yaw: Double = 0.0, val pitch: Double = 15.0,
    val automatic: Boolean = true, val manualOrientation: PlanetCompassViewQuaternion = PlanetCompassViewQuaternion(),
    val returning: Boolean = false) {
    fun drag(dx: Double, dy: Double, extent: Double): PlanetCompassCelestialRotation {
        if (!dx.isFinite() || !dy.isFinite() || !extent.isFinite() || extent <= 0) return this
        val horizontal = dx / extent * 180
        val vertical = dy / extent * 180
        if (!horizontal.isFinite() || !vertical.isFinite()) return this
        // Positive screen X/Y move the visible surface right/down, respectively.
        val turn = PlanetCompassViewQuaternion.axisAngle(PlanetCompassViewVector(1.0, 0.0, 0.0), vertical) *
            PlanetCompassViewQuaternion.axisAngle(PlanetCompassViewVector(0.0, 1.0, 0.0), horizontal)
        return copy(manualOrientation = (turn * manualOrientation).unit(), automatic = false, returning = false)
    }
    fun advance(seconds: Double, retrograde: Boolean = false) = if (!automatic || !seconds.isFinite() || seconds < 0) this
        else copy(yaw = ((yaw + seconds.coerceAtMost(0.1) * (if (retrograde) -12 else 12)) % 360 + 360) % 360)
    fun resume() = copy(automatic = true, manualOrientation = PlanetCompassViewQuaternion(), returning = false)
    fun beginReturn() = if (automatic && !returning) this else copy(automatic = false, returning = true)
    fun geometry(): PlanetCompassCelestialViewGeometry {
        val a = Math.toRadians(yaw); val b = Math.toRadians(pitch)
        // Pole is upright in this illustrative view; turn about the body's own north axis.
        return PlanetCompassCelestialViewGeometry(manualOrientation.rotate(PlanetCompassViewVector(cos(a), -sin(a) * sin(b), sin(a) * cos(b))),
            manualOrientation.rotate(PlanetCompassViewVector(sin(a), cos(a) * sin(b), -cos(a) * cos(b))),
            manualOrientation.rotate(PlanetCompassViewVector(0.0, cos(b), sin(b))), PlanetCompassViewVector(-0.4, 0.3, 1.0).unit())
    }
}
