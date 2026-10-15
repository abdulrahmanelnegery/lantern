package dev.thabit.core

import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FolderMigrationTest {
    private val fs = FakeFileSystem()
    private val old = "/sd/Thabit".toPath()
    private val new = "/sd/Zimam/Thabit".toPath()

    private fun put(path: String, text: String) { val p = path.toPath(); fs.createDirectories(p.parent!!); fs.write(p) { writeUtf8(text) } }
    private fun read(path: String) = fs.read(path.toPath()) { readUtf8() }

    @Test fun `copies every file, verifies, leaves the old folder and writes a marker`() {
        put("/sd/Thabit/notes/a.md", "A"); put("/sd/Thabit/.lantern/settings.json", "{}"); put("/sd/Thabit/journal/2026/2026-10-14.md", "J")
        val r = FolderMigration.copyAndVerify(fs, old, new)
        assertTrue(r.ok); assertEquals(3, r.copied)
        assertEquals("A", read("/sd/Zimam/Thabit/notes/a.md")); assertEquals("J", read("/sd/Zimam/Thabit/journal/2026/2026-10-14.md"))
        assertTrue(fs.exists("/sd/Thabit/notes/a.md".toPath())); assertTrue(fs.exists(FolderMigration.marker(new, "Thabit")))
        assertTrue(FolderMigration.safeToRemove(fs, old, new))
    }

    @Test fun `a clashing file is never overwritten and the old copy is kept beside it`() {
        put("/sd/Thabit/tasks/Tasks.md", "old"); put("/sd/Zimam/Thabit/tasks/Tasks.md", "new")
        val r = FolderMigration.copyAndVerify(fs, old, new)
        assertTrue(r.ok); assertEquals(1, r.kept)
        assertEquals("new", read("/sd/Zimam/Thabit/tasks/Tasks.md")); assertEquals("old", read("/sd/Zimam/Thabit/tasks/Tasks (from Thabit).md"))
    }

    @Test fun `running twice copies nothing more and keeps no extra files`() {
        put("/sd/Thabit/notes/a.md", "A"); put("/sd/Zimam/Thabit/notes/a.md", "other")
        FolderMigration.copyAndVerify(fs, old, new)
        val again = FolderMigration.copyAndVerify(fs, old, new)
        assertTrue(again.ok); assertEquals(0, again.copied); assertEquals(0, again.kept)
        assertEquals(2, fs.list("/sd/Zimam/Thabit/notes".toPath()).size)
    }

    @Test fun `a partial copy is detected and finished`() {
        put("/sd/Thabit/notes/a.md", "A"); put("/sd/Thabit/notes/b.md", "B"); put("/sd/Zimam/Thabit/notes/a.md", "A")
        assertEquals(listOf("missing or different: notes/b.md"), FolderMigration.verify(fs, old, new))
        assertFalse(FolderMigration.safeToRemove(fs, old, new))
        assertTrue(FolderMigration.copyAndVerify(fs, old, new).ok)
        assertTrue(FolderMigration.safeToRemove(fs, old, new))
    }

    @Test fun `a changed destination file is a verify failure`() {
        put("/sd/Thabit/notes/a.md", "A"); FolderMigration.copyAndVerify(fs, old, new)
        put("/sd/Thabit/notes/a.md", "A changed")
        assertFalse(FolderMigration.safeToRemove(fs, old, new))
    }

    @Test fun `a leftover part file is ignored and the real file is copied`() {
        put("/sd/Thabit/notes/a.md", "A"); put("/sd/Zimam/Thabit/notes/a.md.part", "half")
        assertTrue(FolderMigration.copyAndVerify(fs, old, new).ok)
        assertEquals("A", read("/sd/Zimam/Thabit/notes/a.md"))
    }

    @Test fun `choose keeps the old folder when the shared one cannot be used and uses the private vault only when no visible one exists`() {
        val private = "/data/Thabit".toPath()
        put("/data/Thabit/notes/p.md", "P")
        assertEquals(private, VaultLocation.choose(fs, false, new, listOf(old), private).dir)
        val c = VaultLocation.choose(fs, true, new, listOf(old), private)
        assertEquals(new, c.dir); assertEquals("P", read("/sd/Zimam/Thabit/notes/p.md"))
        put("/sd/Thabit/notes/v.md", "V")
        VaultLocation.choose(fs, true, "/sd/Zimam/Other".toPath(), listOf(old), private)
        assertFalse(fs.exists("/sd/Zimam/Other/notes/p.md".toPath())); assertEquals("V", read("/sd/Zimam/Other/notes/v.md"))
    }

    @Test fun `removal needs the shared folder in use and a fresh verify, and then removes only the old folder`() {
        put("/sd/Thabit/notes/a.md", "A"); VaultLocation.choose(fs, true, new, listOf(old), "/data/Thabit".toPath())
        assertEquals(listOf(old), VaultLocation.removable(fs, new, new, listOf(old)))
        assertEquals(emptyList(), VaultLocation.removable(fs, old, new, listOf(old)))
        put("/sd/Thabit/notes/new.md", "added later")
        assertFalse(VaultLocation.remove(fs, old, new)); assertTrue(fs.exists("/sd/Thabit/notes/a.md".toPath()))
        FolderMigration.copyAndVerify(fs, old, new)
        assertTrue(VaultLocation.remove(fs, old, new)); assertFalse(fs.exists(old)); assertEquals("A", read("/sd/Zimam/Thabit/notes/a.md"))
    }

    @Test fun `two old folders with the same name in different places keep separate markers and both are copied`() {
        val (oldA, oldB) = LegacyNames.oldVaultFolders; val nameB = oldB.substringAfterLast("/"); val a = "/sd/$oldA".toPath(); val b = "/sd/$oldB".toPath(); val target = "/sd/Zimam/Thabit".toPath()
        put("/sd/$oldA/notes/a.md", "A"); put("/sd/$oldB/notes/b.md", "B"); put("/sd/$oldB/notes/a.md", "different A")
        val c = VaultLocation.choose(fs, true, target, listOf(a, b), "/data/Thabit".toPath())
        assertEquals(target, c.dir)
        assertEquals("A", read("/sd/Zimam/Thabit/notes/a.md")); assertEquals("B", read("/sd/Zimam/Thabit/notes/b.md"))
        assertEquals("different A", read("/sd/Zimam/Thabit/notes/a (from $nameB).md"))
        assertTrue(fs.exists(FolderMigration.marker(target, oldA.replace("/", "-")))); assertTrue(fs.exists(FolderMigration.marker(target, nameB)))
        assertEquals(listOf(a, b), VaultLocation.removable(fs, target, target, listOf(a, b)))
    }
}
