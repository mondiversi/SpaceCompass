package me.mondiversi.spacecompass

import io.github.cosinekitty.astronomy.AxisInfo
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.Vector
import kotlin.math.cos
import kotlin.math.sin

/** IAU 2015 / NAIF pck00011.tpc, BODY602 pole and prime-meridian coefficients. */
internal fun spaceCompassEnceladusAxis(time: Time): AxisInfo {
    val centuries = time.tt / 36525.0
    val ra = 40.66 - .036 * centuries
    val dec = 83.52 - .004 * centuries
    val r = Math.toRadians(ra); val c = Math.toRadians(dec)
    return AxisInfo(ra / 15, dec, 6.32 + 262.7318996 * time.tt,
        Vector(cos(c) * cos(r), cos(c) * sin(r), sin(c), time))
}
