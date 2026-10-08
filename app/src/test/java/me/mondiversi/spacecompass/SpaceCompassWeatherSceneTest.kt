package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class SpaceCompassWeatherSceneTest {
    private fun weather(kind: SpaceCompassSunWeatherKind, cover: Float) =
        SpaceCompassSunWeatherSnapshot(kind, cover, 1_800_000_000_000L)
    private fun scene(kind: SpaceCompassSunWeatherKind, cover: Float, width: Float = 1000f, height: Float = 1000f) =
        spaceCompassWeatherScene(width, height, weather(kind, cover))

    @Test fun fullOvercastHasNoUncoveredBlueHolesInEitherAspectRatio() {
        for ((w, h) in listOf(1000f to 1800f, 4096f to 1024f)) {
            val sky = scene(SpaceCompassSunWeatherKind.CLOUDY, 1f, w, h)
            assertTrue(sky.veilAlpha >= .8f)
            assertTrue(sky.clouds.isNotEmpty())
            assertTrue(sky.precipitation.isEmpty())
        }
    }

    @Test fun increasingCoverKeepsCloudsAndIncreasesTheirSizeAndOpacity() {
        val skies = listOf(.2f, .4f, .6f, .8f, 1f).map { scene(SpaceCompassSunWeatherKind.PARTLY_CLOUDY, it) }
        skies.zipWithNext().forEach { (before, after) ->
            assertTrue(after.clouds.size >= before.clouds.size)
            assertTrue(after.cloudAlpha >= before.cloudAlpha && after.veilAlpha >= before.veilAlpha)
            before.clouds.forEach { cloud ->
                val next = after.clouds.single { it.x == cloud.x && it.y == cloud.y }
                assertTrue(next.radius >= cloud.radius)
            }
        }
    }

    @Test fun rainSnowAndDrizzleDensityRemainProportionalToVisibleSkyArea() {
        for (kind in listOf(SpaceCompassSunWeatherKind.DRIZZLE, SpaceCompassSunWeatherKind.RAIN,
            SpaceCompassSunWeatherKind.SNOW, SpaceCompassSunWeatherKind.STORM)) {
            val a = scene(kind, .8f, 1000f, 1800f)
            val b = scene(kind, .8f, 4096f, 1024f)
            fun density(s: SpaceCompassWeatherScene) = s.precipitation.size / (s.width / s.unit * s.height / s.unit)
            assertEquals(density(a), density(b), .6f)
            assertEquals(a.veilAlpha, b.veilAlpha, 0f)
            assertEquals(a.cloudAlpha, b.cloudAlpha, 0f)
        }
        assertTrue(scene(SpaceCompassSunWeatherKind.DRIZZLE, .8f).precipitation.size <
            scene(SpaceCompassSunWeatherKind.RAIN, .8f).precipitation.size)
        assertTrue(scene(SpaceCompassSunWeatherKind.RAIN, .8f).precipitation.size <
            scene(SpaceCompassSunWeatherKind.STORM, .8f).precipitation.size)
    }

    @Test fun disabledOrClearWeatherNeverProducesCloudsFogOrPrecipitation() {
        for (sky in listOf(spaceCompassWeatherScene(1000f, 1800f, null),
            scene(SpaceCompassSunWeatherKind.CLEAR, 1f))) {
            assertEquals(0f, sky.cover, 0f)
            assertEquals(0f, sky.veilAlpha, 0f)
            assertEquals(0f, sky.fogAlpha, 0f)
            assertTrue(sky.clouds.isEmpty() && sky.precipitation.isEmpty())
        }
    }

    @Test fun fogAndCloudyLabelsCannotAccidentallyProduceAStarryClearSky() {
        assertTrue(scene(SpaceCompassSunWeatherKind.FOG, 0f).fogAlpha > .4f)
        for (cover in listOf(0f, Float.NaN, Float.POSITIVE_INFINITY)) {
            val snapshot = weather(SpaceCompassSunWeatherKind.CLOUDY, cover)
            assertTrue(spaceCompassSunDisplayCloudCover(snapshot) >= .75f)
            assertFalse(spaceCompassSunDisplayStars(snapshot))
        }
    }

    @Test fun visualProfilesAreScaleIndependentForEveryWeatherKind() {
        for (kind in SpaceCompassSunWeatherKind.entries) {
            val a = scene(kind, .72f, 1000f, 1000f)
            val b = scene(kind, .72f, 2000f, 2000f)
            assertEquals(a.clouds.size, b.clouds.size)
            assertEquals(a.precipitation.size, b.precipitation.size)
            a.clouds.zip(b.clouds).forEach { (first, second) ->
                assertEquals(first.x / a.width, second.x / b.width, .00001f)
                assertEquals(first.y / a.height, second.y / b.height, .00001f)
                assertEquals(first.radius / a.unit, second.radius / b.unit, .00001f)
            }
            assertEquals(a.cloudAlpha, b.cloudAlpha, 0f)
            assertEquals(a.veilAlpha, b.veilAlpha, 0f)
            assertEquals(a.fogAlpha, b.fogAlpha, 0f)
        }
    }
}
