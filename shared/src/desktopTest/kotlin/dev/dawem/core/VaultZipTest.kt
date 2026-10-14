package dev.dawem.core

import okio.Buffer
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import java.util.zip.ZipInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import dev.dawem.notes.Note
import dev.dawem.tasks.Task

class VaultZipTest {
    @Test fun exportHasTheVaultFilesButNotTheTrash() {
        val fs = FakeFileSystem()
        val vault = Vault(fs, "/v".toPath())
        vault.saveNote(Note("", "Keep me", "body", "", ""))
        vault.saveTasks(listOf(Task("t1", "Task one")))
        val trashed = vault.saveNote(Note("", "Bin me", "body", "", ""))
        vault.trashNote(trashed.id)

        val buffer = Buffer()
        zipVault(fs, "/v".toPath(), buffer)
        val names = ArrayList<String>()
        ZipInputStream(buffer.inputStream()).use { zip -> generateSequence { zip.nextEntry }.forEach { names += it.name } }

        assertTrue(names.any { it.startsWith("Dawem/notes/keep-me-") }, names.toString())
        assertTrue("Dawem/tasks/Tasks.md" in names, names.toString())
        assertEquals(0, names.count { it.contains("bin-me") }, "the trash stays out of an export")
    }
}
