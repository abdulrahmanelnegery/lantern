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
    var today by mutableStateOf(vault.today())
        private set
    var todayEntry by mutableStateOf(vault.journal(vault.today()))
        private set

    /** Reads everything again (the vault may have changed on disk, or the date rolled over). */
    fun reload() {
        notes = vault.notes(); tasks = vault.tasks(); habits = vault.habits(); habitLog = vault.habitLog()
        accounts = vault.accounts(); txns = vault.allTransactions(); today = vault.today(); todayEntry = vault.journal(today)
    }

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
