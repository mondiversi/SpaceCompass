package me.mondiversi.spacecompass

import kotlin.math.*

internal data class SpaceCompassGeographicGridCurve(val degrees: Int,val latitude: Boolean,
    val vertices: List<SpaceCompassGlobeVector>)

/** Independent 15-degree geographic reference; named tropics/polar circles remain a separate layer. */
internal val spaceCompassGeographicGrid: List<SpaceCompassGeographicGridCurve> = buildList {
    for(degrees in -75..75 step 15) {
        val phi=Math.toRadians(degrees.toDouble())
        add(SpaceCompassGeographicGridCurve(degrees,true,(0..120).map { i ->
            val theta=Math.toRadians(i*3.0)
            SpaceCompassGlobeVector(cos(phi)*sin(theta),sin(phi),cos(phi)*cos(theta))
        }))
    }
    for(degrees in -180 until 180 step 15) {
        val theta=Math.toRadians(degrees.toDouble())
        add(SpaceCompassGeographicGridCurve(degrees,false,(0..60).map { i ->
            val phi=Math.toRadians(i*3.0-90)
            SpaceCompassGlobeVector(cos(phi)*sin(theta),sin(phi),cos(phi)*cos(theta))
        }))
    }
}

/** Positive zero uses the conventional north/east suffix to distinguish the two axes. */
internal fun spaceCompassGeographicGridLabel(degrees: Int,latitude: Boolean,
    formatting: SpaceCompassPanoramaFormatting,directions: List<String> = listOf("N","E","S","W")): String {
    require(degrees in if(latitude) -90..90 else -180..180)
    val symbols=directions.takeIf { it.size==4 && it.all(String::isNotBlank) } ?: listOf("N","E","S","W")
    val direction=symbols[if(latitude) { if(degrees<0) 2 else 0 } else { if(degrees<0) 3 else 1 }]
    return formatSpaceCompassNumber(kotlin.math.abs(degrees.toDouble()),0,formatting.numeric,
        grouping=false,systemLocale=formatting.deviceLocale)+"° "+direction
}
