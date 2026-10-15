package dev.thabit.core

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsFileTest {
    private val fs = FakeFileSystem()
    private val vault = Vault(fs, "/v".toPath(), Clock.System, TimeZone.UTC)
    private val path = "/v/.lantern/settings.json".toPath()

    @Test fun brokenSettingsReadAsDefaultsButTheFileIsKept() {
        fs.write(path) { writeUtf8("{ \"mode\": ") }
        assertEquals(AppSettings(), vault.settings())
        val kept = fs.list("/v/.lantern".toPath()).single { it.name.startsWith("settings.json.corrupt-") }
        assertEquals("{ \"mode\": ", fs.read(kept) { readUtf8() })
        assertEquals(listOf(kept.name), vault.files.corruptCopies)
    }

    @Test fun savingSettingsAfterABrokenReadDoesNotLoseTheOldBytes() {
        fs.write(path) { writeUtf8("{ \"mode\": ") }
        vault.settings()
        vault.saveSettings(AppSettings(mode = "dark"))
        assertEquals("dark", vault.settings().mode)
        assertTrue(fs.list("/v/.lantern".toPath()).any { it.name.startsWith("settings.json.corrupt-") })
    }
}
