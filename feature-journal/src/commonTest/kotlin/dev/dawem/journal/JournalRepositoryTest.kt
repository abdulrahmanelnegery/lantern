package dev.dawem.journal

import dev.dawem.core.VaultFiles
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JournalRepositoryTest {
    private val clock = object : Clock { override fun now(): Instant = Instant.parse("2026-03-10T08:00:00Z") }
    private val fs = FakeFileSystem()
    private val repo = JournalRepository(VaultFiles(fs, "/v".toPath(), clock, TimeZone.UTC))
    private val day = LocalDate(2026, 3, 9)

    @Test fun anEntryKeepsItsMoodAndTags() {
        repo.save(JournalEntry(day, "a long day", 4, listOf("work")))
        assertEquals(JournalEntry(day, "a long day", 4, listOf("work")), repo.entry(day))
    }

    @Test fun moodIsKeptInsideOneToFive() {
        repo.save(JournalEntry(day, "x", 9))
        assertEquals(5, repo.entry(day)?.mood)
    }

    @Test fun savingAnEmptyEntryRemovesTheFile() {
        repo.save(JournalEntry(day, "text"))
        repo.save(JournalEntry(day, "  "))
        assertNull(repo.entry(day))
        assertEquals(emptyList(), repo.dates())
    }

    @Test fun datesAreNewestFirstAcrossYears() {
        repo.save(JournalEntry(LocalDate(2025, 12, 31), "a"))
        repo.save(JournalEntry(LocalDate(2026, 1, 2), "b"))
        repo.save(JournalEntry(LocalDate(2026, 3, 9), "c"))
        assertEquals(listOf(LocalDate(2026, 3, 9), LocalDate(2026, 1, 2), LocalDate(2025, 12, 31)), repo.dates())
    }

    @Test fun entriesLooksBackFromTheVaultClocksToday() {
        repo.save(JournalEntry(LocalDate(2026, 3, 9), "recent"))
        repo.save(JournalEntry(LocalDate(2026, 1, 1), "old"))
        assertEquals(listOf("recent"), repo.entries(7).map { it.body })
    }

    @Test fun streakAndAverageMood() {
        val days = setOf(LocalDate(2026, 3, 8), LocalDate(2026, 3, 9))
        assertEquals(2, journalStreak(days, LocalDate(2026, 3, 10)))
        assertEquals(0, journalStreak(days, LocalDate(2026, 3, 12)))
        assertEquals(3.0, averageMood(listOf(JournalEntry(day, "", 2), JournalEntry(day, "", 4), JournalEntry(day, "", null))))
    }
}
