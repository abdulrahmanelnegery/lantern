package dev.thabit.notes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The notes as Compose state, read again after each change. */
class NotesState(private val repo: NotesRepository) {
    var notes by mutableStateOf(repo.notes())
        private set

    fun reload() { notes = repo.notes() }

    /** Every note tag with how many notes carry it, most used first. */
    fun tags(): List<Pair<String, Int>> = notes.flatMap { it.tags }.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }

    fun save(note: Note): Note = repo.save(note).also { notes = repo.notes() }

    fun trash(id: String) { repo.trash(id); notes = repo.notes() }
}
