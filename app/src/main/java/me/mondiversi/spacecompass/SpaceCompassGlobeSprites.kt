package me.mondiversi.spacecompass

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream

/** Reuse the same offline/catalogue miniature renderer as the other sky/capture views. */
internal fun spaceCompassGlobeSpriteKey(item: SpaceCompassPanoramaObject,now: Long)=
    item.body.name+"@"+item.referenceTime(now)/60_000

internal fun spaceCompassGlobeSprites(context: Context,objects: List<SpaceCompassPanoramaObject>,now: Long): Map<String,String> =
    objects.associate { item ->
        val bitmap=renderSpaceCompassPanoramaMarker(context,item.body,item.referenceTime(now),64)
        val encoded=try { ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG,100,output)
            "data:image/png;base64,"+Base64.encodeToString(output.toByteArray(),Base64.NO_WRAP)
        } } finally { bitmap.recycle() }
        spaceCompassGlobeSpriteKey(item,now) to encoded
    }
