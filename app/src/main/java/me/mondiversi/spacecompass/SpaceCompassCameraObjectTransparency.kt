package me.mondiversi.spacecompass

internal const val SPACE_COMPASS_CAMERA_OBJECT_TRANSLUCENCY_KEY = "camera_objects_translucent"
internal const val SPACE_COMPASS_CAMERA_TRANSLUCENT_OBJECT_OPACITY = .5f

/** A remembered camera aid must never fade the virtual sky, catalogue or Observatory. */
internal fun spaceCompassCameraObjectOpacity(cameraEnabled: Boolean, translucent: Boolean): Float =
    if (cameraEnabled && translucent) SPACE_COMPASS_CAMERA_TRANSLUCENT_OBJECT_OPACITY else 1f
