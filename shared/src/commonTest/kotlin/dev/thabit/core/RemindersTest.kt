package dev.thabit.core

import dev.thabit.habits.Habit
import dev.thabit.tasks.Task
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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

    // ---- one scheduling pass (T-02) -------------------------------------------------------------

    private fun plan(habits: List<Habit>, now: LocalDateTime, last: LocalDateTime?, snooze: Reminder? = null, tasks: List<Task> = emptyList(), log: Map<String, Set<LocalDate>> = emptyMap()) =
        Reminders.plan(habits, tasks, log, emptyMap(), now, last, snooze)

    @Test fun `two habits at the same minute are both shown when the alarm fires`() {
        val a = Habit("a", "Read", remindAt = "08:00"); val b = Habit("b", "Walk", remindAt = "08:00")
        val p = plan(listOf(a, b), at(day, 8, 0).let { LocalDateTime(it.year, it.monthNumber, it.dayOfMonth, 8, 0, 20) }, at(day, 7, 30))
        assertEquals(setOf("Read", "Walk"), p.showNow.map { it.title }.toSet())
    }

    @Test fun `a reminder that came due 20 minutes ago is replayed once and not again on the next pass`() {
        val h = Habit("a", "Read", remindAt = "08:00")
        val first = plan(listOf(h), at(day, 8, 20), at(day, 7, 30))
        assertEquals(listOf("Read"), first.showNow.map { it.title })
        assertTrue(plan(listOf(h), at(day, 8, 25), at(day, 8, 20)).showNow.isEmpty(), "the previous pass was after it, so it is not shown again")
    }

    @Test fun `a reminder already done is not replayed`() {
        val h = Habit("a", "Read", remindAt = "08:00")
        assertTrue(plan(listOf(h), at(day, 9), at(day, 7), log = mapOf("a" to setOf(day))).showNow.isEmpty())
    }

    @Test fun `setting a reminder time that is already past does not fire it at once`() {
        val h = Habit("a", "Read", remindAt = "08:00")
        assertTrue(plan(listOf(h), at(day, 15), at(day, 14, 59)).showNow.isEmpty())
    }

    @Test fun `no previous pass means nothing is replayed`() {
        assertTrue(plan(listOf(Habit("a", "Read", remindAt = "08:00")), at(day, 15), null).showNow.isEmpty())
    }

    @Test fun `replay goes back at most twelve hours`() {
        val h = Habit("a", "Read", remindAt = "08:00")
        assertTrue(plan(listOf(h), at(day, 23), at(day, 6)).showNow.isEmpty(), "15 hours late is too old")
        assertEquals(1, plan(listOf(h), at(day, 19), at(day, 6)).showNow.size, "11 hours late still counts")
    }

    @Test fun `the next alarm is the soonest of what is still ahead`() {
        val a = Habit("a", "Read", remindAt = "21:00"); val b = Habit("b", "Walk", remindAt = "18:00")
        assertEquals("Walk", plan(listOf(a, b), at(day, 9), at(day, 8, 50)).next?.title)
    }

    @Test fun `a snooze that is due is shown if the habit is still open, and cleared either way`() {
        val h = Habit("a", "Read", remindAt = "08:00")
        val snooze = Reminder(Reminder.Kind.Habit, "a", "Read", at(day, 8, 10))
        val shown = plan(listOf(h), at(day, 8, 12), at(day, 8, 9), snooze)
        assertEquals(listOf("Read"), shown.showNow.map { it.title }); assertNull(shown.snooze)
        val done = plan(listOf(h), at(day, 8, 12), at(day, 8, 9), snooze, log = mapOf("a" to setOf(day)))
        assertTrue(done.showNow.isEmpty()); assertNull(done.snooze)
    }

    @Test fun `a snooze still ahead is kept and wins the alarm when it is sooner than the next regular reminder`() {
        val h = Habit("a", "Read", remindAt = "21:00")
        val snooze = Reminder(Reminder.Kind.Habit, "a", "Read", at(day, 8, 30))
        val p = plan(listOf(h), at(day, 8, 20), at(day, 8, 19), snooze)
        assertEquals(snooze, p.snooze); assertEquals(snooze, p.next)
        val later = plan(listOf(h), at(day, 8, 20), at(day, 8, 19), snooze.copy(at = at(day, 22)))
        assertEquals(at(day, 21), later.next?.at, "the regular 21:00 reminder is sooner than a 22:00 snooze")
    }

    @Test fun `a snooze and a regular reminder for the same habit are not shown twice in one pass`() {
        val h = Habit("a", "Read", remindAt = "08:00")
        val snooze = Reminder(Reminder.Kind.Habit, "a", "Read", at(day, 8, 10))
        assertEquals(1, plan(listOf(h), at(day, 8, 12), at(day, 7, 59), snooze).showNow.size)
    }

    @Test fun `a task due earlier today is replayed once with its time`() {
        val t = Task("t", "Call", due = day, remindAt = "10:00")
        val p = plan(emptyList(), at(day, 10, 40), at(day, 9), tasks = listOf(t))
        assertEquals(listOf("Call"), p.showNow.map { it.title })
    }
}
