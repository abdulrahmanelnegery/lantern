package dev.dawem.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.dawem.core.Habit
import dev.dawem.core.HabitMark
import dev.dawem.core.JournalEntry
import dev.dawem.core.Note
import dev.dawem.core.Task
import dev.dawem.core.Vault
import kotlinx.datetime.LocalDate

/** What the screens read and change: the vault's contents as Compose state, rewritten after each change. */
class DawemStore(val vault: Vault, val location: String) {
    var notes by mutableStateOf(vault.notes())
        private set
    var tasks by mutableStateOf(vault.tasks())
        private set
    var habits by mutableStateOf(vault.habits())
        private set
    var habitLog by mutableStateOf(vault.habitLog())
        private set
    var habitSkips by mutableStateOf(vault.habitSkips())
        private set
    var settings by mutableStateOf(vault.settings())
        private set
    var today by mutableStateOf(vault.today())
        private set
    var todayEntry by mutableStateOf(vault.journal(vault.today()))
        private set

    /** Reads everything again (the vault may have changed on disk, or the date rolled over). */
    fun reload() {
        notes = vault.notes(); tasks = vault.tasks(); habits = vault.habits(); habitLog = vault.habitLog(); habitSkips = vault.habitSkips()
        today = vault.today(); todayEntry = vault.journal(today); settings = vault.settings()
    }

    /** The names of the task lists in use, in order of first use; tasks with no list are the inbox. */
    fun taskLists(): List<String> = (tasks.map { it.list } + extraLists).filter { it.isNotEmpty() }.distinct()

    /** Lists created in the menu that have no task yet; they last until the app closes unless a task is added. */
    private val extraLists = mutableListOf<String>()
    fun rememberTaskList(name: String) { if (name.isNotBlank() && name !in extraLists) extraLists += name }

    /** Every note tag with how many notes carry it, most used first. */
    fun noteTags(): List<Pair<String, Int>> = notes.flatMap { it.tags }.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }

    fun saveSettings(value: dev.dawem.core.AppSettings) { vault.saveSettings(value); settings = value }

    // notes
    fun saveNote(note: Note): Note = vault.saveNote(note).also { notes = vault.notes() }
    fun trashNote(id: String) { vault.trashNote(id); notes = vault.notes() }

    // journal
    fun journal(date: LocalDate): JournalEntry? = vault.journal(date)
    fun saveJournal(entry: JournalEntry) {
        vault.saveJournal(entry)
        if (entry.date == today) todayEntry = vault.journal(today)
    }

    // tasks
    fun saveTasks(list: List<Task>) { vault.saveTasks(list); tasks = list }

    // habits
    fun saveHabits(list: List<Habit>) { vault.saveHabits(list); habits = list }
    fun setHabitDone(id: String, date: LocalDate, done: Boolean) { vault.setHabitDone(id, date, done); habitLog = vault.habitLog(); habitSkips = vault.habitSkips() }
    /** Marks a day done or skipped on purpose; null clears it. */
    fun setHabitMark(id: String, date: LocalDate, mark: HabitMark?) { vault.setHabitMark(id, date, mark); habitLog = vault.habitLog(); habitSkips = vault.habitSkips() }

}
