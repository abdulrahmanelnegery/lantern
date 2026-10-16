package dev.thabit.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.thabit.core.AppSettings
import dev.thabit.core.SettingsRepository
import dev.thabit.core.Vault
import dev.thabit.habits.HabitsState
import dev.thabit.tasks.TasksState

/** What the app-level screens read: the vault, the settings and the date, plus each feature's own state. */
class ThabitStore(val vault: Vault, val location: String) {
    val tasksState = TasksState(vault.tasksRepo)
    val habitsState = HabitsState(vault.habitsRepo)

    private val settingsRepo = SettingsRepository(vault)
    var settings by mutableStateOf(settingsRepo.current)
        private set
    var today by mutableStateOf(vault.today())
        private set
    /** True when notes/ or journal/ hold files (they moved to Athar); read from disk on open and on reload, not on every redraw. */
    var hasNotesOrJournalFiles by mutableStateOf(vault.hasNotesOrJournalFiles())
        private set
    /** The hour of the day (0..23), for the greeting. */
    val hour: Int get() = vault.files.stamp().substring(11, 13).toInt()

    /** Reads everything again (the vault may have changed on disk, or the date rolled over). */
    fun reload() {
        tasksState.reload(); habitsState.reload()
        today = vault.today(); settings = settingsRepo.reload(); hasNotesOrJournalFiles = vault.hasNotesOrJournalFiles()
    }

    /**
     * True when the vault already holds anything, so an update from a version without the tour flags is not treated as a fresh
     * install. Files left in notes/ and journal/ count: a user who only had notes must not see the first-run tour again.
     */
    val hasData: Boolean get() = tasksState.tasks.isNotEmpty() || habitsState.habits.isNotEmpty() || hasNotesOrJournalFiles

    /** The one-time card telling people with notes or journal files that those live in Athar now. */
    val showAtharNotice: Boolean get() = !settings.atharNoticeDismissed && hasNotesOrJournalFiles

    /** Set to open the first-install tour again from Settings or About. */
    var tourOpen by mutableStateOf(false)

    fun saveSettings(value: AppSettings) { settings = settingsRepo.update { value } }
}
