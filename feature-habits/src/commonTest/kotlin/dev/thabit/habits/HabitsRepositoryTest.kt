package dev.thabit.habits

import dev.thabit.core.VaultFiles
import kotlinx.datetime.LocalDate
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals

class HabitsRepositoryTest {
    private val fs = FakeFileSystem()
    private val repo = HabitsRepository(VaultFiles(fs, "/v".toPath()))
    private val d1 = LocalDate(2026, 3, 1)
    private val d2 = LocalDate(2026, 3, 2)

    @Test fun habitsRoundTrip() {
        val h = listOf(Habit("h1", "Read", listOf(1, 3, 5)), Habit("h2", "Walk", archived = true))
        repo.save(h)
        assertEquals(h, repo.habits())
    }

    @Test fun doneAndSkippedAreKeptApart() {
        repo.setMark("h1", d1, HabitMark.Done)
        repo.setMark("h1", d2, HabitMark.Skipped)
        assertEquals(mapOf("h1" to setOf(d1)), repo.log())
        assertEquals(mapOf("h1" to setOf(d2)), repo.skips())
    }

    @Test fun markingADayAgainReplacesItAndNullClearsIt() {
        repo.setMark("h1", d1, HabitMark.Done)
        repo.setMark("h1", d1, HabitMark.Skipped)
        assertEquals(emptyMap(), repo.log())
        repo.setMark("h1", d1, null)
        assertEquals(emptyMap(), repo.skips())
    }

    @Test fun setDoneIsTheOldTwoStateSwitch() {
        repo.setDone("h1", d1, true)
        assertEquals(setOf(d1), repo.log()["h1"])
        repo.setDone("h1", d1, false)
        assertEquals(emptyMap(), repo.log())
    }

    @Test fun anOlderTwoColumnLogStillReadsAsDone() {
        fs.createDirectories("/v/habits".toPath())
        fs.write("/v/habits/log.csv".toPath()) { writeUtf8("habit,date\nh1,2026-03-01\nh1,not-a-date\n") }
        assertEquals(mapOf("h1" to setOf(d1)), repo.log())
    }

    @Test fun aBrokenHabitsFileReadsAsNoHabitsInsteadOfCrashing() {
        fs.createDirectories("/v/habits".toPath())
        fs.write("/v/habits/habits.json".toPath()) { writeUtf8("{ not json") }
        assertEquals(emptyList(), repo.habits())
    }
}
