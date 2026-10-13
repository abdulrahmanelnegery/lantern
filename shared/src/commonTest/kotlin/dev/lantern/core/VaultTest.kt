package dev.lantern.core

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    @Test fun moneyTotalsBalancesAndYearFiles() {
        val v = vault()
        val acc = Account("a", "Cash", "USD", 10_000)
        v.saveAccounts(listOf(acc))
        v.saveTransaction(Txn("", LocalDate(2026, 10, 1), 250_000, "a", "Salary"))
        v.saveTransaction(Txn("", LocalDate(2026, 10, 3), -4_550, "a", "Food", "lunch, with \"friends\""))
        v.saveTransaction(Txn("", LocalDate(2026, 10, 9), -12_000, "a", "Rent"))
        v.saveTransaction(Txn("", LocalDate(2025, 12, 30), -1_000, "a", "Food"))
        val all = v.allTransactions()
        assertEquals(4, all.size)
        assertEquals("lunch, with \"friends\"", all.first { it.category == "Food" && it.date.year == 2026 }.note)
        val m = monthSummary(all, 2026, 10)
        assertEquals(250_000, m.incomeMinor)
        assertEquals(-16_550, m.expenseMinor)
        assertEquals("Rent", m.byCategory.first().first)
        assertEquals(10_000 + 250_000 - 4_550 - 12_000 - 1_000, balance(acc, all))
        assertEquals(2, v.fs.list("/v/money".toPath()).count { it.name.endsWith(".csv") })
        v.deleteTransaction(all.first().id)
        assertEquals(3, v.allTransactions().size)
    }

    @Test fun moneyParsingAndFormatting() {
        assertEquals(1250L, parseMoney("12.5"))
        assertEquals(1250L, parseMoney("12,50"))
        assertEquals(-300L, parseMoney("-3"))
        assertNull(parseMoney("abc"))
        assertNull(parseMoney("1.2.3"))
        assertEquals("12.05", formatMoney(1205))
        assertEquals("-0.07", formatMoney(-7))
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
