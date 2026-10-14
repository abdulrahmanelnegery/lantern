package dev.dawem.core

import dev.dawem.journal.JournalEntry
import dev.dawem.notes.Note
import dev.dawem.tasks.Task
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** What happened on one day, across the whole vault. */
data class DaySummary(
    val date: LocalDate,
    val journalWords: Int,
    val mood: Int?,
    val notesCreated: Int,
    val tasksDone: Int,
    val habitsDone: Int,
) {
    val isEmpty: Boolean get() = journalWords == 0 && mood == null && notesCreated == 0 && tasksDone == 0 && habitsDone == 0
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
): List<DaySummary> {
    val notesByDay = notes.groupingBy { it.created.take(10) }.eachCount()
    val tasksByDay = tasks.filter { it.done && it.doneOn != null }.groupingBy { it.doneOn!! }.eachCount()
    val habitsByDay = habitLog.values.flatten().groupingBy { it }.eachCount()
    val out = ArrayList<DaySummary>()
    var d = from
    var guard = 0
    while (d <= to && guard++ < 4000) {
        val j = journal(d)
        out += DaySummary(d, j?.body?.let(::wordCount) ?: 0, j?.mood, notesByDay[d.toString()] ?: 0, tasksByDay[d] ?: 0, habitsByDay[d] ?: 0)
        d = d.plus(1, DateTimeUnit.DAY)
    }
    return out
}

/** Totals over a run of days, for the period tiles. */
data class PeriodTotals(val days: Int, val journalDays: Int, val words: Int, val moodAverage: Double?, val notes: Int, val tasks: Int, val habitChecks: Int)

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
    )
}
