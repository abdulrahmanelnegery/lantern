package dev.thabit.notes

import dev.thabit.core.Document
import dev.thabit.core.Frontmatter
import dev.thabit.core.VaultFiles
import dev.thabit.core.newId
import dev.thabit.core.slugOf
import okio.Path

/** Notes as Markdown files in `notes/`, each with a small header. */
class NotesRepository(private val files: VaultFiles) {
    private val fs get() = files.fs
    private val dir get() = files.dir("notes")

    init { fs.createDirectories(dir); fs.createDirectories(files.trash) }

    private fun noteFiles(): List<Path> = fs.listOrNull(dir).orEmpty().filter { it.name.endsWith(".md") }

    fun notes(): List<Note> = noteFiles().mapNotNull { parseNote(it) }.sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updated })

    private fun parseNote(path: Path): Note? {
        val doc = Frontmatter.parse(files.read(path) ?: return null)
        val id = doc.meta["id"] ?: path.name.removeSuffix(".md").substringAfterLast('-')
        val title = doc.meta["title"] ?: doc.body.lineSequence().firstOrNull { it.isNotBlank() }?.trimStart('#', ' ') ?: path.name.removeSuffix(".md")
        return Note(id, title, doc.body, doc.meta["created"].orEmpty(), doc.meta["updated"].orEmpty(), doc.list("tags"), doc.meta["pinned"] == "true")
    }

    /** Creates or updates a note, renaming its file when the title changes. */
    fun save(note: Note): Note {
        val id = note.id.ifEmpty { newId() }
        val created = note.created.ifEmpty { files.stamp() }
        val saved = note.copy(id = id, created = created, updated = files.stamp())
        val meta = linkedMapOf("id" to id, "title" to saved.title, "created" to created, "updated" to saved.updated, "tags" to Frontmatter.formatList(saved.tags))
        if (saved.pinned) meta["pinned"] = "true"
        val target = dir / "${slugOf(saved.title)}-$id.md"
        noteFiles().filter { it.name.endsWith("-$id.md") && it != target }.forEach { fs.delete(it) }
        files.write(target, Frontmatter.write(Document(meta, saved.body)))
        return saved
    }

    /** Moves a note to the vault's trash instead of deleting it. */
    fun trash(id: String) {
        noteFiles().filter { it.name.endsWith("-$id.md") }.forEach { fs.atomicMove(it, files.trash / it.name) }
    }
}
