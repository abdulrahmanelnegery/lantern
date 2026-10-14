package dev.thabit.core

import dev.thabit.habits.Habit
import dev.thabit.habits.appliesOn
import dev.thabit.tasks.Task
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** How much of a day's list is done: [done] of [total]. */
data class Progress(val done: Int, val total: Int) {
    val fraction: Float? get() = if (total == 0) null else (done.toFloat() / total).coerceIn(0f, 1f)
}

private fun dueHabits(habits: List<Habit>, skips: Map<String, Set<LocalDate>>, day: LocalDate) =
    habits.filter { !it.archived && it.appliesOn(day) && day !in skips[it.id].orEmpty() }

/** Habits due on [day] (skipped days do not count) plus the tasks that are due by then or were finished that day. */
fun dayProgress(habits: List<Habit>, log: Map<String, Set<LocalDate>>, skips: Map<String, Set<LocalDate>>, tasks: List<Task>, day: LocalDate): Progress {
    val due = dueHabits(habits, skips, day)
    val relevant = tasks.filter { (!it.done && it.due != null && it.due!! <= day) || (it.done && it.doneOn == day) }
    return Progress(
        due.count { day in log[it.id].orEmpty() } + relevant.count { it.done },
        due.size + relevant.size,
    )
}

/** Habit completion per day for the week (Monday to Sunday) that contains [today]; days with no habit due have a null fraction. */
fun weekStrip(habits: List<Habit>, log: Map<String, Set<LocalDate>>, skips: Map<String, Set<LocalDate>>, today: LocalDate): List<Pair<LocalDate, Progress>> {
    val monday = today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY)
    return (0..6).map { i ->
        val d = monday.plus(i, DateTimeUnit.DAY)
        val due = dueHabits(habits, skips, d)
        d to Progress(due.count { d in log[it.id].orEmpty() }, due.size)
    }
}

/** The last [days] moods ending [today], oldest first; a day without a mood is null. */
fun moodSeries(mood: (LocalDate) -> Int?, today: LocalDate, days: Int = 14): List<Int?> =
    (days - 1 downTo 0).map { mood(today.minus(it, DateTimeUnit.DAY)) }

/** Earlier years' entries for today's month and day, newest year first. */
fun onThisDay(entryDates: Collection<LocalDate>, today: LocalDate): List<LocalDate> =
    entryDates.filter { it.monthNumber == today.monthNumber && it.dayOfMonth == today.dayOfMonth && it.year < today.year }.sortedDescending()

/** Overdue tasks first (oldest first), then the ones due soonest, then undated ones by priority; done tasks never. */
fun upcomingTasks(tasks: List<Task>, today: LocalDate, limit: Int = 6): List<Task> =
    tasks.filter { !it.done }
        .sortedWith(compareBy<Task>({ it.due == null }, { it.due }, { -it.priority }))
        .filter { it.due != null || it.priority > 0 }
        .take(limit)

/** The days of the week that contains [today], from Monday. */
fun weekOf(today: LocalDate): Pair<LocalDate, LocalDate> {
    val monday = today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY)
    return monday to monday.plus(6, DateTimeUnit.DAY)
}

/** "Good morning", "Good afternoon" or "Good evening" for an hour 0..23. */
fun greetingFor(hour: Int): String = when (hour) { in 5..11 -> "Good morning"; in 12..16 -> "Good afternoon"; else -> "Good evening" }

val DayOfWeek.shortLabel: String get() = name.take(3).lowercase().replaceFirstChar { it.uppercase() }

/**
 * The last [weeks] weeks (Monday to Sunday, oldest first) of habit completion per day for a heatmap; a day after [today], or with
 * nothing due, has a null progress so it is drawn blank.
 */
fun heatmap(habits: List<Habit>, log: Map<String, Set<LocalDate>>, skips: Map<String, Set<LocalDate>>, today: LocalDate, weeks: Int = 12): List<List<Pair<LocalDate, Progress?>>> {
    val thisMonday = today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY)
    return (weeks - 1 downTo 0).map { w ->
        val monday = thisMonday.minus(w * 7, DateTimeUnit.DAY)
        (0..6).map { i ->
            val d = monday.plus(i, DateTimeUnit.DAY)
            val due = dueHabits(habits, skips, d)
            d to (if (d > today || due.isEmpty()) null else Progress(due.count { d in log[it.id].orEmpty() }, due.size))
        }
    }
}

/** Each heatmap week's average completion (0..1) over the days that had habits due, or null for a week with none. */
fun weeklyRates(map: List<List<Pair<LocalDate, Progress?>>>): List<Float?> =
    map.map { week -> week.mapNotNull { it.second?.fraction }.let { f -> if (f.isEmpty()) null else f.average().toFloat() } }
