package dev.thabit.core

import dev.thabit.habits.Habit
import dev.thabit.habits.appliesOn
import dev.thabit.tasks.Task
import dev.thabit.tasks.Recurrence
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus

/** One reminder to show: what it is for, and when. */
data class Reminder(val kind: Kind, val refId: String, val title: String, val at: LocalDateTime) {
    enum class Kind { Habit, Task }
}

object Reminders {
    private fun time(s: String): LocalTime? = if (Recurrence.validTime(s)) LocalTime(s.take(2).toInt(), s.takeLast(2).toInt()) else null

    /**
     * The reminders after [now] within [days] days, soonest first: a habit at its time on each day it applies (not on a day it is
     * already done or skipped), and an open task at its time on its due day.
     */
    fun upcoming(
        habits: List<Habit>, tasks: List<Task>, log: Map<String, Set<LocalDate>>, skips: Map<String, Set<LocalDate>>, now: LocalDateTime, days: Int = 3,
    ): List<Reminder> {
        val out = ArrayList<Reminder>()
        for (offset in 0 until days) {
            val day = now.date.plus(offset, DateTimeUnit.DAY)
            for (h in habits) {
                val t = time(h.remindAt) ?: continue
                if (h.archived || !h.appliesOn(day) || day in log[h.id].orEmpty() || day in skips[h.id].orEmpty()) continue
                out += Reminder(Reminder.Kind.Habit, h.id, h.name, LocalDateTime(day, t))
            }
            for (t in tasks) {
                if (t.done || t.due != day) continue
                val at = time(t.remindAt) ?: continue
                out += Reminder(Reminder.Kind.Task, t.id, t.title, LocalDateTime(day, at))
            }
        }
        return out.filter { it.at > now }.sortedBy { it.at }
    }

    /** True when anything has a reminder time, so the app can ask for notification permission only when it matters. */
    fun any(habits: List<Habit>, tasks: List<Task>): Boolean = habits.any { !it.archived && time(it.remindAt) != null } || tasks.any { !it.done && time(it.remindAt) != null }
}
