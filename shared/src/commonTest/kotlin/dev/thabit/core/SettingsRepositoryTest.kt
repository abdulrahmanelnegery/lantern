package dev.thabit.core

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsRepositoryTest {
    private val fs = FakeFileSystem()
    private val vault = Vault(fs, "/v".toPath(), Clock.System, TimeZone.UTC)

    @Test fun startsWithDefaults() = assertEquals(AppSettings(), SettingsRepository(vault).current)

    @Test fun updateRoundTripsThroughTheFile() {
        SettingsRepository(vault).update { it.copy(palette = "ocean", textSize = 2, hiddenSections = setOf("insights"), sectionOrder = listOf("tasks")) }
        val again = SettingsRepository(vault).current
        assertEquals("ocean", again.palette); assertEquals(2, again.textSize); assertEquals(setOf("insights"), again.hiddenSections)
    }

    @Test fun updateNormalisesWhatItSaves() {
        val repo = SettingsRepository(vault)
        repo.update { it.copy(textSize = 40, palette = "nope") }
        assertEquals(3, vault.settings().textSize); assertEquals(PaletteIds.DEFAULT, vault.settings().palette)
    }

    @Test fun aHandEditedFileIsBroughtIntoRange() {
        fs.createDirectories("/v/.lantern".toPath())
        fs.write("/v/.lantern/settings.json".toPath()) { writeUtf8("""{"textSize":-5,"density":"huge"}""") }
        val s = SettingsRepository(vault).current
        assertEquals(0, s.textSize); assertEquals("comfortable", s.density)
    }
}
