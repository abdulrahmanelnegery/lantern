package dev.thabit.tasks

import dev.thabit.ui.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.thabit.core.newId
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import dev.thabit.tasks.Task

private enum class TaskFilter(val label: String) { Open("Open"), Due("Due"), Done("Done") }

private val priorityNames = listOf("None", "Low", "Medium", "High")

/** [list]: null shows every task, "" the inbox, anything else that list. */
@Composable
fun TasksScreen(state: TasksState, today: kotlinx.datetime.LocalDate, list: String? = null) {
    var filter by remember { mutableStateOf(TaskFilter.Open) }
    var editing by remember { mutableStateOf<Task?>(null) }
    var adding by remember { mutableStateOf(false) }
    val today = today
    val inList = state.tasks.filter { list == null || it.list == list }
    fun matches(t: Task, f: TaskFilter) = when (f) {
        TaskFilter.Open -> !t.done
        TaskFilter.Due -> !t.done && t.due != null && t.due!! <= today
        TaskFilter.Done -> t.done
    }
    val shown = inList.filter { matches(it, filter) }.sortedWith(compareBy<Task> { it.due ?: LocalDate(9999, 1, 1) }.thenByDescending { it.priority })

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = { FloatingActionButton(onClick = { adding = true }) { Icon(Icons.Rounded.Add, "New task") } },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskFilter.entries.forEach { f -> FilterChip(filter == f, { filter = f }, { Text("${f.label} ${inList.count { matches(it, f) }}") }) }
            }
            if (shown.isEmpty()) EmptyHint(if (filter == TaskFilter.Done) "Nothing done yet" else "All clear", "Tap + to add a task. They live in tasks/Tasks.md as a checklist.")
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(shown, key = { it.id }) { t ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(t.done, { v -> state.complete(t.id, v, today) })
                        Column(Modifier.weight(1f).clickable { editing = t }) {
                            Text(t.title, style = MaterialTheme.typography.bodyLarge, textDecoration = if (t.done) TextDecoration.LineThrough else null, color = if (t.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                            val meta = listOfNotNull(t.due?.let { prettyDate(it) }, t.list.takeIf { it.isNotEmpty() && list == null }, priorityNames[t.priority.coerceIn(0, 3)].takeIf { t.priority > 0 }?.let { "$it priority" }, t.repeat.takeIf { it.isNotEmpty() }?.let { "repeats $it" }, t.remindAt.takeIf { it.isNotEmpty() }?.let { "reminder $it" }, t.subtasks.takeIf { it.isNotEmpty() }?.let { "${it.count { s -> s.done }}/${it.size} steps" }, t.tags.takeIf { it.isNotEmpty() }?.joinToString(" ") { "#$it" })
                            if (meta.isNotEmpty()) Text(
                                meta.joinToString("  ·  "), style = MaterialTheme.typography.labelSmall,
                                color = if (!t.done && t.due != null && t.due!! < today) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { state.save(state.tasks.filter { it.id != t.id }) }) { Icon(Icons.Rounded.Close, "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
    if (adding) TaskDialog(state, today, Task(newId(), "", list = list.orEmpty()), "New task", onDismiss = { adding = false }) { t ->
        state.save(listOf(t) + state.tasks); adding = false
    }
    editing?.let { current ->
        TaskDialog(state, today, current, "Edit task", onDismiss = { editing = null }) { t ->
            state.save(state.tasks.map { if (it.id == t.id) t else it }); editing = null
        }
    }
}

@Composable
private fun TaskDialog(state: TasksState, today: kotlinx.datetime.LocalDate, start: Task, heading: String, onDismiss: () -> Unit, onSave: (Task) -> Unit) {
    var task by remember(start.id) { mutableStateOf(start) }
    val today = today
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(heading) },
        text = {
            Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(task.title, { task = task.copy(title = it) }, singleLine = true, placeholder = { Text("What needs doing?") }, modifier = Modifier.fillMaxWidth())
                Text("Due", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(onClick = { task = task.copy(due = today.takeIf { task.due != it }) }, label = { Text("Today") })
                    AssistChip(onClick = { task = task.copy(due = today.plus(1, DateTimeUnit.DAY).takeIf { task.due != it }) }, label = { Text("Tomorrow") })
                    AssistChip(onClick = { task = task.copy(due = today.plus(7, DateTimeUnit.DAY).takeIf { task.due != it }) }, label = { Text("Next week") })
                }
                task.due?.let { Text(prettyDate(it) + " " + it.year, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
                Text("Priority", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    priorityNames.forEachIndexed { i, n -> FilterChip(task.priority == i, { task = task.copy(priority = i) }, { Text(n) }) }
                }
                Text("List", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { FilterChip(task.list.isEmpty(), { task = task.copy(list = "") }, { Text("Inbox") }) }
                    items(state.lists()) { l -> FilterChip(task.list == l, { task = task.copy(list = l) }, { Text(l) }) }
                }
                Text("Repeat", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(task.repeat.isEmpty(), { task = task.copy(repeat = "") }, { Text("Once") })
                    Recurrence.kinds.forEach { k -> FilterChip(task.repeat == k, { task = task.copy(repeat = k) }, { Text(k.replaceFirstChar { it.uppercase() }) }) }
                }
                Text("Reminder (needs a due date)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(task.remindAt.isEmpty(), { task = task.copy(remindAt = "") }, { Text("None") })
                    listOf("08:00", "12:00", "18:00", "21:00").forEach { t -> FilterChip(task.remindAt == t, { task = task.copy(remindAt = t) }, { Text(t) }) }
                }
                var time by remember(start.id) { mutableStateOf(task.remindAt) }
                OutlinedTextField(time, { time = it; if (Recurrence.validTime(it)) task = task.copy(remindAt = it) }, singleLine = true, label = { Text("Other time, HH:MM") }, isError = time.isNotEmpty() && !Recurrence.validTime(time), modifier = Modifier.fillMaxWidth())
                var tags by remember(start.id) { mutableStateOf(task.tags.joinToString(", ")) }
                OutlinedTextField(tags, { tags = it; task = task.copy(tags = it.split(',', ' ').map { x -> x.trim().trimStart('#') }.filter { x -> x.isNotEmpty() }) }, singleLine = true, label = { Text("Tags, separated by commas") }, modifier = Modifier.fillMaxWidth())
                Text("Steps", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                task.subtasks.forEachIndexed { i, st ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(st.done, { v -> task = task.copy(subtasks = task.subtasks.mapIndexed { j, x -> if (j == i) x.copy(done = v) else x }) })
                        Text(st.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { task = task.copy(subtasks = task.subtasks.filterIndexed { j, _ -> j != i }) }) { Icon(Icons.Rounded.Close, "Remove step") }
                    }
                }
                var step by remember(start.id) { mutableStateOf("") }
                OutlinedTextField(step, { step = it }, singleLine = true, label = { Text("Add a step") }, modifier = Modifier.fillMaxWidth(), trailingIcon = {
                    IconButton(onClick = { if (step.isNotBlank()) { task = task.copy(subtasks = task.subtasks + Subtask(step.trim())); step = "" } }) { Icon(Icons.Rounded.Add, "Add step") }
                })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(task.copy(title = task.title.trim())) }, enabled = task.title.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
