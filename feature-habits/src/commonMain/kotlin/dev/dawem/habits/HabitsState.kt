package dev.dawem.habits

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.LocalDate

/** The habits and their marks as Compose state, read again after each change. */
class HabitsState(private val repo: HabitsRepository) {
    var habits by mutableStateOf(repo.habits())
        private set
    var log by mutableStateOf(repo.log())
        private set
    var skips by mutableStateOf(repo.skips())
        private set

    fun reload() { habits = repo.habits(); log = repo.log(); skips = repo.skips() }

    fun save(list: List<Habit>) { repo.save(list); habits = list }

    fun setDone(id: String, date: LocalDate, done: Boolean) { repo.setDone(id, date, done); log = repo.log(); skips = repo.skips() }

    /** Marks a day done or skipped on purpose; null clears it. */
    fun setMark(id: String, date: LocalDate, mark: HabitMark?) { repo.setMark(id, date, mark); log = repo.log(); skips = repo.skips() }
}
