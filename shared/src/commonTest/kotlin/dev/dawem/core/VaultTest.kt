package dev.dawem.core

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import dev.dawem.habits.Habit
import dev.dawem.habits.HabitMark
import dev.dawem.habits.habitStats
import dev.dawem.habits.perfectDays
import dev.dawem.habits.streak
import dev.dawem.journal.JournalEntry
import dev.dawem.journal.journalStreak
import dev.dawem.notes.Note
import dev.dawem.tasks.Task

class VaultTest {
    private class Fixed(var at: String = "2026-10-14T09:30:00Z") : Clock {
        override fun now(): Instant = Instant.parse(at)
    }

    private fun vault(clock: Clock = Fixed()) = Vault(FakeFileSystem(), "/v".toPath(), clock, kotlinx.datetime.TimeZone.UTC)

    @Test fun frontmatterRoundTrips() {
        val doc = Document(linkedMapOf("title" to "A: tricky \"title\"", "tags" to Frontmatter.formatList(listOf("a", "b c", "d,e"))), "Body\nline two\n")
        val back = Frontmatter.parse(Frontmatter.write(doc))
        assertEquals("A: tricky \"title\"", back.meta["title"])
        assertEquals(listOf("a", "b c", "d,e"), back.list("tags"))
        assertEquals("Body\nline two\n", back.body)
    }

    @Test fun textWithoutHeaderIsJustABody() {
        val doc = Frontmatter.parse("# Hello\n\ntext")
        assertTrue(doc.meta.isEmpty())
        assertEquals("# Hello\n\ntext", doc.body)
    }

    @Test fun csvHandlesQuotesCommasAndNewlines() {
        val rows = listOf(listOf("a", "b,c", "d \"q\""), listOf("x", "line\ntwo", ""))
        assertEquals(rows, Csv.parse(Csv.format(rows)))
    }

    @Test fun notesAreSavedRenamedAndTrashed() {
        val v = vault()
        val saved = v.saveNote(Note("", "First idea", "# First idea\n\nhello", "", "", listOf("work")))
        assertEquals(1, v.notes().size)
        assertEquals("work", v.notes().single().tags.single())
        v.saveNote(saved.copy(title = "Renamed"))
        assertEquals(1, v.notes().size, "a rename moves the file, it does not copy it")
        assertEquals("Renamed", v.notes().single().title)
        v.trashNote(saved.id)
        assertTrue(v.notes().isEmpty())
    }

    @Test fun pinnedNotesComeFirst() {
        val v = vault()
        v.saveNote(Note("", "old", "x", "", ""))
        v.saveNote(Note("", "pinned", "y", "", "", pinned = true))
        assertEquals("pinned", v.notes().first().title)
    }

    @Test fun journalSavesMoodAndEmptyEntriesVanish() {
        val v = vault()
        val day = LocalDate(2026, 10, 14)
        v.saveJournal(JournalEntry(day, "A good day.", 4, listOf("home")))
        assertEquals(4, v.journal(day)?.mood)
        assertEquals("A good day.", v.journal(day)?.body)
        assertEquals(listOf(day), v.journalDates())
        v.saveJournal(JournalEntry(day, "", null))
        assertNull(v.journal(day))
    }

    @Test fun tasksRoundTripAsAMarkdownChecklist() {
        val v = vault()
        val tasks = listOf(
            Task("aaaa1111", "Buy milk", false, LocalDate(2026, 10, 20), 2, "Home errands"),
            Task("bbbb2222", "Call back: the plumber", true),
        )
        v.saveTasks(tasks)
        assertEquals(tasks, v.tasks())
        assertTrue(v.fs.read("/v/tasks/Tasks.md".toPath()) { readUtf8() }.contains("- [x] Call back: the plumber"))
    }

