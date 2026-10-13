package dev.lantern.core

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

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

fun habitStats(habit: Habit, done: Set<LocalDate>, today: LocalDate): HabitStats {
    val first = done.minOrNull()
    fun rate(window: Int): Double {
        first ?: return 0.0
        val from = maxOf(today.minus(window - 1, DateTimeUnit.DAY), first)
        val scheduled = scheduledDays(habit, from, today).filter { it != today || it in done }
        return if (scheduled.isEmpty()) 0.0 else scheduled.count { it in done }.toDouble() / scheduled.size
    }
    var best = 0
    var run = 0
    if (first != null) {
        for (d in scheduledDays(habit, first, today)) {
            if (d in done) { run++; if (run > best) best = run } else if (d != today) run = 0
        }
    }
    val weekday = MutableList(7) { 0 }
    done.forEach { weekday[it.dayOfWeek.ordinal]++ }
    return HabitStats(streak(habit, done, today), best, done.size, rate(7), rate(30), rate(90), weekday, first)
}

/** What happened on one day, across the whole vault. */
data class DaySummary(
    val date: LocalDate,
    val journalWords: Int,
    val mood: Int?,
    val notesCreated: Int,
    val tasksDone: Int,
    val habitsDone: Int,
    val spentMinor: Long,
) {
    val isEmpty: Boolean get() = journalWords == 0 && mood == null && notesCreated == 0 && tasksDone == 0 && habitsDone == 0 && spentMinor == 0L
}

private fun wordCount(text: String): Int = text.split(Regex("\\s+")).count { it.isNotEmpty() }

/** One summary per day from [from] to [to], oldest first. */
fun daySummaries(
    from: LocalDate,
    to: LocalDate,
    journal: (LocalDate) -> JournalEntry?,
    notes: List<Note>,
    tasks: List<Task>,
    habitLog: Map<String, Set<LocalDate>>,
    txns: List<Txn>,
): List<DaySummary> {
    val notesByDay = notes.groupingBy { it.created.take(10) }.eachCount()
    val tasksByDay = tasks.filter { it.done && it.doneOn != null }.groupingBy { it.doneOn!! }.eachCount()
    val habitsByDay = habitLog.values.flatten().groupingBy { it }.eachCount()
    val spentByDay = txns.filter { it.amountMinor < 0 }.groupBy { it.date }.mapValues { -it.value.sumOf { t -> t.amountMinor } }
    val out = ArrayList<DaySummary>()
    var d = from
    var guard = 0
    while (d <= to && guard++ < 4000) {
        val j = journal(d)
        out += DaySummary(d, j?.body?.let(::wordCount) ?: 0, j?.mood, notesByDay[d.toString()] ?: 0, tasksByDay[d] ?: 0, habitsByDay[d] ?: 0, spentByDay[d] ?: 0L)
        d = d.plus(1, DateTimeUnit.DAY)
    }
    return out
}

/** Totals over a run of days, for the period tiles. */
data class PeriodTotals(val days: Int, val journalDays: Int, val words: Int, val moodAverage: Double?, val notes: Int, val tasks: Int, val habitChecks: Int, val spentMinor: Long)

fun totals(days: List<DaySummary>): PeriodTotals {
    val moods = days.mapNotNull { it.mood }
    return PeriodTotals(
        days = days.size,
        journalDays = days.count { it.journalWords > 0 || it.mood != null },
        words = days.sumOf { it.journalWords },
        moodAverage = if (moods.isEmpty()) null else moods.sum().toDouble() / moods.size,
        notes = days.sumOf { it.notesCreated },
        tasks = days.sumOf { it.tasksDone },
        habitChecks = days.sumOf { it.habitsDone },
        spentMinor = days.sumOf { it.spentMinor },
    )
}
