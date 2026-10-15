package dev.thabit.habits

import dev.thabit.core.Csv
import dev.thabit.core.VaultFiles
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer

/** Habits: what you track in `habits/habits.json` and the days you did or skipped them in `habits/log.csv`. */
class HabitsRepository(private val files: VaultFiles) {
    init { files.fs.createDirectories(files.dir("habits")) }

    private val habitsPath get() = files.dir("habits") / "habits.json"
    private val logPath get() = files.dir("habits") / "log.csv"

    fun habits(): List<Habit> = files.readJson(habitsPath, ListSerializer(Habit.serializer()), emptyList())

    fun save(habits: List<Habit>) = files.write(habitsPath, files.json.encodeToString(ListSerializer(Habit.serializer()), habits))

    private fun rows(): List<Triple<String, LocalDate, HabitMark>> =
        Csv.parse(files.read(logPath) ?: "").drop(1).mapNotNull { r ->
            val date = runCatching { LocalDate.parse(r.getOrElse(1) { "" }) }.getOrNull() ?: return@mapNotNull null
            // Older files have two columns, and every row there is a day that was done.
            Triple(r[0], date, if (r.getOrElse(2) { "" } == "skip") HabitMark.Skipped else HabitMark.Done)
        }

    /** The days each habit was done, by habit id. */
    fun log(): Map<String, Set<LocalDate>> =
        rows().filter { it.third == HabitMark.Done }.groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() }

    /** The days each habit was skipped on purpose (a sick day, a trip): they neither break a streak nor count against it. */
    fun skips(): Map<String, Set<LocalDate>> =
        rows().filter { it.third == HabitMark.Skipped }.groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() }

    /** Marks a day as done or skipped; null clears it. */
    fun setMark(habitId: String, date: LocalDate, mark: HabitMark?) {
        val rows = rows().filterNot { it.first == habitId && it.second == date }.toMutableList()
        if (mark != null) rows += Triple(habitId, date, mark)
        val out = listOf(listOf("habit", "date", "mark")) + rows.sortedWith(compareBy({ it.first }, { it.second })).map { listOf(it.first, it.second.toString(), if (it.third == HabitMark.Skipped) "skip" else "done") }
        files.write(logPath, Csv.format(out))
    }

    fun setDone(habitId: String, date: LocalDate, done: Boolean) = setMark(habitId, date, if (done) HabitMark.Done else null)
}
