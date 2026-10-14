package dev.thabit.core

import dev.thabit.habits.Habit
import dev.thabit.tasks.Task
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomeTest {
    private val wed = LocalDate(2026, 10, 14) // a Wednesday
    private val read = Habit("h1", "Read")
    private val gym = Habit("h2", "Gym", days = listOf(1, 3, 5))   // Mon, Wed, Fri
    private val old = Habit("h3", "Old", archived = true)

    @Test fun `day progress counts due habits and tasks and ignores skipped, archived and other-day habits`() {
        val log = mapOf("h1" to setOf(wed))
        val tasks = listOf(Task("t1", "a", due = wed), Task("t2", "b", done = true, doneOn = wed), Task("t3", "c", due = LocalDate(2026, 10, 20)))
        assertEquals(Progress(2, 4), dayProgress(listOf(read, gym, old), log, emptyMap(), tasks, wed))
        assertEquals(Progress(2, 3), dayProgress(listOf(read, gym, old), log, mapOf("h2" to setOf(wed)), tasks, wed))
    }

    @Test fun `an empty day has no fraction`() = assertNull(dayProgress(emptyList(), emptyMap(), emptyMap(), emptyList(), wed).fraction)

    @Test fun `the week strip runs Monday to Sunday and counts only days with something due`() {
        val strip = weekStrip(listOf(gym), mapOf("h2" to setOf(LocalDate(2026, 10, 12))), emptyMap(), wed)
        assertEquals(LocalDate(2026, 10, 12), strip.first().first); assertEquals(LocalDate(2026, 10, 18), strip.last().first)
        assertEquals(Progress(1, 1), strip[0].second); assertEquals(Progress(0, 1), strip[2].second); assertEquals(Progress(0, 0), strip[1].second)
    }

    @Test fun `mood series is oldest first with gaps as null`() {
        val s = moodSeries({ if (it == wed) 4 else if (it == LocalDate(2026, 10, 12)) 2 else null }, wed, 3)
        assertEquals(listOf(2, null, 4), s)
    }

    @Test fun `on this day finds earlier years only, newest first`() {
        val dates = listOf(LocalDate(2025, 10, 14), LocalDate(2026, 10, 14), LocalDate(2024, 10, 14), LocalDate(2025, 10, 15))
        assertEquals(listOf(LocalDate(2025, 10, 14), LocalDate(2024, 10, 14)), onThisDay(dates, wed))
    }

    @Test fun `upcoming tasks list overdue first, then soonest, skip done and plain undated ones`() {
        val tasks = listOf(
            Task("a", "later", due = LocalDate(2026, 10, 20)), Task("b", "overdue", due = LocalDate(2026, 10, 10)),
            Task("c", "plain"), Task("d", "urgent undated", priority = 3), Task("e", "done", done = true, due = LocalDate(2026, 10, 1)),
        )
        assertEquals(listOf("b", "a", "d"), upcomingTasks(tasks, wed).map { it.id })
    }

    @Test fun `greeting follows the hour`() { assertEquals("Good morning", greetingFor(8)); assertEquals("Good afternoon", greetingFor(13)); assertEquals("Good evening", greetingFor(22)); assertEquals("Good evening", greetingFor(3)) }

    @Test fun `the heatmap has the asked number of weeks, blank future days and blank days with nothing due`() {
        val gym = Habit("h2", "Gym", days = listOf(1, 3, 5))
        val map = heatmap(listOf(gym), mapOf("h2" to setOf(LocalDate(2026, 10, 12))), emptyMap(), wed, 3)
        assertEquals(3, map.size); assertEquals(7, map.first().size)
        val last = map.last()
        assertEquals(LocalDate(2026, 10, 12), last[0].first); assertEquals(Progress(1, 1), last[0].second)
        assertNull(last[1].second)           // Tuesday: nothing due
        assertEquals(Progress(0, 1), last[2].second) // Wednesday today, not done
        assertNull(last[4].second)           // Friday is after today
    }

    @Test fun `weekly rates average the days with something due and skip empty weeks`() {
        val read = Habit("h1", "Read")
        val map = heatmap(listOf(read), mapOf("h1" to setOf(LocalDate(2026, 10, 12), LocalDate(2026, 10, 13))), emptyMap(), wed, 2)
        val rates = weeklyRates(map)
        assertEquals(2, rates.size); assertEquals(0f, rates[0]!!, 0.001f)
        assertEquals(2f / 3f, rates[1]!!, 0.001f)
    }
}
