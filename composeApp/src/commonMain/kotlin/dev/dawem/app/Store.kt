package dev.dawem.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.dawem.core.AppSettings
import dev.dawem.core.Vault
import dev.dawem.habits.HabitsState
import dev.dawem.journal.JournalState
import dev.dawem.notes.NotesState
import dev.dawem.tasks.TasksState

/** What the app-level screens read: the vault, the settings and the date, plus each feature's own state. */
class DawemStore(val vault: Vault, val location: String) {
    val notesState = NotesState(vault.notesRepo)
    val tasksState = TasksState(vault.tasksRepo)
    val journalState = JournalState(vault.journalRepo)
    val habitsState = HabitsState(vault.habitsRepo)

    var settings by mutableStateOf(vault.settings())
        private set
    var today by mutableStateOf(vault.today())
        private set

    /** Reads everything again (the vault may have changed on disk, or the date rolled over). */
    fun reload() {
        notesState.reload(); tasksState.reload(); journalState.reload(); habitsState.reload()
        today = vault.today(); settings = vault.settings()
    }

    /** True when the vault already holds anything, so an update from a version without the tour flags is not treated as a fresh install. */
    val hasData: Boolean get() = notesState.notes.isNotEmpty() || tasksState.tasks.isNotEmpty() || habitsState.habits.isNotEmpty() || journalState.dates().isNotEmpty()

    /** Set to open the first-install tour again from Settings or About. */
    var tourOpen by mutableStateOf(false)

    fun saveSettings(value: AppSettings) { vault.saveSettings(value); settings = value }
}
