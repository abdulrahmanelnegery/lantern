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
