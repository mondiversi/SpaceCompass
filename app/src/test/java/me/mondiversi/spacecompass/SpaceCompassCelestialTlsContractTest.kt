package me.mondiversi.spacecompass

import java.io.File
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test
import org.w3c.dom.Element

class SpaceCompassCelestialTlsContractTest {
    private val main = listOf(File("src/main"), File("app/src/main")).first { it.isDirectory }
    private fun xml(path: String) = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    }.newDocumentBuilder().parse(File(main, path))

    @Test fun extraPublicRootIsLimitedToTheExactJplHost() {
        val manifest = xml("AndroidManifest.xml")
        val application = manifest.getElementsByTagName("application").item(0) as Element
        assertEquals("@xml/network_security_config", application.getAttributeNS(
            "http://schemas.android.com/apk/res/android", "networkSecurityConfig"))
        val config = xml("res/xml/network_security_config.xml")
        assertEquals(0, config.getElementsByTagName("base-config").length)
        assertEquals(0, config.getElementsByTagName("debug-overrides").length)
        assertEquals(1, config.getElementsByTagName("domain-config").length)
        val domainConfig = config.getElementsByTagName("domain-config").item(0) as Element
        assertEquals("false", domainConfig.getAttribute("cleartextTrafficPermitted"))
        assertEquals(1, config.getElementsByTagName("domain").length)
        val domain = config.getElementsByTagName("domain").item(0) as Element
        assertEquals("ssd.jpl.nasa.gov", domain.textContent.trim())
        assertEquals("false", domain.getAttribute("includeSubdomains"))
        val anchors = config.getElementsByTagName("certificates")
        assertEquals(setOf("system", "@raw/sectigo_server_root_r46"),
            (0 until anchors.length).map { (anchors.item(it) as Element).getAttribute("src") }.toSet())
        assertEquals(2, anchors.length)
    }

    @Test fun bundledCaMatchesIndependentlyVerifiedMozillaFingerprint() {
        val certificates = File(main, "res/raw/sectigo_server_root_r46.pem").inputStream().use {
            CertificateFactory.getInstance("X.509").generateCertificates(it)
        }
        assertEquals(1, certificates.size)
        val cert = certificates.single() as X509Certificate
        val sha256 = MessageDigest.getInstance("SHA-256").digest(cert.encoded)
            .joinToString("") { "%02X".format(it.toInt() and 0xff) }
        assertEquals("7BB647A62AEEAC88BF257AA522D01FFEA395E0AB45C73F93F65654EC38F25A06", sha256)
        assertEquals(cert.subjectX500Principal, cert.issuerX500Principal)
        assertTrue(cert.basicConstraints >= 0)
        assertTrue(cert.keyUsage[5]) // Certificate-signing CA, not the server's expiring leaf.
        cert.verify(cert.publicKey)
    }
}
