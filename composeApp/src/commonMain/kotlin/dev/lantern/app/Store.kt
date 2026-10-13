package dev.lantern.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.lantern.core.Account
import dev.lantern.core.Habit
import dev.lantern.core.JournalEntry
import dev.lantern.core.Note
import dev.lantern.core.Task
import dev.lantern.core.Txn
import dev.lantern.core.Vault
import kotlinx.datetime.LocalDate

/** What the screens read and change: the vault's contents as Compose state, rewritten after each change. */
class LanternStore(val vault: Vault, val location: String) {
    var notes by mutableStateOf(vault.notes())
        private set
    var tasks by mutableStateOf(vault.tasks())
        private set
    var habits by mutableStateOf(vault.habits())
        private set
    var habitLog by mutableStateOf(vault.habitLog())
        private set
    var accounts by mutableStateOf(vault.accounts())
        private set
    var txns by mutableStateOf(vault.allTransactions())
        private set
    var settings by mutableStateOf(vault.settings())
        private set
    var today by mutableStateOf(vault.today())
        private set
    var todayEntry by mutableStateOf(vault.journal(vault.today()))
        private set

    /** Reads everything again (the vault may have changed on disk, or the date rolled over). */
    fun reload() {
        notes = vault.notes(); tasks = vault.tasks(); habits = vault.habits(); habitLog = vault.habitLog()
        accounts = vault.accounts(); txns = vault.allTransactions(); today = vault.today(); todayEntry = vault.journal(today); settings = vault.settings()
    }

    /** The names of the task lists in use, in order of first use; tasks with no list are the inbox. */
    fun taskLists(): List<String> = (tasks.map { it.list } + extraLists).filter { it.isNotEmpty() }.distinct()

    /** Lists created in the menu that have no task yet; they last until the app closes unless a task is added. */
    private val extraLists = mutableListOf<String>()
    fun rememberTaskList(name: String) { if (name.isNotBlank() && name !in extraLists) extraLists += name }

    /** Every note tag with how many notes carry it, most used first. */
    fun noteTags(): List<Pair<String, Int>> = notes.flatMap { it.tags }.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }

    fun saveSettings(value: dev.lantern.core.AppSettings) { vault.saveSettings(value); settings = value }

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
    fun setHabitDone(id: String, date: LocalDate, done: Boolean) { vault.setHabitDone(id, date, done); habitLog = vault.habitLog() }

    // money
    fun saveAccounts(list: List<Account>) { vault.saveAccounts(list); accounts = list }
    fun saveTxn(txn: Txn) { vault.saveTransaction(txn); txns = vault.allTransactions() }
    fun deleteTxn(id: String) { vault.deleteTransaction(id); txns = vault.allTransactions() }
}
