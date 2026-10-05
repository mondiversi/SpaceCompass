package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import kotlin.math.*

internal data class SpaceCompassCraftFace(val a: SpaceCompassViewVector, val b: SpaceCompassViewVector, val c: SpaceCompassViewVector, val rgb: Int)

/** Recognisable structural models. No live spacecraft attitude is asserted by these meshes. */
internal fun spaceCompassCelestialCraftMesh(body: SpaceCompassCelestialBody): List<SpaceCompassCraftFace> = buildList {
    fun quad(a: SpaceCompassViewVector, b: SpaceCompassViewVector, c: SpaceCompassViewVector, d: SpaceCompassViewVector, color: Int) {
        add(SpaceCompassCraftFace(a, b, c, color)); add(SpaceCompassCraftFace(a, c, d, color))
    }
    fun box(x: Double, y: Double, z: Double, dx: Double, dy: Double, dz: Double, color: Int) {
        val p = listOf(SpaceCompassViewVector(x-dx,y-dy,z-dz), SpaceCompassViewVector(x+dx,y-dy,z-dz),
            SpaceCompassViewVector(x+dx,y+dy,z-dz), SpaceCompassViewVector(x-dx,y+dy,z-dz),
            SpaceCompassViewVector(x-dx,y-dy,z+dz), SpaceCompassViewVector(x+dx,y-dy,z+dz),
            SpaceCompassViewVector(x+dx,y+dy,z+dz), SpaceCompassViewVector(x-dx,y+dy,z+dz))
        listOf(intArrayOf(0,3,2,1), intArrayOf(4,5,6,7), intArrayOf(0,1,5,4),
            intArrayOf(3,7,6,2), intArrayOf(0,4,7,3), intArrayOf(1,2,6,5)).forEach {
            quad(p[it[0]],p[it[1]],p[it[2]],p[it[3]],color)
        }
    }
    if (body == SpaceCompassCelestialBody.ISS) {
        box(0.0,0.0,0.0,1.8,0.035,0.035,0xb8bcc6)
        box(0.0,0.0,0.0,0.13,1.0,0.13,0xe0e4ec)
        box(0.0,-0.05,0.0,0.48,0.13,0.13,0xd0d4df)
        box(0.0,0.72,0.0,0.37,0.12,0.12,0xcbc7bc)
        for (x in listOf(-1.4,-0.8,0.8,1.4)) for (y in listOf(-0.72,0.72)) {
            box(x,y,0.0,0.22,0.57,0.009,0x344f8a)
            // Thin cell seams make the eight arrays identifiable without image downloads.
            for (cell in -5..5) box(x,y+cell*0.095,0.013,0.215,0.004,0.004,0x74809b)
            box(x,y,0.014,0.004,0.56,0.003,0x74809b)
        }
        box(0.18,-0.36,0.2,0.5,0.028,0.028,0xdccfba)
    } else if (body == SpaceCompassCelestialBody.STARLINK_V3) {
        // Illustrative flat bus/solar arrays, not telemetry or asserted V3 engineering dimensions.
        box(0.0,0.0,0.0,0.52,0.34,0.055,0xe0e5eb)
        for (y in listOf(-1.03,1.03)) {
            box(0.0,y,0.0,0.03,0.72,0.02,0xaeb7c5)
            box(0.0,y,0.025,0.78,0.64,0.012,0x284e85)
            for (cell in -5..5) box(0.0,y+cell*0.11,0.04,0.77,0.003,0.003,0x8096b6)
            for (cell in -3..3) box(cell*0.21,y,0.04,0.003,0.63,0.003,0x8096b6)
        }
        box(0.0,0.0,0.075,0.39,0.23,0.015,0xb6c3d4)
    } else {
        box(0.0,0.0,-0.28,0.30,0.30,0.18,0xb89b53)
        // Parabolic high-gain dish, radius 1; segment positions follow z = 0.22 r².
        for (ring in 0..3) for (part in 0 until 32) {
            fun point(r: Double, a: Double) = SpaceCompassViewVector(r*cos(a),r*sin(a),0.22*r*r)
            val r0=ring/4.0; val r1=(ring+1)/4.0; val a0=part*2*PI/32; val a1=(part+1)*2*PI/32
            quad(point(r0,a0),point(r1,a0),point(r1,a1),point(r0,a1),0xe7e7db)
        }
        box(0.0,0.0,0.38,0.045,0.045,0.3,0xa6a7ab)
        box(0.86,0.0,-0.39,0.8,0.025,0.025,0xb9bdc6)
        box(1.64,0.0,-0.39,0.14,0.10,0.10,0x636879)
        box(-0.95,0.0,-0.35,0.68,0.028,0.028,0xb9bdc6)
        box(-1.65,0.0,-0.35,0.22,0.12,0.13,0x555c68)
        box(0.0,-1.02,-0.3,0.015,0.8,0.015,0xbfc2c8)
    }
}

@androidx.compose.runtime.Composable
internal fun SpaceCompassCelestialCraftCanvas(body: SpaceCompassCelestialBody, geometry: SpaceCompassCelestialViewGeometry,
    viewport: SpaceCompassCelestialViewportState, modifier: Modifier, colorFilter: ColorFilter? = null, opacity: Float = 1f) {
    val mesh = androidx.compose.runtime.remember(body) { spaceCompassCelestialCraftMesh(body) }
    Canvas(modifier) {
        drawRect(Color(0xff04060c))
        val scale = min(size.width,size.height)*0.225f*viewport.zoom.toFloat()
        fun project(v: SpaceCompassViewVector) = Offset(center.x+v.x.toFloat()*scale+viewport.panX.toFloat()*size.width/2,
            center.y-v.y.toFloat()*scale-viewport.panY.toFloat()*size.height/2)
        mesh.map { face -> face to listOf(geometry.toCamera(face.a),geometry.toCamera(face.b),geometry.toCamera(face.c)) }
            .sortedBy { (_,p) -> p.sumOf { it.z } }.forEach { (face,p) ->
                val normal=(p[1]-p[0]).cross(p[2]-p[0])
                val length=sqrt(normal.dot(normal))
                val brightness=if(length<1e-9)0.5 else 0.25+0.75*abs(normal.dot(geometry.light)/length)
                val path=Path().apply { val a=project(p[0]); moveTo(a.x,a.y)
                    p.drop(1).forEach { val b=project(it); lineTo(b.x,b.y) }; close() }
                drawPath(path,Color(((face.rgb shr 16 and 255)*brightness/255).toFloat(),
                    ((face.rgb shr 8 and 255)*brightness/255).toFloat(),((face.rgb and 255)*brightness/255).toFloat()),
                    colorFilter = colorFilter, alpha = opacity)
            }
    }
}
