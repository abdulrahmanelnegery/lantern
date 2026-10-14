package dev.dawem.tasks

import dev.dawem.core.VaultFiles
import dev.dawem.core.newId
import kotlinx.datetime.LocalDate

/** Tasks as one Markdown checklist, `tasks/Tasks.md`. */
class TasksRepository(private val files: VaultFiles) {
    init { files.fs.createDirectories(files.dir("tasks")) }

    private val path get() = files.dir("tasks") / "Tasks.md"

    fun tasks(): List<Task> = (files.read(path) ?: "").lineSequence().mapNotNull(::parse).toList()

    fun save(tasks: List<Task>) = files.write(path, "# Tasks\n\n" + tasks.joinToString("\n") { format(it) } + "\n")

    private fun format(t: Task): String = buildString {
        append(if (t.done) "- [x] " else "- [ ] ").append(t.title)
        t.due?.let { append(" due:").append(it) }
        if (t.priority > 0) append(" p:").append(t.priority)
        if (t.list.isNotEmpty()) append(" list:").append(t.list.replace(' ', '_'))
        t.doneOn?.let { append(" done:").append(it) }
        append(" id:").append(t.id)
    }

    private fun parse(line: String): Task? {
        val m = Regex("^\\s*- \\[( |x|X)] (.*)$").find(line) ?: return null
        val tokens = m.groupValues[2].split(' ').toMutableList()
        var due: LocalDate? = null
        var priority = 0
        var list = ""
        var id = ""
        var doneOn: LocalDate? = null
        while (tokens.isNotEmpty()) {
            val t = tokens.last()
            when {
                t.startsWith("due:") -> due = runCatching { LocalDate.parse(t.removePrefix("due:")) }.getOrNull()
                t.startsWith("p:") -> priority = t.removePrefix("p:").toIntOrNull() ?: 0
                t.startsWith("list:") -> list = t.removePrefix("list:").replace('_', ' ')
                t.startsWith("id:") -> id = t.removePrefix("id:")
                t.startsWith("done:") -> doneOn = runCatching { LocalDate.parse(t.removePrefix("done:")) }.getOrNull()
                else -> break
            }
            tokens.removeAt(tokens.lastIndex)
        }
        val title = tokens.joinToString(" ").trim()
        if (title.isEmpty()) return null
        return Task(id.ifEmpty { newId() }, title, m.groupValues[1] != " ", due, priority, list, doneOn)
    }
}