    @Test fun habitStreakCountsDaysInARow() {
        val today = LocalDate(2026, 10, 14)
        val habit = Habit("h", "Read")
        val done = setOf(LocalDate(2026, 10, 13), LocalDate(2026, 10, 12), LocalDate(2026, 10, 11), LocalDate(2026, 10, 9))
        assertEquals(3, streak(habit, done, today), "today not done yet does not break it")
        assertEquals(4, streak(habit, done + today, today))
        assertEquals(0, streak(habit, emptySet(), today))
    }

    @Test fun weeklyHabitsSkipOtherDays() {
        // 2026-10-14 is a Wednesday (3); the habit is Mon/Wed/Fri.
        val habit = Habit("h", "Gym", days = listOf(1, 3, 5))
        val done = setOf(LocalDate(2026, 10, 14), LocalDate(2026, 10, 12), LocalDate(2026, 10, 9))
        assertEquals(3, streak(habit, done, LocalDate(2026, 10, 14)))
    }

    @Test fun habitLogPersists() {
        val v = vault()
        val d = LocalDate(2026, 10, 14)
        v.setHabitDone("h1", d, true)
        v.setHabitDone("h1", LocalDate(2026, 10, 13), true)
        v.setHabitDone("h2", d, true)
        assertEquals(setOf(d, LocalDate(2026, 10, 13)), v.habitLog()["h1"])
        v.setHabitDone("h1", d, false)
        assertEquals(setOf(LocalDate(2026, 10, 13)), v.habitLog()["h1"])
    }

    @Test fun searchFindsNotesJournalAndTasks() {
        val v = vault()
        v.saveNote(Note("", "Garden plan", "plant tomatoes in spring", "", ""))
        v.saveJournal(JournalEntry(LocalDate(2026, 10, 14), "Bought tomatoes today", 3))
        v.saveTasks(listOf(Task("t1", "Water the tomatoes")))
        val kinds = v.search("TOMATO").map { it.kind }.toSet()
        assertEquals(setOf("note", "journal", "task"), kinds)
        assertTrue(v.search("   ").isEmpty())
    }

    @Test fun slugsAreSafe() {
        assertEquals("hello-world", slugOf("  Hello, World!  "))
        assertEquals("untitled", slugOf("???"))
        assertEquals(8, newId().length)
    }
}

class InsightsTest {
    private val today = LocalDate(2026, 10, 14) // a Wednesday

    @Test fun habitStatsCountStreaksAndRates() {
        val h = Habit("h", "Read")
        val done = setOf(11, 12, 13).map { LocalDate(2026, 10, it) }.toSet() + setOf(5, 6, 7, 8).map { LocalDate(2026, 10, it) }
        val s = habitStats(h, done, today)
        assertEquals(3, s.current, "today not done yet does not break it")
        assertEquals(4, s.best)
        assertEquals(7, s.total)
        assertEquals(LocalDate(2026, 10, 5), s.firstDay)
        // since the first day: Oct 5..13 is 9 scheduled days (today is skipped while undone), 7 done
        assertEquals(7.0 / 9, s.rate30, 0.0001)
    }

    @Test fun neverDoneHasEmptyStats() {
        val s = habitStats(Habit("h", "x"), emptySet(), today)
        assertEquals(0, s.current); assertEquals(0, s.best); assertEquals(0.0, s.rate90)
    }

    @Test fun weeklyHabitsOnlyCountTheirDays() {
        val h = Habit("h", "Gym", days = listOf(1, 3, 5)) // Mon, Wed, Fri
        val done = setOf(LocalDate(2026, 10, 9), LocalDate(2026, 10, 12), LocalDate(2026, 10, 14)) // Fri, Mon, Wed
        val s = habitStats(h, done, today)
        assertEquals(3, s.best)
        assertEquals(1, s.byWeekday[0]); assertEquals(1, s.byWeekday[2]); assertEquals(1, s.byWeekday[4])
        assertEquals(1.0, s.rate7, 0.0001)
    }

