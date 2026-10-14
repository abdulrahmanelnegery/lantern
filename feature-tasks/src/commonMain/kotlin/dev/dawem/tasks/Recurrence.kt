package dev.dawem.tasks

import dev.dawem.core.newId
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

object Recurrence {
    val kinds = listOf("daily", "weekly", "monthly")

    /** "HH:MM" with a real hour and minute. */
    fun validTime(s: String): Boolean = Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(s)

    /** The next due date after finishing: one step past the old due date (or today), moved on until it is after [today]. */
    fun nextDue(due: LocalDate?, repeat: String, today: LocalDate): LocalDate? {
        val unit = when (repeat) { "daily" -> DateTimeUnit.DAY; "weekly" -> DateTimeUnit.WEEK; "monthly" -> DateTimeUnit.MONTH; else -> return null }
        var next = (due ?: today).plus(1, unit)
        var guard = 0
        while (next <= today && guard++ < 1000) next = next.plus(1, unit)
        return next
    }

    /** The tasks after ticking [id] done on [today]: the task is done, and a repeating one also leaves its next, open copy. Unticking just reopens it. */
    fun complete(tasks: List<Task>, id: String, done: Boolean, today: LocalDate): List<Task> {
        val t = tasks.firstOrNull { it.id == id } ?: return tasks
        val updated = tasks.map { if (it.id == id) it.copy(done = done, doneOn = if (done) today else null) else it }
        if (!done || t.done || t.repeat.isEmpty()) return updated
        val next = t.copy(id = newId(), done = false, doneOn = null, due = nextDue(t.due, t.repeat, today), subtasks = t.subtasks.map { it.copy(done = false) })
        val at = updated.indexOfFirst { it.id == id }
        return updated.toMutableList().also { it.add(at, next) }
    }
}
