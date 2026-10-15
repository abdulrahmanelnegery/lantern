package dev.thabit.core

import dev.thabit.tasks.Task
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import java.util.zip.ZipInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Notes and journal moved to Athar, but their files stay with the user: a vault that holds them must still open,
 * be saved around them, and export them byte for byte.
 */
class LegacyFilesTest {
    private val fs = FakeFileSystem()
    private val root = "/v".toPath()

    // Awkward on purpose: CRLF, no final newline, Arabic, a multi-line header value and a hidden trash file.
    private val legacy = mapOf(
        "notes/plan-aaaa1111.md" to "---\nid: aaaa1111\ntitle: Plan\ntags: [work]\n---\nLine one\r\nLine two without a newline",
        "notes/ملاحظة-bbbb2222.md" to "---\nid: bbbb2222\ntitle: ملاحظة\naliases:\n  - one\n  - two\n---\nنص عربي\n",
        "journal/2026/2026-10-14.md" to "---\ndate: 2026-10-14\nmood: 4\n---\nA good day.\n",
        "journal/2025/2025-02-29.md" to "not even a header\n\n\n",
    )

    private fun seed() {
        legacy.forEach { (rel, text) -> (root / rel).let { fs.createDirectories(it.parent!!); fs.write(it) { writeUtf8(text) } } }
        fs.createDirectories(root / ".lantern" / "trash"); fs.write(root / ".lantern" / "trash" / "old-cccc3333.md") { writeUtf8("trashed") }
    }

    private fun onDisk() = legacy.keys.associateWith { fs.read(root / it) { readByteString() } }

    private fun zipEntries(): Map<String, okio.ByteString> {
        val buffer = Buffer(); zipVault(fs, root, buffer)
        val out = LinkedHashMap<String, okio.ByteString>()
        ZipInputStream(buffer.inputStream()).use { zip -> generateSequence { zip.nextEntry }.forEach { out[it.name] = zip.readBytes().let { b -> okio.Buffer().write(b).readByteString() } } }
        return out
    }

    @Test fun aVaultWithNotesAndJournalOpensAndKeepsWorking() {
        seed()
        val vault = Vault(fs, root)
        assertTrue(vault.hasNotesOrJournalFiles())
        vault.saveTasks(listOf(Task("t1", "Still works")))
        vault.saveSettings(AppSettings(mode = "dark"))
        assertEquals("Still works", vault.tasks().single().title)
        assertEquals("dark", vault.settings().mode)
    }

    @Test fun savingAroundThemLeavesTheFilesByteIdenticalAndAddsNone() {
        seed()
        val before = onDisk()
        val vault = Vault(fs, root)
        vault.saveTasks(listOf(Task("t1", "x"))); vault.saveSettings(AppSettings()); vault.search("plan"); vault.setHabitDone("h", vault.today(), true)
        assertEquals(before, onDisk())
        assertEquals(setOf("notes/plan-aaaa1111.md", "notes/ملاحظة-bbbb2222.md"), fs.list(root / "notes").map { "notes/" + it.name }.toSet(), "no stray files appear in notes/")
        assertEquals(setOf("2026", "2025"), fs.list(root / "journal").map { it.name }.toSet())
    }

    @Test fun theExportCarriesThemByteForByteAndLeavesTheTrashOut() {
        seed()
        val vault = Vault(fs, root)
        vault.saveTasks(listOf(Task("t1", "x")))
        val zip = zipEntries()
        legacy.forEach { (rel, text) -> assertEquals(text.encodeUtf8(), zip["Thabit/$rel"], "$rel must be in the export unchanged") }
        assertTrue(zip.keys.none { it.contains("trash") })
    }

    @Test fun aVaultWithoutThemHasNoLegacyFiles() {
        assertTrue(!Vault(fs, root).hasNotesOrJournalFiles())
    }
}
