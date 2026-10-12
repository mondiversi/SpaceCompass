package me.mondiversi.spacecompass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt

/** Render the model alone with its captured framing, without buttons, gestures or a second exposure. */
internal fun renderSpaceCompassObservatoryImage(context: Context, images: SpaceCompassObservatoryImages,
    current: Boolean, presentation: SpaceCompassObservatoryPresentation): Bitmap {
    val source=images.size
    val ratio=if (source.width>0 && source.height>0) source.width.toDouble()/source.height else 1.0
    val width=if (ratio>=1) 1536 else (1536*ratio).roundToInt().coerceAtLeast(256)
    val height=if (ratio>=1) (1536/ratio).roundToInt().coerceAtLeast(256) else 1536
    val body=images.body
    val geometry=if (current) images.current else images.rotation
    val symbolic=body.usesDeepSkySymbol || body.isSpacecraft || body.isComet || body.hasCatalogPhotograph || body==SpaceCompassCelestialBody.EARTH_CENTER
    val bitmap=if (!symbolic && geometry!=null) renderSpaceCompassCelestialOffscreen(context,body,geometry,
        images.viewport,current,width,height) else Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888).apply {
        eraseColor(0xff04060c.toInt())
    }
    try {
        val canvas=Canvas(bitmap)
        if (symbolic) {
            if (body.hasCatalogPhotograph) {
                val map=requireNotNull(SpaceCompassCelestialTextures.openImage(context,requireNotNull(body.viewerTexture))).use {
                    requireNotNull(BitmapFactory.decodeStream(it)) }
                try {
                    val fit=minOf(width.toFloat()/map.width,height.toFloat()/map.height)*images.viewport.zoom.toFloat()
                    val left=(width-map.width*fit)/2+images.viewport.panX.toFloat()*width/2
                    val top=(height-map.height*fit)/2-images.viewport.panY.toFloat()*height/2
                    canvas.drawBitmap(map,null,RectF(left,top,left+map.width*fit,top+map.height*fit),
                        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                } finally { map.recycle() }
            } else CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,
                androidx.compose.ui.graphics.Canvas(canvas),Size(width.toFloat(),height.toFloat())) {
                if (body.isSpacecraft) drawSpaceCompassCelestialCraft(spaceCompassCelestialCraftMesh(body),
                    requireNotNull(geometry),images.viewport)
                else translate(images.viewport.panX.toFloat()*width/2,-images.viewport.panY.toFloat()*height/2) {
                    scale(images.viewport.zoom.toFloat()) {
                        when {
                            body.isComet -> drawSpaceCompassCometSymbol(body)
                            body==SpaceCompassCelestialBody.EARTH_CENTER -> drawSpaceCompassEarthCenterSymbol()
                            else -> drawSpaceCompassDeepSkySymbol(body)
                        }
                    }
                }
            }
        } else if (geometry==null) {
            val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=android.graphics.Color.WHITE;textSize=22f;textAlign=Paint.Align.CENTER }
            canvas.drawText(presentation.missingImage,width/2f,height/2f,paint)
        }
        val caption=if (current) presentation.currentCaption else presentation.rotationCaption
        val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=android.graphics.Color.WHITE;textSize=22f;textAlign=Paint.Align.CENTER }
        paint.textSize=(paint.textSize*(width-32f)/paint.measureText(caption).coerceAtLeast(width-32f)).coerceAtLeast(10f)
        val captionHeight=paint.textSize*2.3f
        canvas.drawRect(0f,0f,width.toFloat(),captionHeight,Paint().apply { color=0xbb000000.toInt() })
        canvas.drawText(caption,width/2f,captionHeight/2-(paint.ascent()+paint.descent())/2,paint)
        return bitmap
    } catch (error: Throwable) { bitmap.recycle();throw error }
}
