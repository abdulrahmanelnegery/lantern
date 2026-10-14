package dev.dawem.tasks

import dev.dawem.core.VaultFiles
import kotlinx.datetime.LocalDate
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals

class TasksRepositoryTest {
    private val fs = FakeFileSystem()
    private val files = VaultFiles(fs, "/v".toPath())
    private val repo = TasksRepository(files)

    @Test fun tasksRoundTripWithDueDatePriorityListAndDoneDay() {
        val t = Task("t1", "Pay rent", true, LocalDate(2026, 3, 1), 3, "Home stuff", LocalDate(2026, 2, 28))
        repo.save(listOf(t, Task("t2", "Call mum")))
        assertEquals(listOf(t, Task("t2", "Call mum")), repo.tasks())
    }

    @Test fun theFileIsAMarkdownChecklist() {
        repo.save(listOf(Task("t1", "One", true), Task("t2", "Two")))
        val text = fs.read("/v/tasks/Tasks.md".toPath()) { readUtf8() }
        assertEquals(true, text.contains("- [x] One id:t1"), text)
        assertEquals(true, text.contains("- [ ] Two id:t2"), text)
    }

    @Test fun linesThatAreNotTasksAreIgnoredAndATaskWithoutAnIdGetsOne() {
        fs.createDirectories("/v/tasks".toPath())
        fs.write("/v/tasks/Tasks.md".toPath()) { writeUtf8("# Tasks\n\nsome prose\n- [ ] Plain task\n- [ ]   \n") }
        val tasks = repo.tasks()
        assertEquals(listOf("Plain task"), tasks.map { it.title })
        assertEquals(8, tasks.single().id.length)
    }

    @Test fun aTitleThatLooksLikeAMarkerStaysWhenItIsNotAtTheEnd() {
        repo.save(listOf(Task("t1", "due:soon is not a date")))
        assertEquals("due:soon is not a date", repo.tasks().single().title)
    }
}

class TaskExtrasTest {
    private val fs = FakeFileSystem()
    private val repo = TasksRepository(VaultFiles(fs, "/v".toPath()))
    private val day = LocalDate(2026, 10, 14)

    @Test fun repeatTagsReminderAndSubtasksRoundTrip() {
        val t = Task("t1", "Water plants", false, day, 2, "Home", null, "weekly", listOf("garden", "morning chores"), "08:30",
            listOf(Subtask("Fill the can"), Subtask("Balcony", true)))
        repo.save(listOf(t, Task("t2", "Plain")))
        assertEquals(listOf(t, Task("t2", "Plain")), repo.tasks())
        val text = fs.read("/v/tasks/Tasks.md".toPath()) { readUtf8() }
        assertEquals(true, text.contains("  - [x] Balcony"), text)
        assertEquals(true, text.contains("repeat:weekly tags:garden,morning_chores at:08:30 id:t1"), text)
    }

    @Test fun anOldFileWithoutTheNewMarkersStillReads() {
        fs.createDirectories("/v/tasks".toPath())
        fs.write("/v/tasks/Tasks.md".toPath()) { writeUtf8("# Tasks\n\n- [ ] Old one due:2026-10-20 p:1 id:o1\n") }
        assertEquals(Task("o1", "Old one", false, LocalDate(2026, 10, 20), 1), repo.tasks().single())
    }

    @Test fun anInvalidRepeatOrTimeIsDropped() {
        fs.createDirectories("/v/tasks".toPath())
        fs.write("/v/tasks/Tasks.md".toPath()) { writeUtf8("- [ ] X repeat:yearly at:25:99 id:x1\n") }
        val t = repo.tasks().single()
        assertEquals("", t.repeat); assertEquals("", t.remindAt)
    }

    @Test fun nextDueMovesPastTodayInWholeSteps() {
        assertEquals(LocalDate(2026, 10, 15), Recurrence.nextDue(day, "daily", day))
        assertEquals(LocalDate(2026, 10, 21), Recurrence.nextDue(day, "weekly", day))
        assertEquals(LocalDate(2026, 11, 14), Recurrence.nextDue(day, "monthly", day))
        assertEquals(LocalDate(2026, 10, 15), Recurrence.nextDue(LocalDate(2026, 10, 1), "weekly", day))
        assertEquals(LocalDate(2026, 2, 28), Recurrence.nextDue(LocalDate(2026, 1, 31), "monthly", LocalDate(2026, 1, 31)))
        assertEquals(null, Recurrence.nextDue(day, "", day))
    }

    @Test fun finishingARepeatingTaskLeavesTheNextOneOpenAndUntickingDoesNot() {
        val t = Task("t1", "Gym", false, day, repeat = "weekly", subtasks = listOf(Subtask("Warm up", true)))
        val after = Recurrence.complete(listOf(t), "t1", true, day)
        assertEquals(2, after.size)
        assertEquals(true, after.first { it.id == "t1" }.done)
        val next = after.first { it.id != "t1" }
        assertEquals(false, next.done); assertEquals(LocalDate(2026, 10, 21), next.due); assertEquals(false, next.subtasks.single().done)
        assertEquals(2, Recurrence.complete(after, "t1", false, day).size)
        assertEquals(1, Recurrence.complete(listOf(Task("p", "Once")), "p", true, day).size)
    }
}
