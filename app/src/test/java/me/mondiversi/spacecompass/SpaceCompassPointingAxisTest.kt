package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassPointingAxisTest {
    private val east = SpaceCompassSunVector(1.0, 0.0, 0.0)
    private val north = SpaceCompassSunVector(0.0, 1.0, 0.0)
    private val up = SpaceCompassSunVector(0.0, 0.0, 1.0)
    private fun neg(v: SpaceCompassSunVector) = SpaceCompassSunVector(-v.east, -v.north, -v.up)
    private val flat = SpaceCompassSunOrientation(east, north, neg(up))
    private fun screen(pose: SpaceCompassSunOrientation, rotation: Int) = when (rotation) {
        // Android remap describes the old axes in the new frame, an inverse basis change.
        1 -> SpaceCompassSunOrientation(neg(pose.screenUp), pose.right, pose.forward)
        2 -> SpaceCompassSunOrientation(neg(pose.right), neg(pose.screenUp), pose.forward)
        3 -> SpaceCompassSunOrientation(pose.screenUp, neg(pose.right), pose.forward)
        else -> pose
    }

    @Test fun defaultReferenceRetainsTheExactCameraPose() {
        for (rotation in 0..3) {
            val pose = screen(flat, rotation)
            assertSame(pose, spaceCompassSunViewOrientation(pose, false, false, rotation))
        }
    }

    @Test fun aFlatPhonePointsAtTheHorizonAlongItsLengthInsteadOfAtTheGround() {
        val pose = spaceCompassSunViewOrientation(flat, true, false, 0)!!
        assertEquals(0.0, pose.headingDegrees, 1e-12)
        assertEquals(0.0, pose.tiltDegrees, 1e-12)
        assertEquals(-90.0, flat.tiltDegrees, 1e-12)
        assertEquals(east, pose.right)
        assertEquals(up, pose.screenUp)
        assertEquals(north, pose.forward)
    }

    @Test fun portraitLandscapeAndReverseRotationsKeepTheSamePhysicalLongAxis() {
        val tilted = SpaceCompassSunOrientation(east, up, north)
        for (device in listOf(flat, tilted)) {
            val expected = spaceCompassSunViewOrientation(device, true, false, 0)
            for (rotation in 0..3) assertEquals(expected,
                spaceCompassSunViewOrientation(screen(device, rotation), true, false, rotation))
        }
    }

    @Test fun cameraModeAlwaysUsesTheOpticalAxisWithoutChangingTheSavedVirtualChoice() {
        for (rotation in 0..3) {
            val pose = screen(flat, rotation)
            assertSame(pose, spaceCompassSunViewOrientation(pose, true, true, rotation))
            assertEquals(north, spaceCompassSunViewOrientation(pose, true, false, rotation)!!.forward)
        }
    }

    @Test fun bothReferencesKeepAnOrthonormalBasisAndTheCorrectHandedness() {
        for (rotation in 0..3) for (topEdge in listOf(false, true)) {
            val pose = spaceCompassSunViewOrientation(screen(flat, rotation), topEdge, false, rotation)!!
            assertEquals(1.0, pose.right.dot(pose.right), 1e-12)
            assertEquals(1.0, pose.screenUp.dot(pose.screenUp), 1e-12)
            assertEquals(1.0, pose.forward.dot(pose.forward), 1e-12)
            assertEquals(0.0, pose.right.dot(pose.screenUp), 1e-12)
            assertEquals(0.0, pose.right.dot(pose.forward), 1e-12)
            assertEquals(0.0, pose.screenUp.dot(pose.forward), 1e-12)
            val a = pose.right; val b = pose.screenUp
            val cross = SpaceCompassSunVector(a.north*b.up-a.up*b.north,
                a.up*b.east-a.east*b.up, a.east*b.north-a.north*b.east)
            assertEquals(-1.0, cross.dot(pose.forward), 1e-12)
        }
    }

    @Test fun theReticleAndGroundHorizonUseTheSameChangedReference() {
        val pose = spaceCompassSunViewOrientation(flat, true, false, 0)!!
        val frame = SpaceCompassSunSceneFrame(0.0, 0.0, 400.0, 600.0)
        val objectPosition = projectSpaceCompassSun(SpaceCompassSunPosition(0.0, 0.0), pose, frame.width, frame.height)
        assertTrue(objectPosition.visible)
        assertEquals(200.0, objectPosition.x, 1e-10)
        assertEquals(300.0, objectPosition.y, 1e-10)
        val horizon = projectSpaceCompassSunGround(pose, frame, frame.width, frame.height)!!.horizon
        assertEquals(2, horizon.size)
        horizon.forEach { assertEquals(objectPosition.y, it.y, 1e-10) }
    }

    @Test fun missingSensorsDoNotCreateAFictitiousReference() {
        assertNull(spaceCompassSunViewOrientation(null, true, false, 0))
        assertNull(spaceCompassSunViewOrientation(null, true, true, 1))
    }
}
