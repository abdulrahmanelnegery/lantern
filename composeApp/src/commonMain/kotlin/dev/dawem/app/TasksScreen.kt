package dev.dawem.app

import dev.dawem.ui.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import dev.dawem.core.Task
import dev.dawem.core.newId
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

private enum class TaskFilter(val label: String) { Open("Open"), Due("Due"), Done("Done") }

private val priorityNames = listOf("None", "Low", "Medium", "High")

/** [list]: null shows every task, "" the inbox, anything else that list. */
@Composable
fun TasksScreen(store: DawemStore, list: String? = null) {
    var filter by remember { mutableStateOf(TaskFilter.Open) }
    var editing by remember { mutableStateOf<Task?>(null) }
    var adding by remember { mutableStateOf(false) }
    val today = store.today
    val inList = store.tasks.filter { list == null || it.list == list }
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
                        Checkbox(t.done, { v -> store.saveTasks(store.tasks.map { x -> if (x.id == t.id) x.copy(done = v, doneOn = if (v) store.today else null) else x }) })
                        Column(Modifier.weight(1f).clickable { editing = t }) {
                            Text(t.title, style = MaterialTheme.typography.bodyLarge, textDecoration = if (t.done) TextDecoration.LineThrough else null, color = if (t.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                            val meta = listOfNotNull(t.due?.let { prettyDate(it) }, t.list.takeIf { it.isNotEmpty() && list == null }, priorityNames[t.priority.coerceIn(0, 3)].takeIf { t.priority > 0 }?.let { "$it priority" })
                            if (meta.isNotEmpty()) Text(
                                meta.joinToString("  ·  "), style = MaterialTheme.typography.labelSmall,
                                color = if (!t.done && t.due != null && t.due!! < today) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { store.saveTasks(store.tasks.filter { it.id != t.id }) }) { Icon(Icons.Rounded.Close, "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
    if (adding) TaskDialog(store, Task(newId(), "", list = list.orEmpty()), "New task", onDismiss = { adding = false }) { t ->
        store.saveTasks(listOf(t) + store.tasks); adding = false
    }
    editing?.let { current ->
        TaskDialog(store, current, "Edit task", onDismiss = { editing = null }) { t ->
            store.saveTasks(store.tasks.map { if (it.id == t.id) t else it }); editing = null
        }
    }
}

@Composable
private fun TaskDialog(store: DawemStore, start: Task, heading: String, onDismiss: () -> Unit, onSave: (Task) -> Unit) {
    var task by remember(start.id) { mutableStateOf(start) }
    val today = store.today
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(heading) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    items(store.taskLists()) { l -> FilterChip(task.list == l, { task = task.copy(list = l) }, { Text(l) }) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(task.copy(title = task.title.trim())) }, enabled = task.title.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
