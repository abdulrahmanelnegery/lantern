package dev.thabit.habits

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Days in a row, ending today, that [habit] was done on the days it applies; today not yet done does not break it. */
fun streak(habit: Habit, done: Set<LocalDate>, today: LocalDate, skipped: Set<LocalDate> = emptySet()): Int {
    // A skipped day is as if the habit did not apply: it neither breaks the streak nor adds to it.
    fun applies(d: LocalDate) = (habit.days.isEmpty() || d.dayOfWeek.ordinal + 1 in habit.days) && d !in skipped
    var day = today
    if (applies(day) && day !in done) day = day.minus(1, DateTimeUnit.DAY)
    var n = 0
    var guard = 0
    while (guard++ < 3660) {
        if (!applies(day)) { day = day.minus(1, DateTimeUnit.DAY); continue }
        if (day !in done) break
        n++
        day = day.minus(1, DateTimeUnit.DAY)
    }
    return n
}

/** Everything one habit's page shows. Rates are 0..1 over the days the habit applied to. */
data class HabitStats(
    val current: Int,
    val best: Int,
    val total: Int,
    val rate7: Double,
    val rate30: Double,
    val rate90: Double,
    /** Times done on Monday .. Sunday. */
    val byWeekday: List<Int>,
    val firstDay: LocalDate?,
)

fun Habit.appliesOn(d: LocalDate): Boolean = days.isEmpty() || d.dayOfWeek.ordinal + 1 in days

/** The days from [from] to [to] (both included) that [habit] applies to. */
fun scheduledDays(habit: Habit, from: LocalDate, to: LocalDate): List<LocalDate> {
    val out = ArrayList<LocalDate>()
    var d = from
    var guard = 0
    while (d <= to && guard++ < 4000) { if (habit.appliesOn(d)) out += d; d = d.plus(1, DateTimeUnit.DAY) }
    return out
}

fun habitStats(habit: Habit, done: Set<LocalDate>, today: LocalDate, skipped: Set<LocalDate> = emptySet()): HabitStats {
    val first = done.minOrNull()
    fun rate(window: Int): Double {
        first ?: return 0.0
        val from = maxOf(today.minus(window - 1, DateTimeUnit.DAY), first)
        val scheduled = scheduledDays(habit, from, today).filter { it !in skipped && (it != today || it in done) }
        return if (scheduled.isEmpty()) 0.0 else scheduled.count { it in done }.toDouble() / scheduled.size
    }
    var best = 0
    var run = 0
    if (first != null) {
        for (d in scheduledDays(habit, first, today).filter { it !in skipped }) {
            if (d in done) { run++; if (run > best) best = run } else if (d != today) run = 0
        }
    }
    val weekday = MutableList(7) { 0 }
    done.forEach { weekday[it.dayOfWeek.ordinal]++ }
    return HabitStats(streak(habit, done, today, skipped), best, done.size, rate(7), rate(30), rate(90), weekday, first)
}

/**
 * Days in a row, ending today, on which every habit that applied was done or skipped. A day with
 * nothing scheduled is passed over; today unfinished does not break it.
 */
fun perfectDays(habits: List<Habit>, done: Map<String, Set<LocalDate>>, skipped: Map<String, Set<LocalDate>>, today: LocalDate): Int {
    fun complete(d: LocalDate): Boolean? {
        val due = habits.filter { !it.archived && it.appliesOn(d) }
        if (due.isEmpty()) return null
        return due.all { d in done[it.id].orEmpty() || d in skipped[it.id].orEmpty() }
    }
    var d = today
    if (complete(d) == false) d = d.minus(1, DateTimeUnit.DAY)
    var n = 0
    var guard = 0
    while (guard++ < 3660) {
        when (complete(d)) {
            true -> n++
            false -> break
            null -> {}
        }
        d = d.minus(1, DateTimeUnit.DAY)
    }
    return n
}
