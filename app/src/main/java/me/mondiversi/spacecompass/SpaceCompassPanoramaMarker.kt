package me.mondiversi.spacecompass

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/** Render the exact reference miniature used in the main view, including the frozen lunar phase. */
internal fun renderSpaceCompassPanoramaMarker(context: Context?, body: SpaceCompassCelestialBody,
    timeMs: Long, extent: Int = 128): Bitmap {
    val output = Bitmap.createBitmap(extent, extent, Bitmap.Config.ARGB_8888)
    var texture: Bitmap? = null
    try {
        if (!body.isSpacecraft && !body.isComet && body != SpaceCompassCelestialBody.EARTH_CENTER && !body.usesDeepSkySymbol)
            texture = createSpaceCompassCelestialThumbnail(context, body, extent)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(output.asImageBitmap()), Size(extent.toFloat(), extent.toFloat())) {
            when {
                body.isComet -> drawSpaceCompassCometSymbol(body)
                body == SpaceCompassCelestialBody.EARTH_CENTER -> drawSpaceCompassEarthCenterSymbol()
                body.usesDeepSkySymbol -> drawSpaceCompassDeepSkySymbol(body)
                body.isSpacecraft -> clipPath(Path().apply { addOval(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height)) }) {
                    drawSpaceCompassCelestialCraft(spaceCompassCelestialCraftMesh(body),
                        SpaceCompassCelestialRotation(yaw = 25.0, pitch = 50.0).geometry(), SpaceCompassCelestialViewportState())
                }
                else -> drawSpaceCompassCelestialThumbnail(texture?.asImageBitmap(),
                    if (body == SpaceCompassCelestialBody.MOON) calculateSpaceCompassMoonPhase(timeMs) else null)
            }
        }
        return output
    } catch (error: Throwable) { output.recycle(); throw error
    } finally { texture?.recycle() }
}
