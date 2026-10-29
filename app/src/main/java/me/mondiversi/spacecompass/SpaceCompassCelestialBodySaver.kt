package me.mondiversi.spacecompass

import androidx.compose.runtime.saveable.Saver

/** Android restoration stores stable scientific identifiers, never implementation classes or ordinals. */
internal val spaceCompassCelestialBodySaver = Saver<SpaceCompassCelestialBody,String>(
    save = { it.name }, restore = { runCatching { SpaceCompassCelestialBody.valueOf(it) }.getOrNull() })
internal val spaceCompassOptionalCelestialBodySaver = Saver<SpaceCompassCelestialBody?,String>(
    save = { it?.name.orEmpty() }, restore = { name ->
        name.takeIf { it.isNotEmpty() }?.let { runCatching { SpaceCompassCelestialBody.valueOf(it) }.getOrNull() }
    })
