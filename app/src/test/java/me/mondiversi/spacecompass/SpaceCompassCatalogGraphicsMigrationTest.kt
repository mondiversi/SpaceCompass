package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.io.File
import java.security.MessageDigest

class SpaceCompassCatalogGraphicsMigrationTest {
    @Test fun oldMoonNamesResolveToTheSamePinnedBytesAndTheCurrentPublishedUrls() {
        val full=SpaceCompassCelestialTexturePolicy.texture("moon.webp")!!
        assertEquals("moon_jpl_0301.webp",full.name)
        assertSame(full,SpaceCompassCelestialTexturePolicy.texture(full.name))
        assertEquals(full.name,full.downloadName)
        assertTrue(full.url!!.endsWith("/celestial-textures-v1.6/moon_jpl_0301.webp"))
        val preview=SpaceCompassCelestialThumbnailPolicy.forImage("moon.webp")!!
        assertEquals("thumb_moon_jpl_0301.webp",preview.name)
        assertSame(preview,SpaceCompassCelestialTexturePolicy.texture("thumb_moon.webp"))
    }
    @Test fun legacyFilenameCacheImportsOnlyMatchingBytesAndKeepsTheOriginal() {
        val content="small verified test".toByteArray()
        val digest=MessageDigest.getInstance("SHA-256").digest(content).joinToString("") { "%02x".format(it.toInt() and 255) }
        val texture=SpaceCompassCelestialTexture("moon_jpl_0301.webp",content.size,digest,2,1,downloadName="moon.webp")
        val root=Files.createTempDirectory("catalogue-alias-import").toFile()
        try {
            val old=File(root,"old").apply { mkdir() };File(old,"moon.webp").writeBytes(content)
            val current=SpaceCompassCelestialTextureFiles(File(root,"current"))
            assertArrayEquals(content,current.importVerified(texture,listOf(old))!!.readBytes())
            assertArrayEquals(content,File(old,"moon.webp").readBytes())
            assertNotNull(current.cached(texture))
        } finally { root.walkBottomUp().forEach(File::delete) }
    }
    @Test fun canonicalPairsHaveCoherentNamesAndPinThePublishedGraphicsRelease() {
        val names=Regex("(moon|planet|star|dwarf|asteroid|comet|probe|satellite|cluster|nebula|galaxy|black_hole|fictional)_[a-z0-9_]+\\.webp")
        val policy=SpaceCompassCelestialTexturePolicy
        assertEquals(0,policy.images.count { it.downloadPack==null })
        policy.images.forEach { parent ->
            assertTrue(parent.name,names.matches(parent.name))
            val preview=SpaceCompassCelestialThumbnailPolicy.forImage(parent.name)!!
            assertEquals("thumb_"+parent.name,preview.name)
            assertEquals(policy.PACK,parent.downloadPack)
            assertEquals(policy.PACK,preview.downloadPack)
            assertTrue(policy.allowedUrl(requireNotNull(parent.url)))
            assertTrue(policy.allowedUrl(requireNotNull(preview.url)))
        }
    }
    @Test fun ambiguousOrEscapedSatelliteJsonCannotBypassIdentityAndChecksumValidation() {
        val body=SpaceCompassCelestialBody.SATELLITE_NORAD_39084
        val tle=javaClass.classLoader!!.getResource("catalog-999/satellite_norad_39084.tle")!!.readText().trim().lines()
        val json="[{\"tle1\":\"${tle[0]}\",\"tle2\":\"${tle[1]}\"}]"
        val parsed=spaceCompassSatnogsTle(json)
        val orbit=SpaceCompassCatalogSatelliteOrbit.parse(body,parsed)
        assertTrue(orbit.usable(orbit.epochMs))
        assertThrows(IllegalArgumentException::class.java) { spaceCompassSatnogsTle("[]") }
        assertThrows(IllegalArgumentException::class.java) { spaceCompassSatnogsTle(json.dropLast(1)+","+json.drop(1)) }
        assertThrows(IllegalArgumentException::class.java) { SpaceCompassCatalogSatelliteOrbit.parse(SpaceCompassCelestialBody.HUBBLE,parsed) }
    }
}
