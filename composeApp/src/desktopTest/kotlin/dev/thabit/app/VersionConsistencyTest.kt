package dev.thabit.app

import dev.thabit.core.ReleaseNotes
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The version lives in a few hand-edited places; a release is wrong when they disagree. */
class VersionConsistencyTest {
    @Test fun brandGradleChangelogAndWhatsNewAgree() {
        val gradle = File("build.gradle.kts").readText()
        assertEquals(Brand.VERSION, Regex("""(?:val appVersion|versionName) = "([^"]+)"""").find(gradle)?.groupValues?.get(1), "the app version in composeApp/build.gradle.kts")
        assertTrue(File("../CHANGELOG.md").readLines().any { it == "## ${Brand.VERSION}" }, "CHANGELOG.md needs a '## ${Brand.VERSION}' section")
        assertTrue(ReleaseNotes.forVersion(Brand.VERSION).isNotEmpty())
        assertTrue(ReleaseNotes.forVersion(Brand.VERSION) != ReleaseNotes.forVersion("0.0.1"), "What's new needs its own note for ${Brand.VERSION}")
    }
}
