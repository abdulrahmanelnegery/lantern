package dev.lantern.core

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * A Lantern vault: one folder of plain files, readable in any editor.
 *
 * ```
 * notes/<title>-<id>.md            Markdown with a small header (id, title, tags, dates)
 * journal/<year>/<date>.md         one file a day: mood in the header, text below
 * tasks/Tasks.md                   a Markdown checklist
 * habits/habits.json, log.csv      what you track, and the days you did it
 * money/accounts.json, <year>.csv  accounts and transactions in minor units
 * .lantern/                        settings and the trash
 * ```
 */
class Vault(
    val fs: FileSystem,
    val root: Path,
    private val clock: Clock = Clock.System,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    init {
        listOf("notes", "journal", "tasks", "habits", "money", ".lantern/trash").forEach { fs.createDirectories(root / it) }
    }

    fun today(): LocalDate = clock.now().toLocalDateTime(zone).date

    private fun stamp(): String = clock.now().toLocalDateTime(zone).toString().take(19)

    private fun read(path: Path): String? = if (fs.exists(path)) fs.read(path) { readUtf8() } else null

    private fun write(path: Path, text: String) {
        path.parent?.let { fs.createDirectories(it) }
        val tmp = path.parent!! / (path.name + ".tmp")
        fs.write(tmp) { writeUtf8(text) }
        fs.atomicMove(tmp, path)
    }

    // ---- settings --------------------------------------------------------------------------

    private val settingsPath get() = root / ".lantern" / "settings.json"

    fun settings(): AppSettings = read(settingsPath)?.let { runCatching { json.decodeFromString(AppSettings.serializer(), it) }.getOrNull() } ?: AppSettings()

    fun saveSettings(settings: AppSettings) = write(settingsPath, json.encodeToString(AppSettings.serializer(), settings))

    // ---- notes -----------------------------------------------------------------------------

    private fun noteFiles(): List<Path> = fs.listOrNull(root / "notes").orEmpty().filter { it.name.endsWith(".md") }

    fun notes(): List<Note> = noteFiles().mapNotNull { parseNote(it) }.sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updated })

    private fun parseNote(path: Path): Note? {
        val doc = Frontmatter.parse(read(path) ?: return null)
        val id = doc.meta["id"] ?: path.name.removeSuffix(".md").substringAfterLast('-')
        val title = doc.meta["title"] ?: doc.body.lineSequence().firstOrNull { it.isNotBlank() }?.trimStart('#', ' ') ?: path.name.removeSuffix(".md")
        return Note(id, title, doc.body, doc.meta["created"].orEmpty(), doc.meta["updated"].orEmpty(), doc.list("tags"), doc.meta["pinned"] == "true")
    }

    /** Creates or updates a note, renaming its file when the title changes. */
    fun saveNote(note: Note): Note {
        val id = note.id.ifEmpty { newId() }
        val created = note.created.ifEmpty { stamp() }
        val saved = note.copy(id = id, created = created, updated = stamp())
        val meta = linkedMapOf("id" to id, "title" to saved.title, "created" to created, "updated" to saved.updated, "tags" to Frontmatter.formatList(saved.tags))
        if (saved.pinned) meta["pinned"] = "true"
        val target = root / "notes" / "${slugOf(saved.title)}-$id.md"
        noteFiles().filter { it.name.endsWith("-$id.md") && it != target }.forEach { fs.delete(it) }
        write(target, Frontmatter.write(Document(meta, saved.body)))
        return saved
    }

    /** Moves a note to the vault's trash instead of deleting it. */
    fun trashNote(id: String) {
        noteFiles().filter { it.name.endsWith("-$id.md") }.forEach { fs.atomicMove(it, root / ".lantern" / "trash" / it.name) }
    }

    // ---- journal ---------------------------------------------------------------------------

    private fun journalPath(date: LocalDate) = root / "journal" / date.year.toString() / "$date.md"

    fun journal(date: LocalDate): JournalEntry? {
        val doc = Frontmatter.parse(read(journalPath(date)) ?: return null)
        return JournalEntry(date, doc.body.trimEnd('\n'), doc.meta["mood"]?.toIntOrNull(), doc.list("tags"))
    }

    /** Saves the day's entry; an empty entry with no mood removes the file. */
    fun saveJournal(entry: JournalEntry) {
        val path = journalPath(entry.date)
        if (entry.body.isBlank() && entry.mood == null && entry.tags.isEmpty()) { fs.delete(path); return }
        val meta = linkedMapOf("date" to entry.date.toString())
        entry.mood?.let { meta["mood"] = it.coerceIn(1, 5).toString() }
        if (entry.tags.isNotEmpty()) meta["tags"] = Frontmatter.formatList(entry.tags)
        write(path, Frontmatter.write(Document(meta, entry.body)))
    }

    /** Every day that has an entry, newest first. */
    fun journalDates(): List<LocalDate> = fs.listOrNull(root / "journal").orEmpty().flatMap { year ->
        fs.listOrNull(year).orEmpty().mapNotNull { runCatching { LocalDate.parse(it.name.removeSuffix(".md")) }.getOrNull() }
    }.sortedDescending()

    fun journalEntries(days: Int): List<JournalEntry> {
        val from = today().minus(days, DateTimeUnit.DAY)
        return journalDates().filter { it >= from }.mapNotNull { journal(it) }
    }

    // ---- tasks -----------------------------------------------------------------------------

    private val tasksPath get() = root / "tasks" / "Tasks.md"

    fun tasks(): List<Task> = (read(tasksPath) ?: "").lineSequence().mapNotNull(::parseTask).toList()

    fun saveTasks(tasks: List<Task>) = write(tasksPath, "# Tasks\n\n" + tasks.joinToString("\n") { formatTask(it) } + "\n")

    private fun formatTask(t: Task): String = buildString {
        append(if (t.done) "- [x] " else "- [ ] ").append(t.title)
        t.due?.let { append(" due:").append(it) }
        if (t.priority > 0) append(" p:").append(t.priority)
        if (t.list.isNotEmpty()) append(" list:").append(t.list.replace(' ', '_'))
        t.doneOn?.let { append(" done:").append(it) }
        append(" id:").append(t.id)
    }

    private fun parseTask(line: String): Task? {
        val m = Regex("^\\s*- \\[( |x|X)] (.*)$").find(line) ?: return null
        val tokens = m.groupValues[2].split(' ').toMutableList()
        var due: LocalDate? = null
        var priority = 0
        var list = ""
        var id = ""
        var doneOn: LocalDate? = null
        while (tokens.isNotEmpty()) {
            val t = tokens.last()
            when {
                t.startsWith("due:") -> due = runCatching { LocalDate.parse(t.removePrefix("due:")) }.getOrNull()
                t.startsWith("p:") -> priority = t.removePrefix("p:").toIntOrNull() ?: 0
                t.startsWith("list:") -> list = t.removePrefix("list:").replace('_', ' ')
                t.startsWith("id:") -> id = t.removePrefix("id:")
                t.startsWith("done:") -> doneOn = runCatching { LocalDate.parse(t.removePrefix("done:")) }.getOrNull()
                else -> break
            }
            tokens.removeAt(tokens.lastIndex)
        }
        val title = tokens.joinToString(" ").trim()
        if (title.isEmpty()) return null
        return Task(id.ifEmpty { newId() }, title, m.groupValues[1] != " ", due, priority, list, doneOn)
    }

    // ---- habits ----------------------------------------------------------------------------

    private val habitsPath get() = root / "habits" / "habits.json"
    private val habitLogPath get() = root / "habits" / "log.csv"

    fun habits(): List<Habit> = read(habitsPath)?.let { runCatching { json.decodeFromString(ListSerializer(Habit.serializer()), it) }.getOrNull() }.orEmpty()

    fun saveHabits(habits: List<Habit>) = write(habitsPath, json.encodeToString(ListSerializer(Habit.serializer()), habits))

    /** The days each habit was done, by habit id. */
    fun habitLog(): Map<String, Set<LocalDate>> =
        Csv.parse(read(habitLogPath) ?: "").drop(1).mapNotNull { r ->
            val date = runCatching { LocalDate.parse(r.getOrElse(1) { "" }) }.getOrNull() ?: return@mapNotNull null
            r[0] to date
        }.groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() }

    fun setHabitDone(habitId: String, date: LocalDate, done: Boolean) {
        val log = habitLog().toMutableMap()
        val days = (log[habitId] ?: emptySet()).let { if (done) it + date else it - date }
        log[habitId] = days
        val rows = listOf(listOf("habit", "date")) + log.flatMap { (h, ds) -> ds.sorted().map { listOf(h, it.toString()) } }
        write(habitLogPath, Csv.format(rows))
    }

    // ---- money -----------------------------------------------------------------------------

    private val accountsPath get() = root / "money" / "accounts.json"

    fun accounts(): List<Account> = read(accountsPath)?.let { runCatching { json.decodeFromString(ListSerializer(Account.serializer()), it) }.getOrNull() }.orEmpty()

    fun saveAccounts(accounts: List<Account>) = write(accountsPath, json.encodeToString(ListSerializer(Account.serializer()), accounts))

    private fun txnPath(year: Int) = root / "money" / "$year.csv"

    private fun txnYears(): List<Int> = fs.listOrNull(root / "money").orEmpty().mapNotNull { it.name.removeSuffix(".csv").toIntOrNull() }

    fun transactions(year: Int): List<Txn> = Csv.parse(read(txnPath(year)) ?: "").drop(1).mapNotNull { r ->
        runCatching { Txn(r[0], LocalDate.parse(r[1]), r[2].toLong(), r[3], r.getOrElse(4) { "" }, r.getOrElse(5) { "" }) }.getOrNull()
    }

    fun allTransactions(): List<Txn> = txnYears().sorted().flatMap { transactions(it) }.sortedByDescending { it.date }

    /** Adds the transaction, or replaces the one with the same id. */
    fun saveTransaction(txn: Txn): Txn {
        val saved = if (txn.id.isEmpty()) txn.copy(id = newId()) else txn
        allTransactions().firstOrNull { it.id == saved.id && it.date.year != saved.date.year }?.let { deleteTransaction(it.id) }
        val year = saved.date.year
        writeYear(year, transactions(year).filter { it.id != saved.id } + saved)
        return saved
    }

    fun deleteTransaction(id: String) {
        txnYears().forEach { y -> transactions(y).let { list -> if (list.any { it.id == id }) writeYear(y, list.filter { it.id != id }) } }
    }

    private fun writeYear(year: Int, list: List<Txn>) {
        val rows = listOf(listOf("id", "date", "amount_minor", "account", "category", "note")) +
            list.sortedBy { it.date }.map { listOf(it.id, it.date.toString(), it.amountMinor.toString(), it.accountId, it.category, it.note) }
        write(txnPath(year), Csv.format(rows))
    }

    // ---- search ----------------------------------------------------------------------------

    data class Hit(val kind: String, val title: String, val snippet: String, val id: String)

    /** Looks through notes, journal entries and tasks for [query] (case-insensitive). */
    fun search(query: String): List<Hit> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        fun snippet(text: String): String {
            val at = text.lowercase().indexOf(q).coerceAtLeast(0)
            return text.substring((at - 30).coerceAtLeast(0), (at + 60).coerceAtMost(text.length)).replace('\n', ' ')
        }
        val hits = ArrayList<Hit>()
        notes().filter { it.title.lowercase().contains(q) || it.body.lowercase().contains(q) || it.tags.any { t -> t.lowercase().contains(q) } }
            .forEach { hits += Hit("note", it.title, snippet(it.body), it.id) }
        journalDates().mapNotNull { journal(it) }.filter { it.body.lowercase().contains(q) }
            .forEach { hits += Hit("journal", it.date.toString(), snippet(it.body), it.date.toString()) }
        tasks().filter { it.title.lowercase().contains(q) }.forEach { hits += Hit("task", it.title, if (it.done) "done" else "open", it.id) }
        return hits
    }
}