    @Test fun daySummariesGatherEverythingForEachDay() {
        val day = LocalDate(2026, 10, 13)
        val summary = daySummaries(
            day, today,
            journal = { if (it == day) JournalEntry(it, "one two three", 4) else null },
            notes = listOf(Note("n", "t", "b", "2026-10-13T08:00:00", "2026-10-13T08:00:00")),
            tasks = listOf(Task("a", "x", done = true, doneOn = day), Task("b", "y", done = true)),
            habitLog = mapOf("h" to setOf(day, today)),
        )
        assertEquals(2, summary.size)
        val d = summary.first()
        assertEquals(3, d.journalWords); assertEquals(4, d.mood); assertEquals(1, d.notesCreated)
        assertEquals(1, d.tasksDone); assertEquals(1, d.habitsDone)
        assertTrue(summary.last().journalWords == 0 && summary.last().habitsDone == 1)
        val t = totals(summary)
        assertEquals(1, t.journalDays); assertEquals(4.0, t.moodAverage); assertEquals(2, t.habitChecks)
    }

    @Test fun aTaskKeepsItsDoneDayInTheFile() {
        val v = Vault(FakeFileSystem(), "/v".toPath())
        v.saveTasks(listOf(Task("a", "Ship it", done = true, doneOn = LocalDate(2026, 10, 12))))
        assertEquals(LocalDate(2026, 10, 12), v.tasks().single().doneOn)
    }
}


class HabitMarksTest {
    private val today = LocalDate(2026, 10, 14)

    @Test fun skippedDaysDoNotBreakAStreak() {
        val h = Habit("h", "Read")
        val done = setOf(LocalDate(2026, 10, 13), LocalDate(2026, 10, 11), LocalDate(2026, 10, 10))
        val skipped = setOf(LocalDate(2026, 10, 12))
        assertEquals(1, streak(h, done, today))
        assertEquals(3, streak(h, done, today, skipped))
        val stats = habitStats(h, done, today, skipped)
        assertEquals(3, stats.best)
        assertEquals(1.0, stats.rate7, 0.0001, "a skipped day is not held against the rate")
    }

    @Test fun marksPersistAndOlderFilesStillRead() {
        val fs = FakeFileSystem()
        val v = Vault(fs, "/v".toPath())
        // An older two-column log: every row is a day done.
        fs.write("/v/habits/log.csv".toPath()) { writeUtf8("habit,date\nh1,2026-10-13\n") }
        assertEquals(setOf(LocalDate(2026, 10, 13)), v.habitLog()["h1"])
        v.setHabitMark("h1", LocalDate(2026, 10, 12), HabitMark.Skipped)
        v.setHabitMark("h1", LocalDate(2026, 10, 14), HabitMark.Done)
        assertEquals(setOf(LocalDate(2026, 10, 13), LocalDate(2026, 10, 14)), v.habitLog()["h1"])
        assertEquals(setOf(LocalDate(2026, 10, 12)), v.habitSkips()["h1"])
        v.setHabitMark("h1", LocalDate(2026, 10, 12), null)
        assertTrue(v.habitSkips()["h1"].isNullOrEmpty())
    }

    @Test fun journalAndPerfectDayStreaks() {
        val days = setOf(13, 12, 11, 9).map { LocalDate(2026, 10, it) }.toSet()
        assertEquals(3, journalStreak(days, today), "today empty does not break it")
        val habits = listOf(Habit("a", "A"), Habit("b", "B"))
        val done = mapOf("a" to setOf(13, 12, 11).map { LocalDate(2026, 10, it) }.toSet(), "b" to setOf(13, 11).map { LocalDate(2026, 10, it) }.toSet())
        val skipped = mapOf("b" to setOf(LocalDate(2026, 10, 12)))
        assertEquals(3, perfectDays(habits, done, skipped, today))
        assertEquals(0, perfectDays(habits, emptyMap(), emptyMap(), today))
    }

    @Test fun quotesRepeatPerDayAndShuffle() {
        assertEquals(Quotes.affirmation(today), Quotes.affirmation(today))
        assertTrue(Quotes.affirmation(today, 1) != Quotes.affirmation(today, 0))
        assertTrue(Quotes.tip(today).isNotBlank())
    }
}
