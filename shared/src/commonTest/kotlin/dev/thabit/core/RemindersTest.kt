package dev.thabit.core

import dev.thabit.habits.Habit
import dev.thabit.tasks.Task
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemindersTest {
    private val day = LocalDate(2026, 10, 14) // Wednesday
    private fun at(d: LocalDate, h: Int, m: Int = 0) = LocalDateTime(d.year, d.monthNumber, d.dayOfMonth, h, m)

    @Test fun `a habit reminds at its time on the days it applies, not once done or skipped`() {
        val read = Habit("h1", "Read", remindAt = "21:00")
        val gym = Habit("h2", "Gym", days = listOf(1, 5), remindAt = "07:00") // Mon, Fri
        val r = Reminders.upcoming(listOf(read, gym), emptyList(), mapOf("h1" to setOf(LocalDate(2026, 10, 15))), emptyMap(), at(day, 8), 3)
        assertEquals(
            listOf(at(day, 21) to "Read", at(LocalDate(2026, 10, 16), 7) to "Gym", at(LocalDate(2026, 10, 16), 21) to "Read"),
            r.map { it.at to it.title },
        )
    }

    @Test fun `a task reminds on its due day at its time and not when done or without a due day`() {
        val tasks = listOf(Task("a", "Call", due = day, remindAt = "18:00"), Task("b", "Done", done = true, due = day, remindAt = "18:00"), Task("c", "No due", remindAt = "18:00"), Task("d", "No time", due = day))
        assertEquals(listOf("Call"), Reminders.upcoming(emptyList(), tasks, emptyMap(), emptyMap(), at(day, 9), 3).map { it.title })
    }

    @Test fun `reminders already in the past or exactly now are left out`() {
        val h = Habit("h1", "Read", remindAt = "09:00")
        assertTrue(Reminders.upcoming(listOf(h), emptyList(), emptyMap(), emptyMap(), at(day, 9), 1).isEmpty())
        assertEquals(1, Reminders.upcoming(listOf(h), emptyList(), emptyMap(), emptyMap(), at(day, 8, 59), 1).size)
    }

    @Test fun `a bad time or an archived habit never reminds, and any() says whether anything is set`() {
        val bad = Habit("h1", "X", remindAt = "25:00"); val old = Habit("h2", "Y", archived = true, remindAt = "08:00")
        assertTrue(Reminders.upcoming(listOf(bad, old), emptyList(), emptyMap(), emptyMap(), at(day, 0), 2).isEmpty())
        assertFalse(Reminders.any(listOf(bad, old), emptyList())); assertTrue(Reminders.any(listOf(Habit("h", "Z", remindAt = "08:00")), emptyList()))
    }
}
