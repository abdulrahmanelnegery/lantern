package dev.lantern.core

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

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

/** Average mood over entries that have one, or null. */
fun averageMood(entries: List<JournalEntry>): Double? = entries.mapNotNull { it.mood }.let { m -> if (m.isEmpty()) null else m.sum().toDouble() / m.size }
