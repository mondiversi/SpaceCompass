package me.mondiversi.spacecompass

/** A write-only destination: published drawing paths are never mutated by the next frame. */
internal interface SpaceCompassLiveGridSink {
    fun moveTo(x: Double, y: Double)
    fun lineTo(x: Double, y: Double)
}

/** Single-worker scratch storage. Uses the capture grid's exact 2-degree samples and clipping planes,
 * but transforms each vertex once and emits only visible segments without per-segment objects. */
internal class SpaceCompassLiveGridProjector(private val width: Double, private val height: Double,
    perspective: SpaceCompassPerspective, step: Int = 10) {
    init { require(width.isFinite() && height.isFinite() && width > 0 && height > 0) }
    private val curves = spaceCompassCameraGridCurves(step)
    private val capacity = curves.maxOf { it.rays.size }
    private val coordinates = DoubleArray(capacity * 3)
    private val masks = IntArray(capacity)
    private val fx = perspective.focalX(width)
    private val fy = perspective.focalY(height)
    private val cx = perspective.principalX * width
    private val cy = perspective.principalY * height

    private fun side(plane: Int, x: Double, y: Double, z: Double): Double = when(plane) {
        0 -> z - 1e-5
        1 -> fx * x + cx * z
        2 -> -fx * x + (width - cx) * z
        3 -> fy * y + cy * z
        else -> -fy * y + (height - cy) * z
    }

    /** Returns the number of emitted segments; no frame-rate cap or pose quantization is applied. */
    fun project(orientation: SpaceCompassSunOrientation, sink: SpaceCompassLiveGridSink): Int {
        var visible = 0
        val right = orientation.right; val up = orientation.screenUp; val forward = orientation.forward
        for(curve in curves) {
            val rays = curve.rays
            for(i in rays.indices) {
                val ray = rays[i]; val j = i * 3
                val x = ray.dot(right); val y = -ray.dot(up); val z = ray.dot(forward)
                coordinates[j] = x; coordinates[j+1] = y; coordinates[j+2] = z
                var mask = 0
                for(plane in 0..4) if(side(plane,x,y,z) < 0) mask = mask or (1 shl plane)
                masks[i] = mask
            }
            var penX = Double.NaN; var penY = Double.NaN
            for(i in 1 until rays.size) {
                val fromMask = masks[i-1]; val toMask = masks[i]
                // An entire segment outside any common half-space cannot contribute a pixel.
                if((fromMask and toMask) != 0) continue
                val a = (i-1)*3; val b = i*3
                val ax=coordinates[a]; val ay=coordinates[a+1]; val az=coordinates[a+2]
                val bx=coordinates[b]; val by=coordinates[b+1]; val bz=coordinates[b+2]
                var low=0.0; var high=1.0
                val crossed = fromMask or toMask
                if(crossed != 0) {
                    for(plane in 0..4) {
                        if((crossed and (1 shl plane)) == 0) continue
                        val start=side(plane,ax,ay,az); val end=side(plane,bx,by,bz)
                        if((start < 0) != (end < 0)) {
                            val t=start/(start-end)
                            if(start < 0) low=maxOf(low,t) else high=minOf(high,t)
                        }
                    }
                    if(low > high) continue
                }
                val fromZ=az+(bz-az)*low; val toZ=az+(bz-az)*high
                val x1=(cx+fx*(ax+(bx-ax)*low)/fromZ).coerceIn(0.0,width)
                val y1=(cy+fy*(ay+(by-ay)*low)/fromZ).coerceIn(0.0,height)
                val x2=(cx+fx*(ax+(bx-ax)*high)/toZ).coerceIn(0.0,width)
                val y2=(cy+fy*(ay+(by-ay)*high)/toZ).coerceIn(0.0,height)
                val dx=penX-x1; val dy=penY-y1
                if(penX.isNaN() || dx*dx+dy*dy > .04) sink.moveTo(x1,y1)
                sink.lineTo(x2,y2)
                penX=x2; penY=y2; visible++
            }
        }
        return visible
    }
}
