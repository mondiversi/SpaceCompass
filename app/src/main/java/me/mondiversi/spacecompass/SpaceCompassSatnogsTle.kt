package me.mondiversi.spacecompass

/** Read exactly one public element pair. TLE fields are ASCII and cannot contain JSON escapes.
 * No names or JSON metadata replace the NORAD/COSPAR/checksum/epoch checks in the orbit parser. */
internal fun spaceCompassSatnogsTle(text: String): String {
    require(text.length <= 65_536 && text.trim().startsWith("[") && text.trim().endsWith("]"))
    fun field(name: String): String {
        val pattern = Regex("""["]${name}["]\s*:\s*["]([^"\\]+)["]""")
        val matches = pattern.findAll(text).map { it.groupValues[1] }.toList()
        require(matches.size == 1 && matches.single().length == 69) { "Missing or ambiguous satellite elements" }
        return matches.single()
    }
    return field("tle1") + "\n" + field("tle2")
}
