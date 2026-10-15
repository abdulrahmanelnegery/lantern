package dev.thabit.core

import okio.Buffer
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import java.util.zip.ZipInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import dev.thabit.habits.Habit
import dev.thabit.tasks.Task

class VaultZipTest {
    @Test fun exportHasTheVaultFilesButNotTheTrash() {
        val fs = FakeFileSystem()
        val vault = Vault(fs, "/v".toPath())
        vault.saveTasks(listOf(Task("t1", "Task one")))
        vault.saveHabits(listOf(Habit("h1", "Keep me")))
        fs.write("/v/.lantern/trash/bin-me-aaaa1111.md".toPath()) { writeUtf8("trashed") }

        val buffer = Buffer()
        zipVault(fs, "/v".toPath(), buffer)
        val names = ArrayList<String>()
        ZipInputStream(buffer.inputStream()).use { zip -> generateSequence { zip.nextEntry }.forEach { names += it.name } }

        assertTrue("Thabit/habits/habits.json" in names, names.toString())
        assertTrue("Thabit/tasks/Tasks.md" in names, names.toString())
        assertEquals(0, names.count { it.contains("bin-me") }, "the trash stays out of an export")
    }

    @Test fun earlierExportsAreNotZippedIntoTheNextOne() {
        val fs = FakeFileSystem()
        val vault = Vault(fs, "/v".toPath())
        vault.saveTasks(listOf(Task("t1", "Task one")))
        fs.createDirectories("/v/exports".toPath()); fs.write("/v/exports/Thabit-vault-2026-10-14.zip".toPath()) { writeUtf8("zip") }
        val buffer = Buffer()
        zipVault(fs, "/v".toPath(), buffer)
        val names = ArrayList<String>()
        ZipInputStream(buffer.inputStream()).use { zip -> generateSequence { zip.nextEntry }.forEach { names += it.name } }
        assertTrue("Thabit/tasks/Tasks.md" in names, names.toString())
        assertEquals(0, names.count { it.contains("exports") }, names.toString())
    }
}
