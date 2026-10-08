package me.mondiversi.spacecompass

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/** One cached native Canvas renderer for the live backdrop and frozen virtual panoramas. */
internal class SpaceCompassWeatherRenderer(width: Float, height: Float,
    phase: SpaceCompassSunSkyPhase, weather: SpaceCompassSunWeatherSnapshot?) {
    private val scene = spaceCompassWeatherScene(width, height, weather)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val night = phase == SpaceCompassSunSkyPhase.NIGHT || phase == SpaceCompassSunSkyPhase.EVENING
    private val cloudColor = if (scene.kind == SpaceCompassSunWeatherKind.STORM) 0xFF455263.toInt()
        else if (night) 0xFF7B8B9F.toInt() else 0xFFFFFFFF.toInt()
    private val clouds = scene.clouds.map { cloud -> Path().apply {
        for (part in 0..3) {
            val radius = cloud.radius * if (part in 1..2) 1.3f else .9f
            val x = cloud.x + (part - 1.5f) * cloud.lobeSpacing
            addOval(RectF(x - radius, cloud.y - radius, x + radius, cloud.y + radius), Path.Direction.CW)
        }
    } }
    private val bolt = Path().apply {
        val x = width * .78f; val y = height * .16f; val r = scene.unit * .055f
        moveTo(x, y); lineTo(x - r * .3f, y + r)
        lineTo(x + r * .1f, y + r * .8f); lineTo(x - r * .1f, y + r * 1.6f)
    }

    fun draw(canvas: Canvas) {
        if (scene.kind == null) return
        val saved = canvas.save()
        try {
            canvas.clipRect(0f, 0f, scene.width, scene.height)
            paint.style = Paint.Style.FILL
            // A genuinely overcast sky needs a continuous deck, not isolated decorative puffs.
            paint.color = cloudColor; paint.alpha = (scene.veilAlpha * 255).toInt()
            if (scene.veilAlpha > 0) canvas.drawRect(0f, 0f, scene.width, scene.height, paint)
            paint.alpha = (scene.cloudAlpha * 255).toInt()
            clouds.forEach { canvas.drawPath(it, paint) }
            if (scene.fogAlpha > 0) {
                paint.color = if (night) 0xFF8995A3.toInt() else 0xFFD8DFE2.toInt()
                paint.alpha = (scene.fogAlpha * 255).toInt()
                canvas.drawRect(0f, 0f, scene.width, scene.height, paint)
            }
            when (scene.kind) {
                SpaceCompassSunWeatherKind.DRIZZLE, SpaceCompassSunWeatherKind.RAIN, SpaceCompassSunWeatherKind.STORM -> {
                    paint.color = 0x4DE3F2FF; paint.strokeWidth = scene.unit * .0016f
                    scene.precipitation.forEach { point ->
                        canvas.drawLine(point.x, point.y, point.x - scene.unit * .006f,
                            point.y + scene.unit * .018f, paint)
                    }
                    if (scene.kind == SpaceCompassSunWeatherKind.STORM) {
                        // One static motif, with identical proportions in both views; never flashing.
                        paint.color = 0x99FFDB87.toInt(); paint.style = Paint.Style.STROKE
                        paint.strokeWidth = scene.unit * .002f
                        canvas.drawPath(bolt, paint)
                    }
                }
                SpaceCompassSunWeatherKind.SNOW -> {
                    paint.color = 0xA6FFFFFF.toInt()
                    scene.precipitation.forEach { canvas.drawCircle(it.x, it.y, it.size, paint) }
                }
                else -> Unit
            }
        } finally { canvas.restoreToCount(saved) }
    }
}
