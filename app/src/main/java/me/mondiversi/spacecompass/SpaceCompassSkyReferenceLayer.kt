package me.mondiversi.spacecompass

import android.graphics.RectF
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp

/** Decorative only: all live guides use the same local viewport/lens as bodies and trajectories. */
@Composable
internal fun SpaceCompassSkyReferenceLayer(latitude: Double?, timeMs: Long?,
    orientation: SpaceCompassSunOrientation?, perspective: SpaceCompassPerspective?,
    exclusions: List<SpaceCompassSunSceneFrame>, enabled: Boolean = SPACE_COMPASS_SKY_REFERENCES_DEFAULT, observerAltitude: Double = 0.0, content: @Composable BoxScope.() -> Unit) {
    val ready = enabled && latitude != null && latitude.isFinite() && latitude in -90.0..90.0 && timeMs != null && orientation != null
    // Full circles do not depend on sidereal time. Cache trig across sensor frames;
    // the sub-microdegree daily obliquity change does not warrant rebuilding on each clock tick.
    val circles = remember(latitude, timeMs?.let { Math.floorDiv(it, 86_400_000L) }, ready) {
        if (ready) spaceCompassSkyReferenceCircles(requireNotNull(latitude), requireNotNull(timeMs), samplesPerCircle = 180)
        else emptyList()
    }
    val names = mapOf(
        SpaceCompassSkyReference.EQUATOR to stringResource(R.string.sky_reference_equator),
        SpaceCompassSkyReference.CANCER to stringResource(R.string.sky_reference_cancer),
        SpaceCompassSkyReference.CAPRICORN to stringResource(R.string.sky_reference_capricorn),
        SpaceCompassSkyReference.ARCTIC to stringResource(R.string.sky_reference_arctic),
        SpaceCompassSkyReference.ANTARCTIC to stringResource(R.string.sky_reference_antarctic))
    val poles = stringResource(R.string.sky_reference_north_pole) to stringResource(R.string.sky_reference_south_pole)
    val observerPoints = stringResource(R.string.celestial_earth_center) to stringResource(R.string.sky_reference_zenith)
    val scale = with(LocalDensity.current) { 12.sp.toPx() / 24f }
    val numericFormat = LocalSpaceCompassNumericFormat.current
    val guides = if (!ready) Modifier else Modifier.drawWithContent {
        if (size.width <= 0f || size.height <= 0f) { drawContent(); return@drawWithContent }
        val occupied = exclusions.map { frame -> RectF(frame.left.toFloat(), frame.top.toFloat(),
            (frame.left + frame.width).toFloat(), (frame.top + frame.height).toFloat()) }.toMutableList()
        val canvas = drawContext.canvas.nativeCanvas
        canvas.save(); canvas.clipRect(0f, 0f, size.width, size.height)
        val drawing = drawSpaceCompassSkyReferences(canvas, requireNotNull(latitude), requireNotNull(timeMs),
            size.width.toInt(), size.height.toInt(), scale, occupied, orientation, perspective,
            circles = circles, referenceNames = names, poleNames = poles, allowLabelOverlap = false,
            numericFormat = numericFormat, observerPointNames = observerPoints, observerAltitude = observerAltitude)
        // Curves/crosses stay behind bodies; all shaped names and point captions are drawn once in front.
        drawSpaceCompassSkyReferenceLabels(canvas, drawing.copy(labels = emptyList(), curveLabels = emptyList()), scale)
        canvas.restore()
        drawContent()
        canvas.save(); canvas.clipRect(0f, 0f, size.width, size.height)
        drawSpaceCompassSkyReferenceLabels(canvas, drawing.copy(poles = emptyList()), scale)
        canvas.restore()
    }
    Box(Modifier.fillMaxSize().then(guides).testTag("sky-reference-guides"), content = content)
}
