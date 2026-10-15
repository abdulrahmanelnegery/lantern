package dev.thabit.core

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import dev.thabit.habits.Habit
import dev.thabit.habits.HabitMark
import dev.thabit.habits.HabitsRepository
import dev.thabit.journal.JournalEntry
import dev.thabit.journal.JournalRepository
import dev.thabit.notes.Note
import dev.thabit.notes.NotesRepository
import dev.thabit.tasks.Task
import dev.thabit.tasks.TasksRepository
import okio.FileSystem
import okio.Path

/**
 * A Thabit vault: one folder of plain files, readable in any editor.
 *
 * ```
 * notes/<title>-<id>.md            Markdown with a small header (id, title, tags, dates)
 * journal/<year>/<date>.md         one file a day: mood in the header, text below
 * tasks/Tasks.md                   a Markdown checklist
 * habits/habits.json, log.csv      what you track, and the days you did it
 * .lantern/                        settings and the trash
 * ```
 */
class Vault(
    val fs: FileSystem,
    val root: Path,
    clock: Clock = Clock.System,
    zone: TimeZone = TimeZone.currentSystemDefault(),
) {
    val files = VaultFiles(fs, root, clock, zone)
    val notesRepo = NotesRepository(files)
    val journalRepo = JournalRepository(files)
    val tasksRepo = TasksRepository(files)
    val habitsRepo = HabitsRepository(files)

    init { files.trash.let { fs.createDirectories(it) } }

    fun today(): LocalDate = files.today()

    // ---- settings --------------------------------------------------------------------------

    private val settingsPath get() = root / ".lantern" / "settings.json"

    fun settings(): AppSettings = files.read(settingsPath)?.let { runCatching { files.json.decodeFromString(AppSettings.serializer(), it) }.getOrNull() } ?: AppSettings()

    fun saveSettings(settings: AppSettings) = files.write(settingsPath, files.json.encodeToString(AppSettings.serializer(), settings))

    // ---- the features, kept under their old names so callers do not change ------------------

    fun notes(): List<Note> = notesRepo.notes()
    fun saveNote(note: Note): Note = notesRepo.save(note)
    fun trashNote(id: String) = notesRepo.trash(id)

    fun journal(date: LocalDate): JournalEntry? = journalRepo.entry(date)
    fun saveJournal(entry: JournalEntry) = journalRepo.save(entry)
    fun journalDates(): List<LocalDate> = journalRepo.dates()
    fun journalEntries(days: Int): List<JournalEntry> = journalRepo.entries(days)

    fun tasks(): List<Task> = tasksRepo.tasks()
    fun saveTasks(tasks: List<Task>) = tasksRepo.save(tasks)

    fun habits(): List<Habit> = habitsRepo.habits()
    fun saveHabits(habits: List<Habit>) = habitsRepo.save(habits)
    fun habitLog(): Map<String, Set<LocalDate>> = habitsRepo.log()
    fun habitSkips(): Map<String, Set<LocalDate>> = habitsRepo.skips()
    fun setHabitMark(habitId: String, date: LocalDate, mark: HabitMark?) = habitsRepo.setMark(habitId, date, mark)
    fun setHabitDone(habitId: String, date: LocalDate, done: Boolean) = habitsRepo.setDone(habitId, date, done)

    /** True when `notes/` or `journal/` hold files. They moved to Athar; Thabit never reads, rewrites or deletes them. */
    fun hasNotesOrJournalFiles(): Boolean = listOf("notes", "journal").any { name ->
        val dir = files.dir(name)
        fs.exists(dir) && fs.listRecursively(dir).any { fs.metadataOrNull(it)?.isRegularFile == true }
    }

    // ---- search ----------------------------------------------------------------------------

    data class Hit(val kind: String, val title: String, val snippet: String, val id: String)

    /** Looks through tasks and habits for [query] (case-insensitive). */
    fun search(query: String): List<Hit> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val hits = ArrayList<Hit>()
        tasks().filter { it.title.lowercase().contains(q) }.forEach { hits += Hit("task", it.title, if (it.done) "done" else "open", it.id) }
        habits().filter { it.name.lowercase().contains(q) }.forEach { hits += Hit("habit", it.name, if (it.archived) "archived" else "habit", it.id) }
        return hits
    }
}
