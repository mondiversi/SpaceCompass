package me.mondiversi.spacecompass

import android.graphics.RectF
import androidx.compose.runtime.staticCompositionLocalOf

/** Draw-pass scratch space, scoped to one sky viewport; never causes recomposition or alters hit targets. */
internal val LocalSpaceCompassOrbitLabelReservations = staticCompositionLocalOf<MutableList<RectF>?> { null }
