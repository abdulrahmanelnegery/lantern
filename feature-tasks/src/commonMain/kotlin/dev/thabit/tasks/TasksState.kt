package dev.thabit.tasks

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The tasks as Compose state, written back to the checklist after each change. */
class TasksState(private val repo: TasksRepository) {
    var tasks by mutableStateOf(repo.tasks())
        private set

    fun reload() { tasks = repo.tasks() }

    fun save(list: List<Task>) { repo.save(list); tasks = list }

    /** Ticks a task done (or reopens it); a repeating task leaves its next copy behind. */
    fun complete(id: String, done: Boolean, today: kotlinx.datetime.LocalDate) = save(Recurrence.complete(tasks, id, done, today))

    /** The names of the task lists in use, in order of first use; tasks with no list are the inbox. */
    fun lists(): List<String> = (tasks.map { it.list } + extraLists).filter { it.isNotEmpty() }.distinct()

    /** Lists created in the menu that have no task yet; they last until the app closes unless a task is added. */
    private val extraLists = mutableListOf<String>()
    fun rememberList(name: String) { if (name.isNotBlank() && name !in extraLists) extraLists += name }
}
