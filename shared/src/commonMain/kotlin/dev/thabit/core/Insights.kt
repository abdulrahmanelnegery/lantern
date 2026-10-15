package dev.thabit.core

import dev.thabit.tasks.Task
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** What happened on one day: tasks finished and habits kept. */
data class DaySummary(
    val date: LocalDate,
    val tasksDone: Int,
    val habitsDone: Int,
) {
    val isEmpty: Boolean get() = tasksDone == 0 && habitsDone == 0
}

/** One summary per day from [from] to [to], oldest first. */
fun daySummaries(
    from: LocalDate,
    to: LocalDate,
    tasks: List<Task>,
    habitLog: Map<String, Set<LocalDate>>,
): List<DaySummary> {
    val tasksByDay = tasks.filter { it.done && it.doneOn != null }.groupingBy { it.doneOn!! }.eachCount()
    val habitsByDay = habitLog.values.flatten().groupingBy { it }.eachCount()
    val out = ArrayList<DaySummary>()
    var d = from
    var guard = 0
    while (d <= to && guard++ < 4000) {
        out += DaySummary(d, tasksByDay[d] ?: 0, habitsByDay[d] ?: 0)
        d = d.plus(1, DateTimeUnit.DAY)
    }
    return out
}

/** Totals over a run of days, for the period tiles. */
data class PeriodTotals(val days: Int, val tasks: Int, val habitChecks: Int)

fun totals(days: List<DaySummary>): PeriodTotals =
    PeriodTotals(days = days.size, tasks = days.sumOf { it.tasksDone }, habitChecks = days.sumOf { it.habitsDone })
