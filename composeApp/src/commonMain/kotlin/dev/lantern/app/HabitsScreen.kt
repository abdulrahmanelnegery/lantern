package dev.lantern.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.lantern.core.Habit
import dev.lantern.core.newId
import dev.lantern.core.streak
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus

@Composable
fun HabitsScreen(store: LanternStore, openId: String? = null, onOpen: (String) -> Unit = {}, onClose: () -> Unit = {}) {
    store.habits.firstOrNull { it.id == openId }?.let { HabitDetailScreen(store, it, onClose); return }
    var adding by remember { mutableStateOf(false) }
    val today = store.today
    val active = store.habits.filter { !it.archived }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = { FloatingActionButton(onClick = { adding = true }) { Icon(Icons.Rounded.Add, "New habit") } },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (active.isEmpty()) EmptyHint("No habits yet", "Tap + to start one: reading, water, a walk. Tick it each day to build a streak.")
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(active, key = { it.id }) { h ->
                    val days = store.habitLog[h.id].orEmpty()
                    Panel(onClick = { onOpen(h.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(h.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                val s = streak(h, days, today)
                                Text(if (s > 0) "$s day streak  ·  ${days.size} total" else "${days.size} total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { store.saveHabits(store.habits.map { if (it.id == h.id) it.copy(archived = true) else it }) }) { Icon(Icons.Rounded.Delete, "Archive", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        // The last 14 days; tap a day to mark or unmark it.
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            (13 downTo 0).forEach { back ->
                                val d = today.minus(back, DateTimeUnit.DAY)
                                val done = d in days
                                val applies = h.days.isEmpty() || d.dayOfWeek.ordinal + 1 in h.days
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(dayName(d.dayOfWeek).take(1), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Box(
                                        Modifier.size(20.dp).clip(CircleShape)
                                            .background(if (done) MaterialTheme.colorScheme.primary else if (applies) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.background)
                                            .clickable { store.setHabitDone(h.id, d, !done) },
                                    )
                                    Text("${d.dayOfMonth}", style = MaterialTheme.typography.labelSmall, color = if (back == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (adding) NewHabitDialog(onDismiss = { adding = false }) { name, days ->
        store.saveHabits(store.habits + Habit(newId(), name.trim(), days))
        adding = false
    }
}

@Composable
private fun NewHabitDialog(onDismiss: () -> Unit, onCreate: (String, List<Int>) -> Unit) {
    var name by remember { mutableStateOf("") }
    var days by remember { mutableStateOf(emptySet<Int>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, singleLine = true, placeholder = { Text("What do you want to do?") }, modifier = Modifier.fillMaxWidth())
                Text(if (days.isEmpty()) "Every day" else "On the days you pick", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { i, label ->
                        FilterChip(i + 1 in days, { days = if (i + 1 in days) days - (i + 1) else days + (i + 1) }, { Text(label) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onCreate(name, days.sorted()) }, enabled = name.isNotBlank()) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
