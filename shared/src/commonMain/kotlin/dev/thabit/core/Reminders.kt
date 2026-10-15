package dev.thabit.core

import dev.thabit.habits.Habit
import dev.thabit.habits.appliesOn
import dev.thabit.tasks.Task
import dev.thabit.tasks.Recurrence
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** One reminder to show: what it is for, and when. */
data class Reminder(val kind: Kind, val refId: String, val title: String, val at: LocalDateTime) {
    enum class Kind { Habit, Task }
}

object Reminders {
    private fun time(s: String): LocalTime? = if (Recurrence.validTime(s)) LocalTime(s.take(2).toInt(), s.takeLast(2).toInt()) else null

    /** Every reminder that falls on [day]: a habit at its time unless already done or skipped that day, an open task at its time on its due day. */
    private fun onDay(habits: List<Habit>, tasks: List<Task>, log: Map<String, Set<LocalDate>>, skips: Map<String, Set<LocalDate>>, day: LocalDate): List<Reminder> {
        val out = ArrayList<Reminder>()
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
        return out
    }

    /**
     * The reminders after [now] within [days] days, soonest first: a habit at its time on each day it applies (not on a day it is
     * already done or skipped), and an open task at its time on its due day.
     */
    fun upcoming(
        habits: List<Habit>, tasks: List<Task>, log: Map<String, Set<LocalDate>>, skips: Map<String, Set<LocalDate>>, now: LocalDateTime, days: Int = 3,
    ): List<Reminder> =
        (0 until days).flatMap { onDay(habits, tasks, log, skips, now.date.plus(it, DateTimeUnit.DAY)) }.filter { it.at > now }.sortedBy { it.at }

    /** The reminders that fell due after [from] up to and including [to] and are still open, oldest first. */
    fun due(
        habits: List<Habit>, tasks: List<Task>, log: Map<String, Set<LocalDate>>, skips: Map<String, Set<LocalDate>>, from: LocalDateTime, to: LocalDateTime,
    ): List<Reminder> {
        val days = from.date.daysUntil(to.date)
        return (0..days).flatMap { onDay(habits, tasks, log, skips, from.date.plus(it, DateTimeUnit.DAY)) }.filter { it.at > from && it.at <= to }.sortedBy { it.at }
    }

    /** What one scheduling pass has to do: show these now, remember [snooze], and wake up again at [next]. */
    data class Plan(val showNow: List<Reminder>, val snooze: Reminder?, val next: Reminder?)

    /**
     * One scheduling pass, run whenever the app or the system asks for the alarm to be set (app open or close, boot, update, time
     * change, an alarm firing). It replays every reminder that came due since the previous pass ([lastPass], at most [maxReplayHours]
     * back) and is still open, so two at the same minute, a Doze delay or a phone switched off lose nothing and nothing shows twice.
     * A [snooze] that has come due is shown if its habit or task is still open; one still ahead competes with the regular reminders
     * for the single alarm.
     */
    fun plan(
        habits: List<Habit>, tasks: List<Task>, log: Map<String, Set<LocalDate>>, skips: Map<String, Set<LocalDate>>,
        now: LocalDateTime, lastPass: LocalDateTime?, snooze: Reminder?, maxReplayHours: Int = 12,
    ): Plan {
        val show = ArrayList<Reminder>()
        if (lastPass != null && lastPass < now) {
            val earliest = now.toInstant(TimeZone.UTC).minus(maxReplayHours, DateTimeUnit.HOUR).toLocalDateTime(TimeZone.UTC)
            show += due(habits, tasks, log, skips, maxOf(lastPass, earliest), now)
        }
        var pending = snooze
        if (pending != null && pending.at <= now) {
            val stillOpen = onDay(habits, tasks, log, skips, pending.at.date).any { it.kind == pending!!.kind && it.refId == pending!!.refId }
            if (stillOpen && show.none { it.kind == pending!!.kind && it.refId == pending!!.refId }) show += pending
            pending = null
        }
        val next = (listOfNotNull(pending) + upcoming(habits, tasks, log, skips, now).take(1)).minByOrNull { it.at }
        return Plan(show, pending, next)
    }

    /** True when anything has a reminder time, so the app can ask for notification permission only when it matters. */
    fun any(habits: List<Habit>, tasks: List<Task>): Boolean = habits.any { !it.archived && time(it.remindAt) != null } || tasks.any { !it.done && time(it.remindAt) != null }
}
