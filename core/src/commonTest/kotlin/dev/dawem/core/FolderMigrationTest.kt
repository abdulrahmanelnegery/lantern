package dev.dawem.core

import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FolderMigrationTest {
    private val fs = FakeFileSystem()
    private val old = "/sd/Dawem".toPath()
    private val new = "/sd/Zimam/Dawem".toPath()

    private fun put(path: String, text: String) { val p = path.toPath(); fs.createDirectories(p.parent!!); fs.write(p) { writeUtf8(text) } }
    private fun read(path: String) = fs.read(path.toPath()) { readUtf8() }

    @Test fun `copies every file, verifies, leaves the old folder and writes a marker`() {
        put("/sd/Dawem/notes/a.md", "A"); put("/sd/Dawem/.lantern/settings.json", "{}"); put("/sd/Dawem/journal/2026/2026-10-14.md", "J")
        val r = FolderMigration.copyAndVerify(fs, old, new)
        assertTrue(r.ok); assertEquals(3, r.copied)
        assertEquals("A", read("/sd/Zimam/Dawem/notes/a.md")); assertEquals("J", read("/sd/Zimam/Dawem/journal/2026/2026-10-14.md"))
        assertTrue(fs.exists("/sd/Dawem/notes/a.md".toPath())); assertTrue(fs.exists(FolderMigration.marker(new, "Dawem")))
        assertTrue(FolderMigration.safeToRemove(fs, old, new))
    }

    @Test fun `a clashing file is never overwritten and the old copy is kept beside it`() {
        put("/sd/Dawem/tasks/Tasks.md", "old"); put("/sd/Zimam/Dawem/tasks/Tasks.md", "new")
        val r = FolderMigration.copyAndVerify(fs, old, new)
        assertTrue(r.ok); assertEquals(1, r.kept)
        assertEquals("new", read("/sd/Zimam/Dawem/tasks/Tasks.md")); assertEquals("old", read("/sd/Zimam/Dawem/tasks/Tasks (from Dawem).md"))
    }

    @Test fun `running twice copies nothing more and keeps no extra files`() {
        put("/sd/Dawem/notes/a.md", "A"); put("/sd/Zimam/Dawem/notes/a.md", "other")
        FolderMigration.copyAndVerify(fs, old, new)
        val again = FolderMigration.copyAndVerify(fs, old, new)
        assertTrue(again.ok); assertEquals(0, again.copied); assertEquals(0, again.kept)
        assertEquals(2, fs.list("/sd/Zimam/Dawem/notes".toPath()).size)
    }

    @Test fun `a partial copy is detected and finished`() {
        put("/sd/Dawem/notes/a.md", "A"); put("/sd/Dawem/notes/b.md", "B"); put("/sd/Zimam/Dawem/notes/a.md", "A")
        assertEquals(listOf("missing or different: notes/b.md"), FolderMigration.verify(fs, old, new))
        assertFalse(FolderMigration.safeToRemove(fs, old, new))
        assertTrue(FolderMigration.copyAndVerify(fs, old, new).ok)
        assertTrue(FolderMigration.safeToRemove(fs, old, new))
    }

    @Test fun `a changed destination file is a verify failure`() {
        put("/sd/Dawem/notes/a.md", "A"); FolderMigration.copyAndVerify(fs, old, new)
        put("/sd/Dawem/notes/a.md", "A changed")
        assertFalse(FolderMigration.safeToRemove(fs, old, new))
    }

    @Test fun `a leftover part file is ignored and the real file is copied`() {
        put("/sd/Dawem/notes/a.md", "A"); put("/sd/Zimam/Dawem/notes/a.md.part", "half")
        assertTrue(FolderMigration.copyAndVerify(fs, old, new).ok)
        assertEquals("A", read("/sd/Zimam/Dawem/notes/a.md"))
    }

    @Test fun `choose keeps the old folder when the shared one cannot be used and uses the private vault only when no visible one exists`() {
        val private = "/data/Dawem".toPath()
        put("/data/Dawem/notes/p.md", "P")
        assertEquals(private, VaultLocation.choose(fs, false, new, listOf(old), private).dir)
        val c = VaultLocation.choose(fs, true, new, listOf(old), private)
        assertEquals(new, c.dir); assertEquals("P", read("/sd/Zimam/Dawem/notes/p.md"))
        put("/sd/Dawem/notes/v.md", "V")
        VaultLocation.choose(fs, true, "/sd/Zimam/Other".toPath(), listOf(old), private)
        assertFalse(fs.exists("/sd/Zimam/Other/notes/p.md".toPath())); assertEquals("V", read("/sd/Zimam/Other/notes/v.md"))
    }

    @Test fun `removal needs the shared folder in use and a fresh verify, and then removes only the old folder`() {
        put("/sd/Dawem/notes/a.md", "A"); VaultLocation.choose(fs, true, new, listOf(old), "/data/Dawem".toPath())
        assertEquals(listOf(old), VaultLocation.removable(fs, new, new, listOf(old)))
        assertEquals(emptyList(), VaultLocation.removable(fs, old, new, listOf(old)))
        put("/sd/Dawem/notes/new.md", "added later")
        assertFalse(VaultLocation.remove(fs, old, new)); assertTrue(fs.exists("/sd/Dawem/notes/a.md".toPath()))
        FolderMigration.copyAndVerify(fs, old, new)
        assertTrue(VaultLocation.remove(fs, old, new)); assertFalse(fs.exists(old)); assertEquals("A", read("/sd/Zimam/Dawem/notes/a.md"))
    }
}
