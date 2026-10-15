package dev.thabit.core

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import dev.thabit.habits.Habit
import dev.thabit.habits.HabitMark
import dev.thabit.habits.HabitsRepository
import dev.thabit.tasks.Task
import dev.thabit.tasks.TasksRepository
import okio.FileSystem
import okio.Path

/**
 * A Thabit vault: one folder of plain files, readable in any editor.
 *
 * ```
 * tasks/Tasks.md                   a Markdown checklist
 * habits/habits.json, log.csv      what you track, and the days you did it
 * .lantern/                        settings and the trash
 * notes/, journal/                  left by older versions; Thabit never reads, writes or deletes them (see docs/vault-format.md)
 * ```
 */
class Vault(
    val fs: FileSystem,
    val root: Path,
    clock: Clock = Clock.System,
    zone: TimeZone = TimeZone.currentSystemDefault(),
) {
    val files = VaultFiles(fs, root, clock, zone)
    val tasksRepo = TasksRepository(files)
    val habitsRepo = HabitsRepository(files)

    init { files.trash.let { fs.createDirectories(it) } }

    fun today(): LocalDate = files.today()

    // ---- settings --------------------------------------------------------------------------

    private val settingsPath get() = root / ".lantern" / "settings.json"

    fun settings(): AppSettings = files.readJson(settingsPath, AppSettings.serializer(), AppSettings())

    fun saveSettings(settings: AppSettings) = files.write(settingsPath, files.json.encodeToString(AppSettings.serializer(), settings))

    // ---- the features, kept under their short names so callers do not change ----------------

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
