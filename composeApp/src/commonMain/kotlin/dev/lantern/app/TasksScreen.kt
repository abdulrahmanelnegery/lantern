package dev.lantern.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.lantern.core.Task
import dev.lantern.core.newId
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

private enum class TaskFilter(val label: String) { Open("Open"), Today("Due"), Done("Done") }

@Composable
fun TasksScreen(store: LanternStore) {
    var filter by remember { mutableStateOf(TaskFilter.Open) }
    var title by remember { mutableStateOf("") }
    var due by remember { mutableStateOf<LocalDate?>(null) }
    val today = store.today
    fun add() {
        if (title.isBlank()) return
        store.saveTasks(listOf(Task(newId(), title.trim(), due = due)) + store.tasks)
        title = ""; due = null
    }
    val shown = store.tasks.filter {
        when (filter) {
            TaskFilter.Open -> !it.done
            TaskFilter.Today -> !it.done && it.due != null && it.due!! <= today
            TaskFilter.Done -> it.done
        }
    }.sortedWith(compareBy<Task> { it.due ?: LocalDate(9999, 1, 1) }.thenByDescending { it.priority })

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                title, { title = it }, singleLine = true, placeholder = { Text("Add a task") },
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() }),
                trailingIcon = { IconButton(onClick = ::add, enabled = title.isNotBlank()) { Icon(Icons.Rounded.Add, "Add") } },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = { due = if (due == today) null else today }, label = { Text("Today") }, leadingIcon = if (due == today) ({ Icon(Icons.Rounded.Close, null) }) else null)
                AssistChip(onClick = { due = today.plus(1, DateTimeUnit.DAY).takeIf { due != it } }, label = { Text("Tomorrow") })
                AssistChip(onClick = { due = today.plus(7, DateTimeUnit.DAY).takeIf { due != it } }, label = { Text("Next week") })
                due?.let { Text(prettyDate(it), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskFilter.entries.forEach { f -> FilterChip(filter == f, { filter = f }, { Text("${f.label} ${count(store, f)}") }) }
            }
        }
        if (shown.isEmpty()) EmptyHint(if (filter == TaskFilter.Done) "Nothing done yet" else "All clear", "Add a task above. They live in tasks/Tasks.md as a checklist.")
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(shown, key = { it.id }) { t ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(t.done, { v -> store.saveTasks(store.tasks.map { x -> if (x.id == t.id) x.copy(done = v) else x }) })
                    Column(Modifier.weight(1f)) {
                        Text(t.title, style = MaterialTheme.typography.bodyLarge, textDecoration = if (t.done) TextDecoration.LineThrough else null, color = if (t.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                        t.due?.let { d ->
                            Text(prettyDate(d), style = MaterialTheme.typography.labelSmall, color = if (!t.done && d < today) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = { store.saveTasks(store.tasks.filter { it.id != t.id }) }) { Icon(Icons.Rounded.Close, "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}

private fun count(store: LanternStore, f: TaskFilter): Int = store.tasks.count {
    when (f) {
        TaskFilter.Open -> !it.done
        TaskFilter.Today -> !it.done && it.due != null && it.due!! <= store.today
        TaskFilter.Done -> it.done
    }
}
