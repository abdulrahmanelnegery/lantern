package dev.thabit.notes

import dev.thabit.core.VaultFiles
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import okio.ForwardingFileSystem
import okio.Path
import okio.Sink
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NotesRepositoryTest {
    private class Fixed(var at: String = "2026-03-01T10:00:00Z") : Clock {
        override fun now(): Instant = Instant.parse(at)
    }

    private val fs = FakeFileSystem()
    private val clock = Fixed()
    private val repo = NotesRepository(VaultFiles(fs, "/v".toPath(), clock, TimeZone.UTC))

    @Test fun aSavedNoteComesBackWithItsTagsAndBody() {
        val saved = repo.save(Note("", "Plan", "line one\n\nline two", "", "", listOf("work", "ideas")))
        val read = repo.notes().single()
        assertEquals(saved.id, read.id)
        assertEquals(listOf("work", "ideas"), read.tags)
        assertEquals("line one\n\nline two", read.body.trim())
    }

    @Test fun renamingKeepsOneFileAndTheSameId() {
        val first = repo.save(Note("", "Old name", "x", "", ""))
        repo.save(first.copy(title = "New name"))
        val files = fs.list("/v/notes".toPath()).map { it.name }
        assertEquals(1, files.size, files.toString())
        assertTrue(files.single().startsWith("new-name-"), files.toString())
        assertEquals(first.id, repo.notes().single().id)
    }

    @Test fun aFailedWriteDuringRenameLeavesTheOldNoteInPlace() {
        var failWrites = false
        val flaky = object : ForwardingFileSystem(fs) {
            override fun sink(file: Path, mustCreate: Boolean): Sink {
                if (failWrites) throw java.io.IOException("disk full")
                return super.sink(file, mustCreate)
            }
        }
        val flakyRepo = NotesRepository(VaultFiles(flaky, "/v".toPath(), clock, TimeZone.UTC))
        val first = flakyRepo.save(Note("", "Old name", "precious", "", ""))
        failWrites = true
        assertFailsWith<java.io.IOException> { flakyRepo.save(first.copy(title = "New name")) }
        failWrites = false
        assertEquals("precious", flakyRepo.notes().single().body.trim())
    }

    @Test fun trashMovesTheFileInsteadOfDeletingIt() {
        val n = repo.save(Note("", "Bin me", "x", "", ""))
        repo.trash(n.id)
        assertEquals(emptyList(), repo.notes())
        assertEquals(1, fs.list("/v/.lantern/trash".toPath()).size)
    }

    @Test fun pinnedFirstThenNewestUpdated() {
        repo.save(Note("", "Old", "x", "", ""))
        clock.at = "2026-03-02T10:00:00Z"
        repo.save(Note("", "New", "x", "", ""))
        clock.at = "2026-03-03T10:00:00Z"
        repo.save(Note("", "Pinned but old", "x", "", "", pinned = true))
        assertEquals(listOf("Pinned but old", "New", "Old"), repo.notes().map { it.title })
    }

    @Test fun aFileWithoutAHeaderStillLoadsWithTitleFromItsFirstLine() {
        fs.createDirectories("/v/notes".toPath())
        fs.write("/v/notes/hello-abc12345.md".toPath()) { writeUtf8("# Hello there\n\nbody") }
        val n = repo.notes().single()
        assertEquals("Hello there", n.title)
        assertEquals("abc12345", n.id)
    }
}
